package com.anne.laserengraving.core.gcode;

import com.anne.laserengraving.exception.LaserSafetyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Kiểm định an toàn tuyệt đối cho G-code trước khi lưu hoặc xuất dữ liệu
 * Tuân thủ [rules/laser-safety-rules.md] và [docs/business-rules.md]
 */
@Component
public class GcodeSafetyValidator {

    @Value("${app.laser.work-area.max-width-mm:300.0}")
    private double maxWidthMm;

    @Value("${app.laser.work-area.max-height-mm:200.0}")
    private double maxHeightMm;

    @Value("${app.laser.limits.max-power:1000}")
    private int maxPowerLimit;

    @Value("${app.laser.limits.min-speed:100}")
    private int minSpeedLimit;

    @Value("${app.laser.limits.max-speed:5000}")
    private int maxSpeedLimit;

    /**
     * Xác thực các tham số đầu vào của người dùng bao gồm tọa độ gốc và kích thước
     */
    public void validateParameters(double originXMm, double originYMm, double targetWidthMm, double targetHeightMm, int speed, int power, int dpi) {
        if (originXMm < 0 || originYMm < 0) {
            throw new LaserSafetyException("Tọa độ gốc gia công (Origin X, Y) không được âm.");
        }
        if (targetWidthMm <= 0 || targetHeightMm <= 0) {
            throw new LaserSafetyException("Kích thước khắc phải lớn hơn 0 mm.");
        }
        if ((originXMm + targetWidthMm) > maxWidthMm || (originYMm + targetHeightMm) > maxHeightMm) {
            throw new LaserSafetyException(String.format(
                    "Vùng khắc vượt quá khổ làm việc an toàn của máy (Max: %s x %s mm). Tọa độ & kích thước: X=[%.1f -> %.1f mm], Y=[%.1f -> %.1f mm]",
                    maxWidthMm, maxHeightMm, originXMm, originXMm + targetWidthMm, originYMm, originYMm + targetHeightMm));
        }
        if (power < 0 || power > maxPowerLimit) {
            throw new LaserSafetyException(String.format(
                    "Công suất Laser S=%d vượt quá dải an toàn cho phép [0, %d].", power, maxPowerLimit));
        }
        if (speed < minSpeedLimit || speed > maxSpeedLimit) {
            throw new LaserSafetyException(String.format(
                    "Tốc độ khắc F=%d mm/min nằm ngoài dải an toàn [%d, %d] mm/min. Tốc độ quá thấp gây nguy cơ cháy phôi!",
                    speed, minSpeedLimit, maxSpeedLimit));
        }
        if (dpi < 100 || dpi > 600) {
            throw new LaserSafetyException(String.format(
                    "Độ phân giải DPI=%d nằm ngoài khoảng cho phép [100, 600].", dpi));
        }
    }

    /**
     * Phương thức tương thích ngược khi không truyền tọa độ gốc (mặc định tại 0, 0)
     */
    public void validateParameters(double targetWidthMm, double targetHeightMm, int speed, int power, int dpi) {
        validateParameters(0.0, 0.0, targetWidthMm, targetHeightMm, speed, power, dpi);
    }

    /**
     * Kiểm tra từng dòng G-code đã sinh để đảm bảo không vi phạm quy tắc an toàn
     */
    public void validateGcodeLines(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty() || line.startsWith(";")) {
                continue;
            }

            // Quy tắc an toàn BR-07: Không bao giờ di chuyển G0 khi laser đang bật
            if (line.startsWith("G0") && (line.contains("M3") || line.contains("M4") || line.contains("S"))) {
                throw new LaserSafetyException(String.format(
                        "Lỗi an toàn tại dòng %d: Lệnh di chuyển nhanh G0 cấm đi kèm bật tia laser (%s)", i + 1, line));
            }
        }
    }
}
