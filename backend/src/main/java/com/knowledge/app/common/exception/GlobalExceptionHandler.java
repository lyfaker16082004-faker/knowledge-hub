package com.knowledge.app.common.exception;
import com.knowledge.app.common.response.ApiResponse;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
@RestControllerAdvice
public class GlobalExceptionHandler {
 @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
 ResponseEntity<ApiResponse<Void>> uploadTooLarge(Exception e){return ResponseEntity.status(413).body(new ApiResponse<>(false,"Dung lượng tệp tối đa 20 MB",null));}
 @ExceptionHandler(ApiException.class)
 ResponseEntity<ApiResponse<Void>> api(ApiException e) { return ResponseEntity.status(e.getStatus()).body(new ApiResponse<>(false,e.getMessage(),null)); }
 @ExceptionHandler({MethodArgumentNotValidException.class, HandlerMethodValidationException.class, org.springframework.http.converter.HttpMessageNotReadableException.class})
 ResponseEntity<ApiResponse<Void>> validation(Exception e) { return ResponseEntity.badRequest().body(new ApiResponse<>(false,"Dữ liệu không hợp lệ. Kiểm tra các trường bắt buộc và giới hạn độ dài; mật khẩu cần ít nhất 8 ký tự và tối đa 72 byte UTF-8.",null)); }
 @ExceptionHandler(DataIntegrityViolationException.class)
 ResponseEntity<ApiResponse<Void>> conflict(DataIntegrityViolationException e) { return ResponseEntity.status(409).body(new ApiResponse<>(false,"Dữ liệu đã tồn tại hoặc đang được tham chiếu, không thể thực hiện thao tác.",null)); }
 @ExceptionHandler({OptimisticLockingFailureException.class, jakarta.persistence.OptimisticLockException.class})
 ResponseEntity<ApiResponse<Void>> concurrent(Exception e) { return ResponseEntity.status(409).body(new ApiResponse<>(false,"Dữ liệu vừa được thay đổi. Vui lòng tải lại.",null)); }
}
