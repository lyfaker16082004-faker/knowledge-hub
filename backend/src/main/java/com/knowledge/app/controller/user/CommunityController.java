package com.knowledge.app.controller.user;
import com.knowledge.app.service.user.CommunityService;
import com.knowledge.app.service.user.CommunityService.*;
import com.knowledge.app.common.response.*;
import com.knowledge.app.security.user.AccountPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/community")
public class CommunityController {
 private final CommunityService community;
 public CommunityController(CommunityService community){this.community=community;}
 @GetMapping("/users") public ApiResponse<PageResponse<Member>> list(@AuthenticationPrincipal AccountPrincipal viewer,@RequestParam(defaultValue="") @Size(max=100) String q,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="12") @Min(1) @Max(50) int size){return ApiResponse.ok(community.list(q,viewer==null?null:viewer.getId(),page,size));}
 @GetMapping("/users/{id}") public ApiResponse<Member> get(@PathVariable Long id,@AuthenticationPrincipal AccountPrincipal viewer){return ApiResponse.ok(community.get(id,viewer==null?null:viewer.getId()));}
 @PostMapping("/users/{id}/follow") public ApiResponse<Member> follow(@PathVariable Long id,@AuthenticationPrincipal AccountPrincipal viewer,@Valid @RequestBody FollowRequest request){return ApiResponse.ok(community.follow(viewer.getId(),id,request.following()));}
 @GetMapping("/following") public ApiResponse<PageResponse<Member>> following(@AuthenticationPrincipal AccountPrincipal viewer,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="12") @Min(1) @Max(50) int size){return ApiResponse.ok(community.following(viewer.getId(),page,size));}
 @GetMapping("/followers") public ApiResponse<PageResponse<Member>> followers(@AuthenticationPrincipal AccountPrincipal viewer,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="12") @Min(1) @Max(50) int size){return ApiResponse.ok(community.followers(viewer.getId(),page,size));}
 @GetMapping("/feed") public ApiResponse<PageResponse<PostItem>> feed(@AuthenticationPrincipal AccountPrincipal viewer,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="10") @Min(1) @Max(50) int size){return ApiResponse.ok(community.feed(viewer.getId(),page,size));}
 @GetMapping("/users/{id}/posts") public ApiResponse<PageResponse<PostItem>> posts(@PathVariable Long id,@AuthenticationPrincipal AccountPrincipal viewer,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="10") @Min(1) @Max(50) int size){return ApiResponse.ok(community.authorPosts(id,viewer==null?null:viewer.getId(),page,size));}
 public record FollowRequest(@NotNull Boolean following){}
}
