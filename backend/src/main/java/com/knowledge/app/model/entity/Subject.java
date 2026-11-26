package com.knowledge.app.model.entity;

import com.knowledge.app.model.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Môn học (bảng "subjects").
 */
@Entity
@Table(name = "subjects")
public class Subject extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 160)
    private String slug;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }
}
