package com.knowledge.app.common.response;

/**
 * Dạng dữ liệu chung mà mọi API trả về: success, message và data.
 */
public record ApiResponse<T>(boolean success, String message, T data) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "Success", data);
    }
}