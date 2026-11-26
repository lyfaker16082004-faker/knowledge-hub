package com.knowledge.app.model.dto.request.post;

import java.util.List;

import com.knowledge.app.common.constant.AppConstants;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Dữ liệu tạo / sửa bài viết.
 * version: chỉ cần khi sửa, dùng để phát hiện người khác đã sửa bài trước đó.
 */
public record PostRequest(
        @NotNull @Pattern(regexp = "QUESTION|ARTICLE")
        String type,

        @NotBlank @Size(min = 10, max = 200, message = "Tiêu đề cần từ 10 đến 200 ký tự")
        String title,

        @NotBlank @Size(min = 20, max = 20000, message = "Nội dung cần từ 20 đến 20.000 ký tự")
        String content,

        @NotNull(message = "Hãy chọn chủ đề") @Positive
        Long categoryId,

        @Size(max = AppConstants.MAX_POST_TAGS, message = "Tối đa 5 thẻ")
        List<@NotBlank @Size(max = 30, message = "Mỗi thẻ tối đa 30 ký tự") String> tags,

        Long version) {
}
