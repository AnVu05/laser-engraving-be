package com.anne.laserengraving.core.image;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Image Processing Engine: Tiền xử lý ảnh (Resize theo DPI, Grayscale ITU-R BT.601, Dithering)
 */
@Service
@RequiredArgsConstructor
public class ImageProcessingEngine {

    private final FloydSteinbergDither floydSteinbergDither;

    /**
     * Chuyển đổi và điều chỉnh kích thước ảnh gốc sang kích thước pixel mục tiêu dựa trên DPI
     */
    public BufferedImage resizeImage(BufferedImage originalImage, int targetWidthPx, int targetHeightPx) {
        BufferedImage resized = new BufferedImage(targetWidthPx, targetHeightPx, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(originalImage, 0, 0, targetWidthPx, targetHeightPx, null);
        } finally {
            g2d.dispose();
        }
        return resized;
    }

    /**
     * Chuyển đổi ảnh sang ma trận xám theo công thức ITU-R BT.601: Y = 0.299R + 0.587G + 0.114B
     */
    public float[][] toGrayscaleMatrix(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        float[][] gray = new float[width][height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                gray[x][y] = 0.299f * r + 0.587f * g + 0.114f * b;
            }
        }
        return gray;
    }

    /**
     * Binarize ảnh thành ma trận nhị phân (0 và 1) theo thuật toán được chọn
     */
    public int[][] binarize(BufferedImage image, DitherAlgorithm algorithm) {
        float[][] grayMatrix = toGrayscaleMatrix(image);
        int width = image.getWidth();
        int height = image.getHeight();

        if (algorithm == DitherAlgorithm.THRESHOLD) {
            return floydSteinbergDither.threshold(grayMatrix, width, height, 128.0f);
        }
        return floydSteinbergDither.dither(grayMatrix, width, height);
    }
}
