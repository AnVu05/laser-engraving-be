package com.anne.laserengraving.service;

import com.anne.laserengraving.dto.request.*;
import com.anne.laserengraving.dto.response.AuthResponse;
import com.anne.laserengraving.dto.response.UserResponse;

public interface AuthService {

    /**
     * Bắt đầu quy trình đăng ký tài khoản và phát mã OTP về Gmail
     */
    void initiateRegister(RegisterRequest request);

    /**
     * Xác thực mã OTP 6 chữ số và kích hoạt tài khoản
     */
    AuthResponse verifyOtpAndRegister(VerifyOtpRequest request);

    /**
     * Gửi lại mã xác thực OTP
     */
    void resendOtp(ResendOtpRequest request);

    /**
     * Đăng nhập hệ thống bằng Gmail & Mật khẩu
     */
    AuthResponse login(LoginRequest request);

    /**
     * Đăng nhập hoặc đăng ký nhanh thông qua Google OAuth 2.0
     */
    AuthResponse authenticateWithGoogle(GoogleAuthRequest request);

    /**
     * Lấy thông tin tài khoản người dùng theo email
     */
    UserResponse getCurrentUserByEmail(String email);
}
