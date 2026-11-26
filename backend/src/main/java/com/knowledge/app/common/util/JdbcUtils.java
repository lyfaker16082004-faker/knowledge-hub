package com.knowledge.app.common.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Các hàm hỗ trợ khi viết SQL thuần với JdbcTemplate.
 */
public final class JdbcUtils {

    private JdbcUtils() {
        // Class tiện ích, chỉ có hàm static
    }

    /** Chạy câu SQL đếm (SELECT COUNT(*) ...) và trả về số. Kết quả null thì trả về 0. */
    public static long queryCount(JdbcTemplate jdbcTemplate, String sql, Object... params) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, params);
        return value != null ? value : 0;
    }

    /** Đọc cột thời gian; cột null thì trả về null. */
    public static LocalDateTime readTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value != null ? value.toLocalDateTime() : null;
    }

    /** Đọc cột số có thể null (rs.getLong trả về 0 khi null, nên phải kiểm tra wasNull). */
    public static Long readNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
