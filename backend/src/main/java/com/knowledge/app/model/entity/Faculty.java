package com.knowledge.app.model.entity;

import com.knowledge.app.model.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Khoa / chuyên ngành, luôn thuộc về một trường (Institution).
 * Trong cùng một trường, tên khoa không được trùng.
 */
@Entity
@Table(name = "faculties", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "institution_id", "name" }),
        @UniqueConstraint(columnNames = { "id", "institution_id" })
})
public class Faculty extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @Column(nullable = false, length = 255)
    private String name;

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
