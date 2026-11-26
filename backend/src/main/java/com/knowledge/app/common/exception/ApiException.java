package com.knowledge.app.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Lỗi nghiệp vụ có kèm mã HTTP. Ném ra trong service, GlobalExceptionHandler sẽ đổi thành JSON trả về.
 */
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
