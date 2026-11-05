package com.knowledge.app;

import com.knowledge.app.model.entity.Role;
import com.knowledge.app.repository.*;
import com.knowledge.app.model.dto.request.user.AdminCreateUserRequest;
import com.knowledge.app.service.user.UserService;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class AccountSecurityIntegrationTest {
 @Autowired MockMvc mvc;
 @Autowired UserRepository users;
 @Autowired RoleRepository roles;
 @Autowired UserRoleRepository grants;
 @Autowired UserService service;
 @Autowired PasswordEncoder encoder;
 private static final String PASSWORD="Secure-test-password-2026!";
 @BeforeEach void seedRoles() { for(String name:Set.of("ADMIN","MODERATOR","USER")) if(roles.findByName(name).isEmpty()) { Role role=new Role(); role.setName(name); roles.saveAndFlush(role); } }
 @Test void registrationHashesPasswordAndCannotGrantAdmin() throws Exception {
  mvc.perform(post("/api/auth/register").contentType("application/json").content("{\"username\":\"newmember\",\"email\":\"new@example.test\",\"password\":\""+PASSWORD+"\",\"roles\":[\"ADMIN\"]}"))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.data.roles[0]").value("USER")).andExpect(jsonPath("$.data.passwordHash").doesNotExist());
  var user=users.findByUsernameIgnoreCase("newmember").orElseThrow();
  assertNotEquals(PASSWORD,user.getPasswordHash()); assertTrue(encoder.matches(PASSWORD,user.getPasswordHash()));
  assertEquals(1,grants.findWithRoles(user.getId()).size());
 }
 @Test void authenticationAndBearerAuthorizationAreEnforced() throws Exception {
  mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/auth/register").contentType("application/json").content("{}" )).andExpect(status().isBadRequest());
  account("member",Set.of("USER")); var session=login("member");
  mvc.perform(get("/api/users/me").header("Authorization","Bearer "+session)).andExpect(status().isOk()).andExpect(jsonPath("$.data.username").value("member"));
  mvc.perform(get("/api/admin/users").header("Authorization","Bearer "+session)).andExpect(status().isForbidden());
  mvc.perform(post("/api/users/me/password").header("Authorization","Bearer "+session).contentType("application/json").content("{}" )).andExpect(status().isBadRequest());
 }
 @Test void jwtLoginReturnsTokenAndLogoutRevokesIt() throws Exception {
  account("jwtmember",Set.of("USER"));
  var result=mvc.perform(post("/api/auth/login").param("username","jwtmember").param("password",PASSWORD)).andExpect(status().isOk()).andExpect(jsonPath("$.data.tokenType").value("Bearer")).andExpect(jsonPath("$.data.expiresIn").value(86400)).andReturn();
  assertNull(result.getRequest().getSession(false));String token=JsonPath.read(result.getResponse().getContentAsString(),"$.data.accessToken");assertEquals(3,token.split("[.]").length);
  mvc.perform(get("/api/users/me").header("Authorization","Bearer "+token)).andExpect(status().isOk());
  mvc.perform(post("/api/auth/logout").header("Authorization","Bearer "+token)).andExpect(status().isOk());
  mvc.perform(get("/api/users/me").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
 }
 @Test void adminCanCreateListAndLockUsersAndTokensAreRevoked() throws Exception {
  account("admin",Set.of("ADMIN")); var admin=login("admin");
  mvc.perform(post("/api/admin/users").header("Authorization","Bearer "+admin).contentType("application/json").content("{\"username\":\"managed\",\"email\":\"managed@example.test\",\"password\":\""+PASSWORD+"\",\"roles\":[\"USER\",\"MODERATOR\"]}" )).andExpect(status().isCreated());
  var member=login("managed"); Long id=users.findByUsernameIgnoreCase("managed").orElseThrow().getId();
  mvc.perform(get("/api/admin/users").header("Authorization","Bearer "+admin).param("q","managed")).andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1)).andExpect(jsonPath("$.data.content[0].passwordHash").doesNotExist());
  mvc.perform(put("/api/admin/users/"+id).header("Authorization","Bearer "+admin).contentType("application/json").content("{\"email\":\"managed@example.test\",\"status\":\"LOCKED\",\"roles\":[\"USER\"]}" )).andExpect(status().isOk());
  mvc.perform(get("/api/users/me").header("Authorization","Bearer "+member)).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/auth/login").param("username","managed").param("password",PASSWORD)).andExpect(status().isUnauthorized());
 }
 @Test void lastActiveAdminCannotBeDisabledOrDemoted() throws Exception {
  Long id=account("onlyadmin",Set.of("ADMIN")); var session=login("onlyadmin");
  mvc.perform(put("/api/admin/users/"+id).header("Authorization","Bearer "+session).contentType("application/json").content("{\"email\":\"onlyadmin@example.test\",\"status\":\"LOCKED\",\"roles\":[\"ADMIN\"]}" )).andExpect(status().isConflict());
  mvc.perform(put("/api/admin/users/"+id).header("Authorization","Bearer "+session).contentType("application/json").content("{\"email\":\"onlyadmin@example.test\",\"status\":\"ACTIVE\",\"roles\":[\"USER\"]}" )).andExpect(status().isConflict());
 }
 @Test void passwordChangeInvalidatesOtherTokensAndRequiresCurrentPassword() throws Exception {
  account("passwordmember",Set.of("USER")); var first=login("passwordmember"); var second=login("passwordmember");
  mvc.perform(post("/api/users/me/password").header("Authorization","Bearer "+first).contentType("application/json").content("{\"currentPassword\":\"wrong\",\"newPassword\":\"Another-secure-password!\"}" )).andExpect(status().isBadRequest());
  mvc.perform(post("/api/users/me/password").header("Authorization","Bearer "+first).contentType("application/json").content("{\"currentPassword\":\""+PASSWORD+"\",\"newPassword\":\"Another-secure-password!\"}" )).andExpect(status().isOk());
  mvc.perform(get("/api/users/me").header("Authorization","Bearer "+second)).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/auth/login").param("username","passwordmember").param("password",PASSWORD)).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/auth/login").param("username","passwordmember").param("password","Another-secure-password!")).andExpect(status().isOk());
 }
 @Test void duplicatesAndLongUnicodePasswordsAreRejected() throws Exception {
  account("duplicate",Set.of("USER"));
  mvc.perform(post("/api/auth/register").contentType("application/json").content("{\"username\":\"DUPLICATE\",\"email\":\"other@example.test\",\"password\":\""+PASSWORD+"\"}" )).andExpect(status().isConflict());
  mvc.perform(post("/api/auth/register").contentType("application/json").content("{\"username\":\"unicode\",\"email\":\"unicode@example.test\",\"password\":\""+"ắ".repeat(25)+"\"}" )).andExpect(status().isBadRequest());
 }
 @Test void moderatorCannotManageAccountsAndAdminCanResetPassword() throws Exception {
  account("moderator",Set.of("MODERATOR")); var moderator=login("moderator");
  mvc.perform(get("/api/admin/users").header("Authorization","Bearer "+moderator)).andExpect(status().isForbidden());
  account("resetadmin",Set.of("ADMIN")); var admin=login("resetadmin");
  Long target=account("resetmember",Set.of("USER")); var member=login("resetmember");
  mvc.perform(post("/api/admin/users/"+target+"/password").header("Authorization","Bearer "+admin).contentType("application/json").content("{\"password\":\"Reset-password-2026!\"}" )).andExpect(status().isOk());
  mvc.perform(get("/api/users/me").header("Authorization","Bearer "+member)).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/auth/login").param("username","resetmember").param("password","Reset-password-2026!")).andExpect(status().isOk());
  mvc.perform(get("/api/admin/users").header("Authorization","Bearer "+admin).param("size","1000")).andExpect(status().isBadRequest());
 }
 @Test void oversizedLoginCredentialsReturnUnauthorized() throws Exception {
  mvc.perform(post("/api/auth/login").param("username","x".repeat(51)).param("password",PASSWORD)).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/auth/login").param("username","anyone").param("password","ắ".repeat(25))).andExpect(status().isUnauthorized());
 }
 private Long account(String username,Set<String> roles) { return service.create(new AdminCreateUserRequest(username,username+"@example.test",PASSWORD,roles)).id(); }
 private String login(String username) throws Exception {var result=mvc.perform(post("/api/auth/login").param("username",username).param("password",PASSWORD)).andExpect(status().isOk()).andReturn();return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(),"$.data.accessToken");}
}
