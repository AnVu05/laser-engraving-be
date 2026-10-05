package com.anne.laserengraving.core.gcode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class GcodeGeneratorEngineTest {

    private GcodeGeneratorEngine engine;

    @BeforeEach
    void setUp() {
        GcodeSafetyValidator validator = new GcodeSafetyValidator();
        ReflectionTestUtils.setField(validator, "maxWidthMm", 300.0);
        ReflectionTestUtils.setField(validator, "maxHeightMm", 200.0);
        ReflectionTestUtils.setField(validator, "maxPowerLimit", 1000);
        ReflectionTestUtils.setField(validator, "minSpeedLimit", 100);
        ReflectionTestUtils.setField(validator, "maxSpeedLimit", 5000);

        engine = new GcodeGeneratorEngine(validator);
    }

    @Test
    @DisplayName("Kiểm tra bộ sinh mã G-code xuất chuẩn Header, Footer và cú pháp GRBL")
    void testGcodeGenerationSyntax() {
        int widthPx = 5;
        int heightPx = 5;
        int[][] binary = new int[widthPx][heightPx];

        // Đánh dấu 1 pixel ở giữa
        binary[2][2] = 1;

        GcodeModel model = engine.generateRasterGcode(
                binary,
                widthPx,
                heightPx,
                50.0,
                50.0,
                1500,
                800,
                254
        );

        assertNotNull(model);
        String gcode = model.getFullGcodeContent();

        // Kiểm tra Header
        assertTrue(gcode.contains("G21"), "Phải chứa lệnh thiết lập đơn vị mm G21");
        assertTrue(gcode.contains("G90"), "Phải chứa lệnh tọa độ tuyệt đối G90");
        assertTrue(gcode.contains("$32=1"), "Phải chứa lệnh bật Laser Mode $32=1");
        assertTrue(gcode.contains("M5"), "Phải chứa lệnh tắt laser M5");

        // Kiểm tra lệnh khắc
        assertTrue(gcode.contains("M3 S800"), "Phải chứa lệnh bật laser công suất S800");
        assertTrue(gcode.contains("F1500"), "Phải chứa tốc độ khắc F1500");

        // Kiểm tra Footer
        assertTrue(gcode.contains("G0 X0.000 Y0.000"), "Phải quay về gốc tọa độ");
        assertTrue(gcode.contains("M2"), "Phải kết thúc chương trình bằng M2");

        assertTrue(model.getTotalLines() > 10, "Tổng số dòng phải lớn hơn 10");
        assertTrue(model.getFileSizeBytes() > 0, "Dung lượng tệp phải lớn hơn 0");
    }

    @Test
    @DisplayName("Kiểm tra bộ sinh mã G-code áp dụng chính xác tọa độ gốc Origin X-Y")
    void testGcodeGenerationWithOriginOffsets() {
        int widthPx = 5;
        int heightPx = 5;
        int[][] binary = new int[widthPx][heightPx];
        binary[0][0] = 1; // Pixel ở dòng Y cuối cùng (y=0 trong matrix là đáy sau khi đảo raster)

        double originX = 45.0;
        double originY = 35.0;

        GcodeModel model = engine.generateRasterGcode(
                binary,
                widthPx,
                heightPx,
                originX,
                originY,
                50.0,
                50.0,
                254,
                750,
                254
        );

        assertNotNull(model);
        String gcode = model.getFullGcodeContent();

        // Kiểm tra Header chứa thông tin Origin
        assertTrue(gcode.contains("Origin: (X=45.00, Y=35.00) mm"), "Header phải ghi nhận tọa độ gốc phôi");

        // Kiểm tra tọa độ G0 bắt đầu phải có offset originX và originY
        assertTrue(gcode.contains("X45.000") || gcode.contains("Y35.000"), "Tọa độ di chuyển phải được tịnh tiến theo Origin");
    }
}
