package com.knowledge.app.security.handler;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Được gọi khi người dùng ĐÃ ĐĂNG NHẬP nhưng KHÔNG CÓ QUYỀN (VD: user thường gọi API admin) -> trả lỗi 403.
 */
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException exception) throws IOException {
        JsonErrorWriter.write(response, 403, "Bạn không có quyền thực hiện thao tác này");
    }
}
