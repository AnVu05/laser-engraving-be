package com.anne.laserengraving.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpServiceImpl implements OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpServiceImpl.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long OTP_VALID_DURATION_SECONDS = 300; // 5 phút
    private static final long RESEND_COOLDOWN_SECONDS = 59;     // 59 giây

    private final Map<String, PendingRegistration> otpStore = new ConcurrentHashMap<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class PendingRegistration {
        private String email;
        private String password;
        private String otpCode;
        private Instant expiresAt;
        private Instant lastSentAt;
    }

    @Override
    public String generateOtp() {
        int number = RANDOM.nextInt(1_000_000);
        return String.format("%06d", number);
    }

    @Override
    public void savePendingRegistration(String email, String rawPassword, String otpCode) {
        Instant now = Instant.now();
        PendingRegistration pending = PendingRegistration.builder()
                .email(email.toLowerCase())
                .password(rawPassword)
                .otpCode(otpCode)
                .expiresAt(now.plusSeconds(OTP_VALID_DURATION_SECONDS))
                .lastSentAt(now)
                .build();

        otpStore.put(email.toLowerCase(), pending);
        log.info("================================================================================");
        log.info("[OTP SERVICE] ĐÃ PHÁT MÃ XÁC THỰC CHO GMAIL: {}", email);
        log.info("[OTP SERVICE] MÃ OTP 6 CHỮ SỐ LÀ: >>> {} <<< (Hiệu lực trong 5 phút)", otpCode);
        log.info("================================================================================");
    }

    @Override
    public boolean canResendOtp(String email) {
        PendingRegistration pending = otpStore.get(email.toLowerCase());
        if (pending == null) {
            return true;
        }
        Instant now = Instant.now();
        long elapsedSeconds = now.getEpochSecond() - pending.getLastSentAt().getEpochSecond();
        return elapsedSeconds >= RESEND_COOLDOWN_SECONDS;
    }

    @Override
    public void updateOtp(String email, String newOtpCode) {
        PendingRegistration pending = otpStore.get(email.toLowerCase());
        if (pending != null) {
            Instant now = Instant.now();
            pending.setOtpCode(newOtpCode);
            pending.setExpiresAt(now.plusSeconds(OTP_VALID_DURATION_SECONDS));
            pending.setLastSentAt(now);

            log.info("================================================================================");
            log.info("[OTP SERVICE] ĐÃ CẬP NHẬT GỬI LẠI MÃ CHO GMAIL: {}", email);
            log.info("[OTP SERVICE] MÃ OTP MỚI LÀ: >>> {} <<< (Hiệu lực trong 5 phút)", newOtpCode);
            log.info("================================================================================");
        }
    }

    @Override
    public boolean verifyOtp(String email, String inputOtp) {
        PendingRegistration pending = otpStore.get(email.toLowerCase());
        if (pending == null) {
            log.warn("[OTP SERVICE] Không tìm thấy phiên đăng ký cho email: {}", email);
            return false;
        }

        Instant now = Instant.now();
        if (now.isAfter(pending.getExpiresAt())) {
            log.warn("[OTP SERVICE] Mã OTP cho email {} đã hết hạn!", email);
            otpStore.remove(email.toLowerCase());
            return false;
        }

        boolean matched = pending.getOtpCode().equals(inputOtp.trim());
        if (!matched) {
            log.warn("[OTP SERVICE] Mã OTP không khớp cho email {}. Nhập: {}, Đúng: {}", email, inputOtp, pending.getOtpCode());
        }
        return matched;
    }

    @Override
    public String getPendingPassword(String email) {
        PendingRegistration pending = otpStore.get(email.toLowerCase());
        return pending != null ? pending.getPassword() : null;
    }

    @Override
    public void removePendingRegistration(String email) {
        otpStore.remove(email.toLowerCase());
        log.info("[OTP SERVICE] Đã dọn dẹp phiên OTP thành công cho: {}", email);
    }
}
