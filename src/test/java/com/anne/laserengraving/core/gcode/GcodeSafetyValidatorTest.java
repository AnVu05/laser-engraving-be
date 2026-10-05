package com.anne.laserengraving.core.gcode;

import com.anne.laserengraving.exception.LaserSafetyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GcodeSafetyValidatorTest {

    private GcodeSafetyValidator validator;

    @BeforeEach
    void setUp() {
        validator = new GcodeSafetyValidator();
        ReflectionTestUtils.setField(validator, "maxWidthMm", 300.0);
        ReflectionTestUtils.setField(validator, "maxHeightMm", 200.0);
        ReflectionTestUtils.setField(validator, "maxPowerLimit", 1000);
        ReflectionTestUtils.setField(validator, "minSpeedLimit", 100);
        ReflectionTestUtils.setField(validator, "maxSpeedLimit", 5000);
    }

    @Test
    @DisplayName("Ném ngoại lệ an toàn khi kích thước vượt quá khổ máy 300x200mm")
    void testDimensionsExceed() {
        assertThrows(LaserSafetyException.class, () ->
                validator.validateParameters(350.0, 100.0, 1500, 800, 254));

        assertThrows(LaserSafetyException.class, () ->
                validator.validateParameters(100.0, 250.0, 1500, 800, 254));
    }

    @Test
    @DisplayName("Ném ngoại lệ an toàn khi công suất laser vượt quá 1000")
    void testPowerExceed() {
        assertThrows(LaserSafetyException.class, () ->
                validator.validateParameters(100.0, 100.0, 1500, 1200, 254));
    }

    @Test
    @DisplayName("Ném ngoại lệ an toàn khi tốc độ khắc quá chậm < 100 mm/min gây nguy cơ cháy phôi")
    void testSpeedTooSlow() {
        assertThrows(LaserSafetyException.class, () ->
                validator.validateParameters(100.0, 100.0, 50, 800, 254));
    }

    @Test
    @DisplayName("Ném ngoại lệ an toàn BR-07 khi phát hiện lệnh di chuyển nhanh G0 đi kèm bật laser")
    void testG0WithLaserActive() {
        List<String> dangerousLines = List.of(
                "G21",
                "G90",
                "G0 X10.0 Y10.0 M3 S500", // VI PHẠM NGUY HIỂM
                "M5"
        );

        assertThrows(LaserSafetyException.class, () ->
                validator.validateGcodeLines(dangerousLines));
    }

    @Test
    @DisplayName("Chấp nhận các tham số và dòng lệnh hợp lệ theo tiêu chuẩn an toàn")
    void testValidParametersAndLines() {
        assertDoesNotThrow(() ->
                validator.validateParameters(150.0, 100.0, 1500, 800, 254));

        List<String> safeLines = List.of(
                "G21",
                "G90",
                "$32=1",
                "M5",
                "G0 X10.0 Y10.0",
                "G1 X15.0 Y10.0 F1500 M3 S800",
                "M5",
                "G0 X0.0 Y0.0",
                "M2"
        );

        assertDoesNotThrow(() -> validator.validateGcodeLines(safeLines));
    }

    @Test
    @DisplayName("Ném ngoại lệ an toàn khi tọa độ gốc âm")
    void testOriginNegative() {
        assertThrows(LaserSafetyException.class, () ->
                validator.validateParameters(-10.0, 0.0, 100.0, 100.0, 1500, 800, 254));

        assertThrows(LaserSafetyException.class, () ->
                validator.validateParameters(0.0, -5.0, 100.0, 100.0, 1500, 800, 254));
    }

    @Test
    @DisplayName("Ném ngoại lệ khi tọa độ gốc + kích thước vượt quá bàn làm việc 300x200mm")
    void testOriginPlusDimensionsExceed() {
        // X = 250 + 100 = 350 > 300
        assertThrows(LaserSafetyException.class, () ->
                validator.validateParameters(250.0, 10.0, 100.0, 50.0, 1500, 800, 254));

        // Y = 10 + 150 + 100 = 210 > 200
        assertThrows(LaserSafetyException.class, () ->
                validator.validateParameters(10.0, 150.0, 100.0, 80.0, 1500, 800, 254));
    }

    @Test
    @DisplayName("Chấp nhận vùng khắc hợp lệ có tọa độ gốc nằm gọn trong khổ bàn máy")
    void testValidOriginAndDimensions() {
        assertDoesNotThrow(() ->
                validator.validateParameters(50.0, 30.0, 100.0, 100.0, 1500, 800, 254));
    }
}
