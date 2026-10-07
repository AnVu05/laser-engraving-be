package com.anne.laserengraving.service;

import com.anne.laserengraving.dto.request.*;
import com.anne.laserengraving.dto.response.AuthResponse;
import com.anne.laserengraving.dto.response.UserResponse;
import com.anne.laserengraving.entity.UserEntity;
import com.anne.laserengraving.exception.InvalidParameterException;
import com.anne.laserengraving.exception.ResourceNotFoundException;
import com.anne.laserengraving.repository.UserRepository;
import com.anne.laserengraving.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public String initiateRegister(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        // 1. Ràng buộc nghiêm ngặt chỉ chấp nhận Gmail
        if (!email.endsWith("@gmail.com")) {
            throw new InvalidParameterException("Hệ thống chỉ chấp nhận đăng ký tài khoản bằng địa chỉ Gmail (@gmail.com)!");
        }

        // 2. Kiểm tra mật khẩu trùng khớp
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new InvalidParameterException("Mật khẩu và xác nhận mật khẩu không trùng khớp!");
        }

        // 3. Kiểm tra email đã tồn tại hay chưa
        if (userRepository.existsByEmail(email)) {
            throw new InvalidParameterException("Địa chỉ Gmail này đã được đăng ký trong hệ thống!");
        }

        // 4. Phát sinh mã OTP 6 số và lưu vào phiên chờ
        String otpCode = otpService.generateOtp();
        otpService.savePendingRegistration(email, request.getPassword(), otpCode);
        log.info("[AUTH] Đã khởi tạo tiến trình đăng ký và gửi mã OTP cho: {}", email);
        return otpCode;
    }

    @Override
    @Transactional
    public AuthResponse verifyOtpAndRegister(VerifyOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        // 1. Xác thực tính hợp lệ của mã OTP
        boolean isValid = otpService.verifyOtp(email, request.getOtp());
        if (!isValid) {
            throw new InvalidParameterException("Mã xác thực OTP không chính xác hoặc đã hết thời gian hiệu lực!");
        }

        // 2. Lấy mật khẩu đã gửi trước đó từ phiên chờ
        String rawPassword = otpService.getPendingPassword(email);
        if (rawPassword == null) {
            throw new InvalidParameterException("Phiên đăng ký đã hết hạn. Vui lòng thực hiện lại từ đầu!");
        }

        // 3. Kiểm tra lại trùng lặp email trước khi lưu DB
        if (userRepository.existsByEmail(email)) {
            otpService.removePendingRegistration(email);
            throw new InvalidParameterException("Tài khoản với Gmail này đã tồn tại!");
        }

        // 4. Tạo tên người dùng cơ bản từ email
        String baseUsername = email.split("@")[0];
        String username = baseUsername;
        int suffix = 1;
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + "_" + suffix++;
        }

        // 5. Lưu tài khoản mới vào cơ sở dữ liệu
        UserEntity newUser = UserEntity.builder()
                .username(username)
                .fullName(username)
                .email(email)
                .authProvider("GMAIL")
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role("OPERATOR")
                .createdAt(LocalDateTime.now())
                .build();

        UserEntity savedUser = userRepository.save(newUser);
        otpService.removePendingRegistration(email);
        log.info("[AUTH] Đăng ký tài khoản thành công cho user: {} (ID: {})", savedUser.getEmail(), savedUser.getUserId());

        return buildAuthResponse(savedUser);
    }

    @Override
    public String resendOtp(ResendOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (!otpService.canResendOtp(email)) {
            throw new InvalidParameterException("Vui lòng chờ hết 59 giây trước khi yêu cầu gửi lại mã OTP mới!");
        }

        String rawPassword = otpService.getPendingPassword(email);
        if (rawPassword == null) {
            throw new InvalidParameterException("Không tìm thấy phiên đăng ký. Vui lòng điền thông tin đăng ký lại!");
        }

        String newOtpCode = otpService.generateOtp();
        otpService.updateOtp(email, newOtpCode);
        log.info("[AUTH] Đã gửi lại mã OTP mới cho: {}", email);
        return newOtpCode;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (!email.endsWith("@gmail.com")) {
            throw new InvalidParameterException("Hệ thống chỉ hỗ trợ đăng nhập với tài khoản Gmail (@gmail.com)!");
        }

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidParameterException("Tài khoản Gmail hoặc mật khẩu không chính xác!"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidParameterException("Tài khoản Gmail hoặc mật khẩu không chính xác!");
        }

        log.info("[AUTH] Người dùng {} đăng nhập thành công vào Laser Studio", email);
        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse authenticateWithGoogle(GoogleAuthRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (!email.endsWith("@gmail.com")) {
            throw new InvalidParameterException("Hệ thống chỉ chấp nhận tài khoản Google có đuôi @gmail.com!");
        }

        UserEntity user = userRepository.findByEmail(email).map(existing -> {
            boolean changed = false;
            if (existing.getFullName() == null && request.getName() != null) {
                existing.setFullName(request.getName());
                changed = true;
            }
            if (existing.getAvatarUrl() == null && request.getAvatarUrl() != null) {
                existing.setAvatarUrl(request.getAvatarUrl());
                changed = true;
            }
            if (existing.getGoogleId() == null && request.getGoogleId() != null) {
                existing.setGoogleId(request.getGoogleId());
                changed = true;
            }
            return changed ? userRepository.save(existing) : existing;
        }).orElseGet(() -> {
            String baseUsername = request.getName() != null && !request.getName().isBlank()
                    ? request.getName().trim().replaceAll("\\s+", "_").toLowerCase()
                    : email.split("@")[0];

            String username = baseUsername;
            int suffix = 1;
            while (userRepository.existsByUsername(username)) {
                username = baseUsername + "_" + suffix++;
            }

            UserEntity newUser = UserEntity.builder()
                    .username(username)
                    .fullName(request.getName() != null ? request.getName() : username)
                    .email(email)
                    .avatarUrl(request.getAvatarUrl())
                    .googleId(request.getGoogleId())
                    .authProvider("GOOGLE")
                    .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .role("OPERATOR")
                    .createdAt(LocalDateTime.now())
                    .build();

            log.info("[AUTH] Khởi tạo tài khoản tự động từ Google OAuth cho: {}", email);
            return userRepository.save(newUser);
        });

        return buildAuthResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserByEmail(String email) {
        UserEntity user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với email: " + email));
        return mapToUserResponse(user);
    }

    private AuthResponse buildAuthResponse(UserEntity user) {
        String token = jwtUtil.generateToken(user.getEmail(), user.getUserId(), user.getRole());
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(mapToUserResponse(user))
                .build();
    }

    private UserResponse mapToUserResponse(UserEntity user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .authProvider(user.getAuthProvider())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
