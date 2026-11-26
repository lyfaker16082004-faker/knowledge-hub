package com.knowledge.app.security.handler;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Ghi lỗi dạng JSON ra response, cùng định dạng với ApiResponse:
 * { "success": false, "message": "...", "data": null }
 *
 * Dùng trong tầng security (filter, handler) vì lúc đó chưa vào tới controller,
 * nên GlobalExceptionHandler không bắt được lỗi.
 */
public final class JsonErrorWriter {

    private JsonErrorWriter() {
        // Chỉ có hàm static
    }

    public static void write(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\",\"data\":null}");
    }
}
