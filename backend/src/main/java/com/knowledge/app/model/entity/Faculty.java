package com.knowledge.app.model.entity;
import com.knowledge.app.model.entity.base.BaseEntity;
import jakarta.persistence.*;
@Entity @Table(name="faculties",uniqueConstraints={@UniqueConstraint(columnNames={"institution_id","name"}),@UniqueConstraint(columnNames={"id","institution_id"})})
public class Faculty extends BaseEntity {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="institution_id",nullable=false) private Institution institution;
 @Column(nullable=false,length=255) private String name;
 public Institution getInstitution(){return institution;} public void setInstitution(Institution value){institution=value;}
 public String getName(){return name;} public void setName(String value){name=value;}
}
