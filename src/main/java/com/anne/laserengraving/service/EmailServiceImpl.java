package com.anne.laserengraving.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Triển khai gửi Transactional Email qua Resend REST API (https://resend.com).
 * Không phụ thuộc thư viện bên ngoài, tích hợp bảo vệ chống lỗi và tự động fallback nếu chưa cấu hình API Key.
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final String apiKey;
    private final String fromEmail;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public EmailServiceImpl(
            @Value("${app.mail.api-key:}") String apiKey,
            @Value("${app.mail.from-email:Laser Studio <onboarding@resend.dev>}") String fromEmail
    ) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.fromEmail = fromEmail;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void sendOtpEmail(String toEmail, String otpCode) {
        if (apiKey.isBlank()) {
            log.info("[EMAIL SERVICE] Chưa cấu hình API Key (biến môi trường RESEND_API_KEY hoặc app.mail.api-key). " +
                     "Hệ thống phát mã xác thực tại console log: >>> {} <<< cho tài khoản: {}", otpCode, toEmail);
            return;
        }

        try {
            String htmlContent = buildOtpHtmlTemplate(toEmail, otpCode);

            Map<String, Object> payload = Map.of(
                    "from", fromEmail,
                    "to", List.of(toEmail),
                    "subject", "Laser Studio - Mã xác thực OTP kích hoạt tài khoản [" + otpCode + "]",
                    "html", htmlContent
            );

            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("[EMAIL SERVICE] Đã gửi thành công email OTP tới: {} qua Resend API (HTTP {})", toEmail, response.statusCode());
            } else {
                log.error("[EMAIL SERVICE] Gửi email thất bại tới {}. Mã lỗi HTTP {}: {}", toEmail, response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("[EMAIL SERVICE] Ngoại lệ khi gửi email OTP tới {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildOtpHtmlTemplate(String email, String otpCode) {
        return """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
              <meta charset="UTF-8">
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f4f6fa; margin: 0; padding: 24px; color: #1e293b; }
                .card { max-width: 520px; margin: 0 auto; background: #ffffff; border-radius: 12px; border: 1px solid #e2e8f0; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.05); }
                .header { background: #0b1c30; padding: 24px; text-align: center; border-bottom: 3px solid #f97316; }
                .logo { font-size: 20px; font-weight: 800; color: #ffffff; letter-spacing: 1.5px; text-transform: uppercase; }
                .subtitle { font-size: 11px; color: #94a3b8; margin-top: 4px; letter-spacing: 0.5px; }
                .body { padding: 32px 28px; text-align: center; }
                .greeting { font-size: 15px; font-weight: 600; color: #0f172a; margin-bottom: 12px; }
                .text { font-size: 13px; line-height: 1.6; color: #475569; margin-bottom: 24px; }
                .otp-box { display: inline-block; background: #eff6ff; border: 2px dashed #3b82f6; border-radius: 8px; padding: 14px 32px; margin: 10px 0 24px; }
                .otp-code { font-family: 'Courier New', Courier, monospace; font-size: 32px; font-weight: 800; color: #1d4ed8; letter-spacing: 8px; }
                .warning { font-size: 12px; color: #b45309; background: #fffbeb; border: 1px solid #fde68a; border-radius: 6px; padding: 10px 14px; text-align: left; margin-bottom: 20px; }
                .footer { background: #f8fafc; padding: 16px; text-align: center; font-size: 11px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="card">
                <div class="header">
                  <div class="logo">⚡ LASER STUDIO</div>
                  <div class="subtitle">HỆ THỐNG ĐIỀU KHIỂN & KHẮC LASER 2 TRỤC X-Y</div>
                </div>
                <div class="body">
                  <div class="greeting">Xin chào bạn,</div>
                  <div class="text">
                    Bạn vừa thực hiện yêu cầu đăng ký hoặc kích hoạt tài khoản tại <strong>Laser Studio</strong> cho địa chỉ Gmail: <strong>%s</strong>.<br>
                    Vui lòng sử dụng mã xác thực OTP 6 chữ số dưới đây để hoàn tất:
                  </div>
                  <div class="otp-box">
                    <div class="otp-code">%s</div>
                  </div>
                  <div class="warning">
                    ⚠️ <strong>Lưu ý quan trọng:</strong> Mã này có hiệu lực trong vòng <strong>5 phút</strong>. Tuyệt đối không chia sẻ mã này cho bất kỳ ai khác để bảo đảm an toàn tài khoản.
                  </div>
                  <div class="text" style="font-size: 12px; color: #64748b; margin-bottom: 0;">
                    Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email.
                  </div>
                </div>
                <div class="footer">
                  © 2026 Laser Studio. Email được gửi tự động qua Resend Transactional Email API.
                </div>
              </div>
            </body>
            </html>
            """.formatted(email, otpCode);
    }
}
