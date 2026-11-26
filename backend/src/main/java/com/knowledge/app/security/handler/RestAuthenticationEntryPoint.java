package com.knowledge.app.security.handler;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Được gọi khi người dùng CHƯA ĐĂNG NHẬP mà gọi API cần đăng nhập -> trả lỗi 401.
 */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        JsonErrorWriter.write(response, 401, "Vui lòng đăng nhập bằng JWT hợp lệ");
    }
}
