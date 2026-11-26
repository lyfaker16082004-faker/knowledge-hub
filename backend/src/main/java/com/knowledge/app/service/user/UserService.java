package com.knowledge.app.service.user;
import com.knowledge.app.model.dto.request.user.*;
import com.knowledge.app.model.dto.response.user.UserResponse;
import com.knowledge.app.common.response.PageResponse;
/**
 * Quản lý tài khoản: xem, tạo, sửa, đặt lại mật khẩu và xoá mềm.
 */
public interface UserService {
 UserResponse get(Long id);
 PageResponse<UserResponse> list(String query,int page,int size);
 UserResponse create(AdminCreateUserRequest request);
 UserResponse update(Long id,AdminUpdateUserRequest request);
 UserResponse softDelete(Long id);
 void changePassword(Long id,ChangePasswordRequest request);
 void resetPassword(Long id,ResetPasswordRequest request);
 void bootstrapAdmin(String username,String email,String password);
}
