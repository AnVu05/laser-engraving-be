package com.anne.laserengraving.service;

/**
 * Giao diện dịch vụ gửi email giao dịch (Transactional Email) cho hệ thống Laser Studio.
 */
public interface EmailService {

    /**
     * Gửi email chứa mã OTP 6 số xác thực tài khoản tới hộp thư Gmail của người dùng.
     *
     * @param toEmail Địa chỉ Gmail người nhận
     * @param otpCode Mã OTP 6 chữ số
     */
    void sendOtpEmail(String toEmail, String otpCode);
}
