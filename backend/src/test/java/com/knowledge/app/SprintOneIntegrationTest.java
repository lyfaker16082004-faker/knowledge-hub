package com.knowledge.app;
import com.knowledge.app.model.entity.*;
import com.knowledge.app.repository.*;
import com.knowledge.app.model.dto.request.user.*;
import com.knowledge.app.service.user.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import java.util.*;
import java.time.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import com.jayway.jsonpath.JsonPath;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class SprintOneIntegrationTest {
 @Autowired MockMvc mvc;@Autowired RoleRepository roles;@Autowired UserService users;@Autowired UserRepository accounts;@Autowired PostRepository posts;@Autowired JwtEncoder encoder;@Autowired UserFollowRepository follows;
 long member,expert,admin;String token,adminToken;static final String PASSWORD="Sprint1!8";
 @BeforeEach void setup()throws Exception{for(String name:Set.of("USER","ADMIN"))if(roles.findByName(name).isEmpty()){var r=new Role();r.setName(name);roles.saveAndFlush(r);}member=users.create(new AdminCreateUserRequest("sprintmember","sprintmember@example.test",PASSWORD,Set.of("USER"))).id();expert=users.create(new AdminCreateUserRequest("sprintexpert","sprintexpert@example.test",PASSWORD,Set.of("USER"))).id();admin=users.create(new AdminCreateUserRequest("sprintadmin","sprintadmin@example.test",PASSWORD,Set.of("ADMIN"))).id();token=login("sprintmember@example.test");adminToken=login("sprintadmin");}
 @Test void emailOnlyRegistrationEightCharactersAndDefaultUser()throws Exception{
  mvc.perform(post("/api/auth/register").contentType("application/json").content("{\"email\":\"emailonly@example.test\",\"password\":\"12345678\",\"roles\":[\"ADMIN\"]}")).andExpect(status().isCreated()).andExpect(jsonPath("$.data.roles[0]").value("USER"));
  mvc.perform(post("/api/auth/register").contentType("application/json").content("{\"email\":\"emailonly@example.test\",\"password\":\"12345678\"}")).andExpect(status().isConflict());
  mvc.perform(post("/api/auth/register").contentType("application/json").content("{\"email\":\"short@example.test\",\"password\":\"1234567\"}")).andExpect(status().isBadRequest());
  mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"username\":\"emailonly@example.test\",\"password\":\"12345678\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.data.expiresIn").value(86400));
 }
 @Test void staleTokenDoesNotBlockFreshLogin()throws Exception{mvc.perform(post("/api/auth/login").header("Authorization","Bearer invalid").contentType("application/json").content("{\"username\":\"sprintmember@example.test\",\"password\":\""+PASSWORD+"\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.data.accessToken").isString());}
 @Test void jwtRejectsTamperingExpiredWrongAudienceAndIssuer()throws Exception{
  String[] parts=token.split("\\.");parts[1]=Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"1\"}".getBytes());reject(String.join(".",parts));
  var user=accounts.findById(member).orElseThrow();
  for(int mode=0;mode<3;mode++){var claims=JwtClaimsSet.builder().issuer(mode==1?"wrong":"knowledge-hub").subject(String.valueOf(member)).audience(List.of(mode==2?"wrong":"knowledge-hub-web")).issuedAt(Instant.now().minusSeconds(120)).expiresAt(Instant.now().plusSeconds(mode==0?-90:3600)).claim("username",user.getUsername()).claim("av",user.getAuthVersion()).build();reject(encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue());}
 }
 @Test void profileBioAndRealAvatarArePersistedAndValidated()throws Exception{
  mvc.perform(put("/api/users/me/profile").header("Authorization","Bearer "+token).contentType("application/json").content("{\"displayName\":\"Sinh viên Việt Nam\",\"bio\":\"Học máy và chia sẻ tri thức 📚\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.data.displayName").value("Sinh viên Việt Nam")).andExpect(jsonPath("$.data.bio").value("Học máy và chia sẻ tri thức 📚"));
  var output=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(20,20,BufferedImage.TYPE_INT_RGB),"png",output);
  var result=mvc.perform(multipart("/api/users/me/avatar").file(new MockMultipartFile("file","avatar.png","image/png",output.toByteArray())).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andReturn();
  String avatar=JsonPath.read(result.getResponse().getContentAsString(),"$.data.avatarUrl");mvc.perform(get(avatar)).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
  mvc.perform(multipart("/api/users/me/avatar").file(new MockMultipartFile("file","bad.png","image/png","fake image".getBytes())).header("Authorization","Bearer "+token)).andExpect(status().isBadRequest());
  mvc.perform(multipart("/api/users/me/avatar").file(new MockMultipartFile("file","big.png","image/png",new byte[2*1024*1024])).header("Authorization","Bearer "+token)).andExpect(status().isPayloadTooLarge());
  mvc.perform(multipart("/api/users/me/avatar").file(new MockMultipartFile("file",output.toByteArray()))).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/community/users/"+member)).andExpect(jsonPath("$.data.bio").value("Học máy và chia sẻ tri thức 📚")).andExpect(jsonPath("$.data.email").doesNotExist());
 }
 @Test void followIsIdempotentPrivateAndDrivesPublishedFeed()throws Exception{
  postFixture("public","PUBLIC","PUBLISHED");postFixture("followers","FOLLOWERS","PUBLISHED");postFixture("private","PRIVATE","PUBLISHED");postFixture("draft","PUBLIC","DRAFT");
  mvc.perform(get("/api/community/users/"+expert+"/posts")).andExpect(jsonPath("$.data.totalElements").value(1));
  mvc.perform(get("/api/community/feed").header("Authorization","Bearer "+token)).andExpect(jsonPath("$.data.totalElements").value(0));
  follow(expert,true);follow(expert,true);assertEquals(1,follows.following(member));
  mvc.perform(get("/api/community/feed").header("Authorization","Bearer "+token)).andExpect(jsonPath("$.data.totalElements").value(2));
  mvc.perform(get("/api/community/following").header("Authorization","Bearer "+token)).andExpect(jsonPath("$.data.content[0].id").value((int)expert));
  mvc.perform(get("/api/community/users/"+expert+"/posts").header("Authorization","Bearer "+token)).andExpect(jsonPath("$.data.totalElements").value(2));
  mvc.perform(post("/api/community/users/"+member+"/follow").header("Authorization","Bearer "+token).contentType("application/json").content("{\"following\":true}")).andExpect(status().isBadRequest());
  mvc.perform(post("/api/community/users/999999999/follow").header("Authorization","Bearer "+token).contentType("application/json").content("{\"following\":true}")).andExpect(status().isNotFound());
  follow(expert,false);follow(expert,false);mvc.perform(get("/api/community/feed").header("Authorization","Bearer "+token)).andExpect(jsonPath("$.data.totalElements").value(0));
  mvc.perform(get("/api/community/following")).andExpect(status().isUnauthorized());
 }
 @Test void adminDetailsAndSoftDeletePreserveDataAndRevokeTokens()throws Exception{
  String expertToken=login("sprintexpert");
  mvc.perform(get("/api/admin/users/"+expert).header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
  mvc.perform(get("/api/admin/users/"+expert).header("Authorization","Bearer "+adminToken)).andExpect(status().isOk()).andExpect(jsonPath("$.data.email").value("sprintexpert@example.test")).andExpect(jsonPath("$.data.passwordHash").doesNotExist());
  mvc.perform(delete("/api/admin/users/"+expert).header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
  mvc.perform(delete("/api/admin/users/"+expert).header("Authorization","Bearer "+adminToken)).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("DELETED"));
  assertTrue(accounts.existsById(expert));assertEquals("sprintexpert@example.test",accounts.findById(expert).orElseThrow().getEmail());
  mvc.perform(get("/api/users/me").header("Authorization","Bearer "+expertToken)).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/community/users/"+expert)).andExpect(status().isNotFound());
  mvc.perform(delete("/api/admin/users/"+expert).header("Authorization","Bearer "+adminToken)).andExpect(status().isOk());
 }
 @Test void softDeleteCannotRemoveLastActiveAdministrator()throws Exception{
  for(var account:accounts.findAll())if(account.getId()!=admin&&account.getId()!=member&&account.getId()!=expert)account.setStatus("INACTIVE");accounts.flush();
  mvc.perform(delete("/api/admin/users/"+admin).header("Authorization","Bearer "+adminToken)).andExpect(status().isConflict());
  assertEquals("ACTIVE",accounts.findById(admin).orElseThrow().getStatus());
 }
 @Test void adminCrudAndRoleBoundaries()throws Exception{
  String json="{\"name\":\"Sprint Đại học thử nghiệm\",\"type\":\"UNIVERSITY\"}";
  mvc.perform(post("/api/admin/institutions").contentType("application/json").content(json)).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/admin/institutions").header("Authorization","Bearer "+token).contentType("application/json").content(json)).andExpect(status().isForbidden());
  long school=create("/api/admin/institutions",json);
  mvc.perform(post("/api/admin/institutions").header("Authorization","Bearer "+adminToken).contentType("application/json").content(json)).andExpect(status().isConflict());
  long faculty=create("/api/admin/faculties","{\"institutionId\":"+school+",\"name\":\"Khoa Công nghệ thông tin\"}");
  mvc.perform(put("/api/admin/faculties/"+faculty).header("Authorization","Bearer "+adminToken).contentType("application/json").content("{\"institutionId\":"+school+",\"name\":\"Khoa Học máy\"}")).andExpect(status().isOk());
  mvc.perform(delete("/api/admin/institutions/"+school).header("Authorization","Bearer "+adminToken)).andExpect(status().isConflict());
  mvc.perform(put("/api/users/me/profile").header("Authorization","Bearer "+token).contentType("application/json").content("{\"institutionId\":"+school+",\"facultyId\":"+faculty+"}")).andExpect(status().isOk());
  mvc.perform(delete("/api/admin/faculties/"+faculty).header("Authorization","Bearer "+adminToken)).andExpect(status().isConflict());
  mvc.perform(put("/api/users/me/profile").header("Authorization","Bearer "+token).contentType("application/json").content("{}")).andExpect(status().isOk());
  mvc.perform(delete("/api/admin/faculties/"+faculty).header("Authorization","Bearer "+adminToken)).andExpect(status().isOk());
  mvc.perform(put("/api/admin/institutions/"+school).header("Authorization","Bearer "+adminToken).contentType("application/json").content("{\"name\":\"Sprint Công ty\",\"type\":\"COMPANY\"}")).andExpect(status().isOk());
  mvc.perform(delete("/api/admin/institutions/"+school).header("Authorization","Bearer "+adminToken)).andExpect(status().isOk());
 }
 private long create(String path,String json)throws Exception{var r=mvc.perform(post(path).header("Authorization","Bearer "+adminToken).contentType("application/json").content(json)).andExpect(status().isCreated()).andReturn();return ((Number)JsonPath.read(r.getResponse().getContentAsString(),"$.data.id")).longValue();}
 private void follow(long target,boolean value)throws Exception{mvc.perform(post("/api/community/users/"+target+"/follow").header("Authorization","Bearer "+token).contentType("application/json").content("{\"following\":"+value+"}")).andExpect(status().isOk());}
 private void reject(String value)throws Exception{mvc.perform(get("/api/users/me").header("Authorization","Bearer "+value)).andExpect(status().isUnauthorized());}
 private String login(String name)throws Exception{var r=mvc.perform(post("/api/auth/login").param("username",name).param("password",PASSWORD)).andExpect(status().isOk()).andReturn();return JsonPath.read(r.getResponse().getContentAsString(),"$.data.accessToken");}
 private void postFixture(String key,String visibility,String status){var p=new Post();p.setAuthor(accounts.getReferenceById(expert));p.setTitle("Sprint post "+key);p.setSlug("sprint-post-"+key);p.setContent("Nội dung tiếng Việt");p.setVisibility(visibility);p.setStatus(status);p.setPublishedAt(LocalDateTime.now(ZoneOffset.UTC));posts.saveAndFlush(p);}
}
