package com.anne.laserengraving.controller;

import com.anne.laserengraving.dto.request.*;
import com.anne.laserengraving.dto.response.ApiResponse;
import com.anne.laserengraving.dto.response.AuthResponse;
import com.anne.laserengraving.dto.response.UserResponse;
import com.anne.laserengraving.service.AuthService;
import com.anne.laserengraving.util.CookieUtil;
import com.anne.laserengraving.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "1. Authentication (Xác thực & Tài khoản)", description = "Các API đăng ký tài khoản Gmail, gửi & xác thực mã OTP 6 số, đăng nhập và Google OAuth 2.0 bảo mật với HttpOnly Cookie")
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    @Operation(
            summary = "Đăng ký tài khoản Gmail (Nhận mã OTP 6 số)",
            description = "Bắt đầu đăng ký tài khoản. Ràng buộc: Email bắt buộc phải có đuôi @gmail.com, mật khẩu tối thiểu 6 ký tự. Hệ thống sẽ phát sinh mã OTP 6 số (hiệu lực 5 phút) và gửi về hộp thư."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(
            @Valid @RequestBody RegisterRequest request) {
        String otpCode = authService.initiateRegister(request);
        Map<String, Object> data = Map.of(
                "email", request.getEmail(),
                "cooldownSeconds", 59,
                "otpCode", otpCode
        );
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok(data, "Mã xác thực OTP 6 chữ số đã được tạo cho: " + request.getEmail()));
    }

    @PostMapping("/verify-otp")
    @Operation(
            summary = "Xác thực mã OTP 6 số & kích hoạt tài khoản",
            description = "Kiểm tra mã OTP 6 số. Nếu hợp lệ, hệ thống sẽ lưu tài khoản và thiết lập HttpOnly Cookie chứa JWT an toàn."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        AuthResponse response = authService.verifyOtpAndRegister(request);
        ResponseCookie cookie = cookieUtil.createAuthCookie(response.getToken());
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.ok(response, "Xác thực tài khoản thành công! Chào mừng bạn gia nhập Laser Studio."));
    }

    @PostMapping("/resend-otp")
    @Operation(
            summary = "Gửi lại mã OTP qua Gmail",
            description = "Tạo mã OTP mới cho phiên đăng ký đang chờ nếu thời gian chờ đã qua 59 giây."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> resendOtp(
            @Valid @RequestBody ResendOtpRequest request) {
        String newOtpCode = authService.resendOtp(request);
        Map<String, Object> data = Map.of(
                "email", request.getEmail(),
                "cooldownSeconds", 59,
                "otpCode", newOtpCode
        );
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok(data, "Đã gửi lại mã OTP mới tới địa chỉ Gmail của bạn!"));
    }

    @PostMapping("/login")
    @Operation(
            summary = "Đăng nhập bằng Gmail & Mật khẩu",
            description = "Đăng nhập vào hệ thống bằng địa chỉ Gmail và mật khẩu, thiết lập HttpOnly Cookie chứa JWT token chống XSS."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        ResponseCookie cookie = cookieUtil.createAuthCookie(response.getToken());
        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.ok(response, "Đăng nhập hệ thống Laser Studio thành công!"));
    }

    @PostMapping("/google")
    @Operation(
            summary = "Đăng nhập nhanh với Google OAuth 2.0",
            description = "Đăng nhập nhanh với Google, tự động thiết lập HttpOnly Cookie bảo vệ phiên."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> googleAuth(
            @Valid @RequestBody GoogleAuthRequest request) {
        AuthResponse response = authService.authenticateWithGoogle(request);
        ResponseCookie cookie = cookieUtil.createAuthCookie(response.getToken());
        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.ok(response, "Đăng nhập nhanh với Google thành công!"));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Đăng xuất tài khoản an toàn",
            description = "Thu hồi và xóa HttpOnly Cookie lưu phiên JWT trên trình duyệt"
    )
    public ResponseEntity<ApiResponse<Void>> logout() {
        ResponseCookie cookie = cookieUtil.clearAuthCookie();
        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.ok(null, "Đăng xuất tài khoản thành công!"));
    }

    @GetMapping("/me")
    @Operation(
            summary = "Lấy thông tin tài khoản hiện tại",
            description = "Truy xuất thông tin người dùng từ HttpOnly Cookie hoặc Bearer Header đã xác thực."
    )
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @CookieValue(name = CookieUtil.AUTH_COOKIE_NAME, required = false) String cookieToken,
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {

        String targetEmail = null;
        if (cookieToken != null && jwtUtil.validateToken(cookieToken)) {
            targetEmail = jwtUtil.extractEmail(cookieToken);
        } else if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String bearerToken = authHeader.substring(7);
            if (jwtUtil.validateToken(bearerToken)) {
                targetEmail = jwtUtil.extractEmail(bearerToken);
            }
        }

        if (targetEmail == null || targetEmail.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Phiên đăng nhập không tồn tại hoặc đã hết hạn. Vui lòng đăng nhập lại!"));
        }

        UserResponse response = authService.getCurrentUserByEmail(targetEmail);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok(response, "Lấy thông tin tài khoản thành công!"));
    }
}
