package com.knowledge.app.service.user;
import com.knowledge.app.repository.*;
import com.knowledge.app.model.entity.*;
import com.knowledge.app.common.exception.ApiException;
import com.knowledge.app.common.response.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import java.time.LocalDateTime;
@Service @Transactional(readOnly=true)
public class CommunityService {
 private final UserRepository users;private final UserProfileRepository profiles;private final UserFollowRepository follows;private final PostRepository posts;
 public CommunityService(UserRepository users,UserProfileRepository profiles,UserFollowRepository follows,PostRepository posts){this.users=users;this.profiles=profiles;this.follows=follows;this.posts=posts;}
 public record Member(Long id,String username,String displayName,String bio,String avatarUrl,String institutionName,String facultyName,long followerCount,long followingCount,boolean following){}
 public record PostItem(Long id,String title,String excerpt,LocalDateTime publishedAt,Long authorId,String authorName){}
 public PageResponse<Member> list(String q,Long viewer,int page,int size){var data=users.community(q.trim(),PageRequest.of(page,size,Sort.by("id").descending()));return memberPage(data,viewer);}
 public Member get(Long id,Long viewer){return member(active(id),viewer);}
 public PageResponse<Member> following(Long viewer,int page,int size){var data=follows.followingPage(viewer,PageRequest.of(page,size,Sort.by("id").descending()));return new PageResponse<>(data.getContent().stream().map(f->member(f.getFollowing(),viewer)).toList(),data.getTotalElements(),data.getTotalPages(),page,size);}
 public PageResponse<Member> followers(Long viewer,int page,int size){var data=follows.followersPage(viewer,PageRequest.of(page,size,Sort.by("id").descending()));return new PageResponse<>(data.getContent().stream().map(f->member(f.getFollower(),viewer)).toList(),data.getTotalElements(),data.getTotalPages(),page,size);}
 @Transactional public Member follow(Long viewer,Long target,boolean enabled){if(viewer.equals(target))throw new ApiException(HttpStatus.BAD_REQUEST,"Bạn không thể theo dõi chính mình");var author=active(target);var previous=follows.findByFollowerIdAndFollowingId(viewer,target);if(enabled&&previous.isEmpty()){var relation=new UserFollow();relation.setFollower(active(viewer));relation.setFollowing(author);follows.saveAndFlush(relation);}else if(!enabled){previous.ifPresent(follows::delete);follows.flush();}return member(author,viewer);}
 public PageResponse<PostItem> feed(Long viewer,int page,int size){return postPage(posts.followedPosts(viewer,PageRequest.of(page,size,Sort.by("publishedAt").descending().and(Sort.by("id").descending()))));}
 public PageResponse<PostItem> authorPosts(Long author,Long viewer,int page,int size){active(author);return postPage(posts.authorPosts(author,viewer!=null&&follows.existsByFollowerIdAndFollowingId(viewer,author),PageRequest.of(page,size,Sort.by("publishedAt").descending().and(Sort.by("id").descending()))));}
 private PageResponse<PostItem> postPage(Page<Post> data){return new PageResponse<>(data.getContent().stream().map(p->new PostItem(p.getId(),p.getTitle(),excerpt(p),p.getPublishedAt(),p.getAuthor().getId(),p.getAuthor().getUsername())).toList(),data.getTotalElements(),data.getTotalPages(),data.getNumber(),data.getSize());}
 private String excerpt(Post post){String text=post.getSummary()==null?post.getContent():post.getSummary();return text.length()>500?text.substring(0,500)+"…":text;}
 private PageResponse<Member> memberPage(Page<User> data,Long viewer){return new PageResponse<>(data.getContent().stream().map(u->member(u,viewer)).toList(),data.getTotalElements(),data.getTotalPages(),data.getNumber(),data.getSize());}
 private Member member(User u,Long viewer){var p=profiles.findByUserId(u.getId()).orElse(null);return new Member(u.getId(),u.getUsername(),p==null?null:p.getDisplayName(),p==null?null:p.getBio(),p==null?null:p.getAvatarUrl(),p==null||p.getInstitution()==null?null:p.getInstitution().getName(),p==null||p.getFaculty()==null?null:p.getFaculty().getName(),follows.followers(u.getId()),follows.following(u.getId()),viewer!=null&&follows.existsByFollowerIdAndFollowingId(viewer,u.getId()));}
 private User active(Long id){return users.findById(id).filter(u->"ACTIVE".equals(u.getStatus())).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Không tìm thấy thành viên"));}
}
