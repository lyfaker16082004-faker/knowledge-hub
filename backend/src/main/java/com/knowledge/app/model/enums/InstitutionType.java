package com.knowledge.app.model.enums;

/**
 * Loại trường / tổ chức.
 *
 * Database vẫn lưu dạng chuỗi (VD: "UNIVERSITY"). Dùng enum để không gõ sai chuỗi trong code.
 */
public enum InstitutionType {

    UNIVERSITY, // Trường đại học
    COMPANY, // Công ty
    OTHER; // Khác (không có khoa)

    /** So sánh với giá trị chuỗi lấy từ database. VD: InstitutionType.UNIVERSITY.is(entity.getStatus()) */
    public boolean is(String value) {
        return name().equals(value);
    }
}
