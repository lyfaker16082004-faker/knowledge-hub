package com.knowledge.app.common.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.knowledge.app.common.response.ApiResponse;

import jakarta.persistence.OptimisticLockException;

/**
 * Bắt lỗi chung cho toàn bộ controller và trả về JSON thống nhất:
 * { "success": false, "message": "...", "data": null }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** File tải lên quá lớn -> 413. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiResponse<Void>> handleUploadTooLarge(Exception e) {
        return error(413, "Dung lượng tệp tối đa 20 MB");
    }

    /** Lỗi nghiệp vụ do chính code ném ra (ApiException) -> dùng status trong exception. */
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiResponse<Void>> handleApiException(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(new ApiResponse<>(false, e.getMessage(), null));
    }

    /** Dữ liệu gửi lên sai định dạng hoặc không qua được validation (@NotBlank, @Size...) -> 400. */
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HandlerMethodValidationException.class,
            jakarta.validation.ConstraintViolationException.class,
            HttpMessageNotReadableException.class })
    ResponseEntity<ApiResponse<Void>> handleValidation(Exception e) {
        return error(400, "Dữ liệu không hợp lệ. Kiểm tra các trường bắt buộc và giới hạn độ dài; "
                + "mật khẩu cần ít nhất 8 ký tự và tối đa 72 byte UTF-8.");
    }

    /** Vi phạm ràng buộc database (trùng dữ liệu, khoá ngoại...) -> 409. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiResponse<Void>> handleDataConflict(DataIntegrityViolationException e) {
        return error(409, "Dữ liệu đã tồn tại hoặc đang được tham chiếu, không thể thực hiện thao tác.");
    }

    /** Hai người cùng sửa một dữ liệu (optimistic locking) -> 409. */
    @ExceptionHandler({ OptimisticLockingFailureException.class, OptimisticLockException.class })
    ResponseEntity<ApiResponse<Void>> handleConcurrentUpdate(Exception e) {
        return error(409, "Dữ liệu vừa được thay đổi. Vui lòng tải lại.");
    }

    private ResponseEntity<ApiResponse<Void>> error(int status, String message) {
        return ResponseEntity.status(status).body(new ApiResponse<>(false, message, null));
    }
}
