package com.anne.laserengraving.service;

public interface OtpService {

    /**
     * Tạo ngẫu nhiên mã OTP 6 chữ số
     */
    String generateOtp();

    /**
     * Lưu trữ thông tin đăng ký chờ xác thực mã OTP
     */
    void savePendingRegistration(String email, String rawPassword, String otpCode);

    /**
     * Kiểm tra thời gian chờ gửi lại mã OTP (cooldown 59s)
     */
    boolean canResendOtp(String email);

    /**
     * Cập nhật mã OTP mới cho email đăng ký
     */
    void updateOtp(String email, String newOtpCode);

    /**
     * Thẩm định mã OTP hợp lệ
     */
    boolean verifyOtp(String email, String inputOtp);

    /**
     * Lấy mật khẩu đã đăng ký của phiên OTP
     */
    String getPendingPassword(String email);

    /**
     * Xóa phiên OTP sau khi xác thực thành công
     */
    void removePendingRegistration(String email);
}
