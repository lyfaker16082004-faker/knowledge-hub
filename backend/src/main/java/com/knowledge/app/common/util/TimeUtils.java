package com.knowledge.app.common.util;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Các hàm xử lý thời gian. Toàn bộ ứng dụng lưu thời gian theo múi giờ UTC.
 */
public final class TimeUtils {

    private TimeUtils() {
        // Class tiện ích, chỉ có hàm static
    }

    /** Thời gian hiện tại theo UTC. */
    public static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    /** Thời gian hiện tại theo UTC, dạng Timestamp để truyền vào câu SQL (JdbcTemplate). */
    public static Timestamp nowTimestamp() {
        return Timestamp.valueOf(now());
    }
}
