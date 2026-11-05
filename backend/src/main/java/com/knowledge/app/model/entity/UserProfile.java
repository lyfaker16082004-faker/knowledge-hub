package com.knowledge.app.model.entity;

import com.knowledge.app.model.entity.base.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_profiles")
public class UserProfile extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "display_name", nullable = true, length = 100)
    private String displayName;

    @Column(name = "bio", nullable = true, columnDefinition = "TEXT")
    private String bio;

    @Column(name = "avatar_url", nullable = true, length = 2048)
    private String avatarUrl;

    @Column(name = "website_url", nullable = true, length = 2048)
    private String websiteUrl;

    @Column(name = "location", nullable = true, length = 150)
    private String location;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="institution_id")
    private Institution institution;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="faculty_id")
    private Faculty faculty;
    public Institution getInstitution(){return institution;}
    public void setInstitution(Institution value){institution=value;}
    public Faculty getFaculty(){return faculty;}
    public void setFaculty(Faculty value){faculty=value;}

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
