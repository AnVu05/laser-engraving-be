package com.anne.laserengraving.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class CryptoUtilTest {

    @Test
    @DisplayName("Tính toán mã băm SHA-256 chính xác từ chuỗi rỗng")
    void testSha256EmptyString() {
        // Known SHA-256 for empty string: e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        String hash = CryptoUtil.calculateSha256("");
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hash);
    }

    @Test
    @DisplayName("Tính toán mã băm SHA-256 chính xác cho chuỗi G-code mẫu")
    void testSha256SampleGcode() {
        String gcode = "G21\nG90\n$32=1\nM5\nG0 X10.0 Y10.0\nG1 X20.0 Y10.0 F1500 M3 S800\nM5\nM2\n";
        String hash = CryptoUtil.calculateSha256(gcode);
        assertNotNull(hash);
        assertEquals(64, hash.length(), "Mã băm SHA-256 phải có đúng 64 ký tự hex");
        assertTrue(hash.matches("^[a-f0-9]{64}$"), "Mã băm phải gồm các ký tự hex thường");
    }

    @Test
    @DisplayName("Tính toán mã băm SHA-256 qua InputStream cho cùng kết quả với byte array")
    void testSha256InputStream() throws IOException {
        String sample = "Makerbase MKS DLC32 V2.1 ESP32 Laser Test";
        byte[] bytes = sample.getBytes(StandardCharsets.UTF_8);

        String hash1 = CryptoUtil.calculateSha256(bytes);
        String hash2 = CryptoUtil.calculateSha256(new ByteArrayInputStream(bytes));

        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Ném ngoại lệ IllegalArgumentException khi đầu vào null")
    void testNullInputThrows() {
        assertThrows(IllegalArgumentException.class, () -> CryptoUtil.calculateSha256((String) null));
        assertThrows(IllegalArgumentException.class, () -> CryptoUtil.calculateSha256((byte[]) null));
    }
}
