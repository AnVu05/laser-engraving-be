package com.anne.laserengraving.core.image;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FloydSteinbergDitherTest {

    private FloydSteinbergDither dither;

    @BeforeEach
    void setUp() {
        dither = new FloydSteinbergDither();
    }

    @Test
    @DisplayName("Kiểm tra thuật toán Dither trả về ma trận nhị phân 0 và 1 đúng kích thước")
    void testDitherDimensionsAndBinaryValues() {
        int width = 10;
        int height = 10;
        float[][] gray = new float[width][height];

        // Tạo gradient từ 0 (đen) đến 255 (trắng)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                gray[x][y] = (float) (x * 25.5);
            }
        }

        int[][] binary = dither.dither(gray, width, height);

        assertNotNull(binary);
        assertEquals(width, binary.length);
        assertEquals(height, binary[0].length);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int val = binary[x][y];
                assertTrue(val == 0 || val == 1, "Giá trị pixel bắt buộc phải là 0 hoặc 1 nhưng nhận được: " + val);
            }
        }
    }

    @Test
    @DisplayName("Kiểm tra vùng tối cho kết quả 1 (đốt laser) và vùng sáng cho kết quả 0 (tắt laser)")
    void testPureBlackAndWhite() {
        int width = 4;
        int height = 4;

        float[][] black = new float[width][height]; // toàn 0.0
        int[][] binaryBlack = dither.dither(black, width, height);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                assertEquals(1, binaryBlack[x][y], "Điểm ảnh đen tuyệt đối phải có bit 1 (cần đốt laser)");
            }
        }

        float[][] white = new float[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                white[x][y] = 255.0f;
            }
        }
        int[][] binaryWhite = dither.dither(white, width, height);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                assertEquals(0, binaryWhite[x][y], "Điểm ảnh trắng tinh phải có bit 0 (tắt laser)");
            }
        }
    }
}
