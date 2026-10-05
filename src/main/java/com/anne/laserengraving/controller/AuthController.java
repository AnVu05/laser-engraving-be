package com.anne.laserengraving.controller;

import com.anne.laserengraving.dto.request.*;
import com.anne.laserengraving.dto.response.ApiResponse;
import com.anne.laserengraving.dto.response.AuthResponse;
import com.anne.laserengraving.dto.response.UserResponse;
import com.anne.laserengraving.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "1. Authentication (Xác thực & Tài khoản)", description = "Các API đăng ký tài khoản Gmail, gửi & xác thực mã OTP 6 số, đăng nhập và Google OAuth 2.0")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(
            summary = "Đăng ký tài khoản Gmail (Nhận mã OTP 6 số)",
            description = "Bắt đầu đăng ký tài khoản. Ràng buộc: Email bắt buộc phải có đuôi @gmail.com, mật khẩu tối thiểu 6 ký tự. Hệ thống sẽ phát sinh mã OTP 6 số (hiệu lực 5 phút) và gửi về hộp thư."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(
            @Valid @RequestBody RegisterRequest request) {
        authService.initiateRegister(request);
        Map<String, Object> data = Map.of(
                "email", request.getEmail(),
                "cooldownSeconds", 59
        );
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok(data, "Mã xác thực OTP 6 chữ số đã được gửi tới Gmail của bạn. Vui lòng kiểm tra hộp thư!"));
    }

    @PostMapping("/verify-otp")
    @Operation(
            summary = "Xác thực mã OTP 6 số & kích hoạt tài khoản",
            description = "Kiểm tra mã OTP 6 số. Nếu hợp lệ, hệ thống sẽ mã hoá mật khẩu bằng BCrypt, lưu tài khoản vào CSDL SQL Server và trả về Bearer Token cùng thông tin người dùng."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        AuthResponse response = authService.verifyOtpAndRegister(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Xác thực tài khoản thành công! Chào mừng bạn gia nhập Laser Studio."));
    }

    @PostMapping("/resend-otp")
    @Operation(
            summary = "Gửi lại mã OTP qua Gmail",
            description = "Tạo mã OTP mới cho phiên đăng ký đang chờ nếu thời gian chờ đã qua 59 giây."
    )
    public ResponseEntity<ApiResponse<Void>> resendOtp(
            @Valid @RequestBody ResendOtpRequest request) {
        authService.resendOtp(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok(null, "Đã gửi lại mã OTP mới tới địa chỉ Gmail của bạn!"));
    }

    @PostMapping("/login")
    @Operation(
            summary = "Đăng nhập bằng Gmail & Mật khẩu",
            description = "Đăng nhập vào hệ thống bằng địa chỉ Gmail đã kích hoạt và mật khẩu đã lưu."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok(response, "Đăng nhập hệ thống Laser Studio thành công!"));
    }

    @PostMapping("/google")
    @Operation(
            summary = "Đăng nhập nhanh với Google OAuth 2.0",
            description = "Đăng nhập nhanh với Google. Nếu tài khoản chưa tồn tại, hệ thống sẽ tự động tạo tài khoản mới với vai trò OPERATOR."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> googleAuth(
            @Valid @RequestBody GoogleAuthRequest request) {
        AuthResponse response = authService.authenticateWithGoogle(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok(response, "Đăng nhập nhanh với Google thành công!"));
    }

    @GetMapping("/me")
    @Operation(
            summary = "Lấy thông tin tài khoản hiện tại",
            description = "Truy xuất thông tin người dùng theo email (ID, Username, Email, Role, CreatedAt)."
    )
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @Parameter(description = "Địa chỉ Gmail cần truy xuất", example = "operator.laser@gmail.com")
            @RequestParam String email) {
        UserResponse response = authService.getCurrentUserByEmail(email);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok(response, "Lấy thông tin tài khoản thành công!"));
    }
}
