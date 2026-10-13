package com.anne.laserengraving.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Tiện ích mã hóa & tính toán mã băm SHA-256 phục vụ bảo mật IoT
 * Đảm bảo tính toàn vẹn 100% của tệp G-code giữa Cloud Backend và ESP32 Edge Device.
 */
public final class CryptoUtil {

    private CryptoUtil() {
    }

    /**
     * Tính toán mã băm SHA-256 từ mảng byte (trả về chuỗi hex 64 ký tự thường)
     */
    public static String calculateSha256(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Dữ liệu đầu vào để tính SHA-256 không được null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Hệ điều hành không hỗ trợ thuật toán SHA-256", e);
        }
    }

    /**
     * Tính toán mã băm SHA-256 từ chuỗi văn bản (UTF-8)
     */
    public static String calculateSha256(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Chuỗi đầu vào để tính SHA-256 không được null");
        }
        return calculateSha256(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Tính toán mã băm SHA-256 trực tiếp từ InputStream của luồng tệp
     */
    public static String calculateSha256(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream không được null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Hệ điều hành không hỗ trợ thuật toán SHA-256", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
