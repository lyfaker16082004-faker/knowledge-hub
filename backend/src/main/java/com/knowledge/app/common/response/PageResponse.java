package com.knowledge.app.common.response;

import java.util.List;

/**
 * Dữ liệu có phân trang: danh sách của trang hiện tại kèm tổng số phần tử và tổng số trang.
 */
public record PageResponse<T>(List<T> content, long totalElements, int totalPages, int page, int size) {
}
