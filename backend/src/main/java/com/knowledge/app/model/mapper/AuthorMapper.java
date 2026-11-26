package com.knowledge.app.model.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.knowledge.app.model.dto.response.post.Author;

/**
 * Đọc thông tin tác giả từ kết quả câu SQL.
 * Câu SQL phải có các cột: author_id, username, display_name, avatar_url.
 */
public final class AuthorMapper {

    private AuthorMapper() {
        // Chỉ có hàm static
    }

    public static Author fromResultSet(ResultSet rs) throws SQLException {
        return new Author(
                rs.getLong("author_id"),
                rs.getString("username"),
                rs.getString("display_name"),
                rs.getString("avatar_url"));
    }
}
