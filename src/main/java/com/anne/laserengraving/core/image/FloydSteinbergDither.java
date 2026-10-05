package com.anne.laserengraving.core.image;

import org.springframework.stereotype.Component;

/**
 * Thuật toán khuếch tán sai số Floyd-Steinberg (Error Diffusion)
 * Phân bổ sai số sang 4 điểm ảnh lân cận theo ma trận trọng số:
 * Phải (+7/16), Dưới-Trái (+3/16), Dưới (+5/16), Dưới-Phải (+1/16)
 */
@Component
public class FloydSteinbergDither {

    public int[][] dither(float[][] grayMatrix, int width, int height) {
        // Tạo bản sao của ma trận xám để tính toán khuếch tán sai số
        float[][] buffer = new float[width][height];
        for (int x = 0; x < width; x++) {
            System.arraycopy(grayMatrix[x], 0, buffer[x], 0, height);
        }

        // Ma trận kết quả nhị phân: 1 = Cần đốt laser (đen), 0 = Tắt laser (trắng)
        int[][] binary = new int[width][height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float oldPixel = buffer[x][y];
                // Ngưỡng 128: < 128 là điểm tối (cần đốt), >= 128 là điểm sáng (tắt)
                int newPixel = (oldPixel < 128.0f) ? 0 : 255;
                binary[x][y] = (newPixel == 0) ? 1 : 0;

                float error = oldPixel - newPixel;

                // Phân bổ sai số
                if (x + 1 < width) {
                    buffer[x + 1][y] += error * (7.0f / 16.0f);
                }
                if (x - 1 >= 0 && y + 1 < height) {
                    buffer[x - 1][y + 1] += error * (3.0f / 16.0f);
                }
                if (y + 1 < height) {
                    buffer[x][y + 1] += error * (5.0f / 16.0f);
                }
                if (x + 1 < width && y + 1 < height) {
                    buffer[x + 1][y + 1] += error * (1.0f / 16.0f);
                }
            }
        }

        return binary;
    }

    public int[][] threshold(float[][] grayMatrix, int width, int height, float thresholdValue) {
        int[][] binary = new int[width][height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                binary[x][y] = (grayMatrix[x][y] < thresholdValue) ? 1 : 0;
            }
        }
        return binary;
    }
}
