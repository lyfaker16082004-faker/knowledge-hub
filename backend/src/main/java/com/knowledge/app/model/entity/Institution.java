package com.knowledge.app.model.entity;
import com.knowledge.app.model.entity.base.BaseEntity;
import jakarta.persistence.*;
@Entity @Table(name="institutions")
public class Institution extends BaseEntity {
 @Column(nullable=false,length=255,unique=true) private String name;
 @Column(nullable=false,length=20) private String type;
 public String getName(){return name;} public void setName(String name){this.name=name;}
 public String getType(){return type;} public void setType(String type){this.type=type;}
}
