package com.anne.laserengraving.exception;

/**
 * Ngoại lệ ném ra khi vi phạm các quy tắc an toàn vận hành khắc Laser (Laser Safety Rules)
 */
public class LaserSafetyException extends RuntimeException {
    public LaserSafetyException(String message) {
        super(message);
    }
}
