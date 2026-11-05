package com.knowledge.app.service.user;
import com.knowledge.app.repository.*;
import com.knowledge.app.model.entity.UserProfile;
import com.knowledge.app.model.dto.response.user.OrganizationInfo;
import com.knowledge.app.model.dto.request.user.UpdateProfileRequest;
import com.knowledge.app.service.organization.OrganizationService;
import com.knowledge.app.common.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
@Service @Transactional(readOnly=true)
public class ProfileService {
 private final UserProfileRepository profiles;private final UserRepository users;private final OrganizationService organizations;private final AvatarStorage avatars;
 public ProfileService(UserProfileRepository profiles,UserRepository users,OrganizationService organizations,AvatarStorage avatars){this.avatars=avatars;this.profiles=profiles;this.users=users;this.organizations=organizations;}
 public ProfileDetails details(Long userId){return profiles.findByUserId(userId).map(p->new ProfileDetails(p.getDisplayName(),p.getBio(),p.getAvatarUrl())).orElse(new ProfileDetails(null,null,null));}
 public record ProfileDetails(String displayName,String bio,String avatarUrl){}
 @Transactional public void avatar(Long userId,org.springframework.web.multipart.MultipartFile file){var profile=profiles.findByUserId(userId).orElseGet(()->{var p=new UserProfile();p.setUser(users.getReferenceById(userId));return p;});String old=profile.getAvatarUrl();profile.setAvatarUrl(avatars.save(file));profiles.saveAndFlush(profile);if(old!=null&&profiles.countByAvatarUrl(old)==0)avatars.deleteAfterCommit(old);}
 public void validateOrganization(Long institutionId,Long facultyId){organizations.resolve(institutionId,facultyId);}
 public OrganizationInfo organization(Long userId){return profiles.findByUserId(userId).map(p->organizations.info(p.getInstitution(),p.getFaculty())).orElse(null);}
 @Transactional public void update(Long userId,UpdateProfileRequest request){
  var selection=organizations.resolve(request.institutionId(),request.facultyId());
  var profile=profiles.findByUserId(userId).orElseGet(()->{var p=new UserProfile();p.setUser(users.findById(userId).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Không tìm thấy tài khoản")));return p;});
  if(request.displayName()!=null)profile.setDisplayName(request.displayName());if(request.bio()!=null)profile.setBio(request.bio());profile.setInstitution(selection.institution());profile.setFaculty(selection.faculty());profiles.saveAndFlush(profile);
 }
}
