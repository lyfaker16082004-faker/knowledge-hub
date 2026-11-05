package com.knowledge.app.model.entity;
import jakarta.persistence.*;
import com.knowledge.app.model.entity.base.BaseEntity;
@Entity @Table(name="subjects") public class Subject extends BaseEntity {
 @Column(nullable=false,length=150) private String name;
 @Column(nullable=false,length=160) private String slug;
 public String getName(){return name;}public void setName(String value){name=value;}
 public String getSlug(){return slug;}public void setSlug(String value){slug=value;}
}
