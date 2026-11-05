package com.knowledge.app;
import com.knowledge.app.model.entity.*;
import com.knowledge.app.repository.*;
import com.knowledge.app.model.dto.request.user.AdminCreateUserRequest;
import com.knowledge.app.service.user.UserService;
import java.math.BigDecimal;
import java.time.*;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class DocumentLibraryIntegrationTest {
 @Autowired MockMvc mvc;@Autowired RoleRepository roles;@Autowired UserService users;
 @Autowired CategoryRepository categories;@Autowired DocumentRepository documents;
 @Autowired DocumentFileRepository files;@Autowired DocumentDownloadRepository downloads;
 @Autowired DocumentAccessRepository access;@Autowired DocumentBookmarkRepository bookmarks;
 private Document free,paid,draft;private DocumentFile freeFile,paidFile;private Long memberId,otherId;private Category category;
 private static final String PASSWORD="Library-test-password-2026!";
 @BeforeEach void setup(){
  for(String name:Set.of("ADMIN","USER","MODERATOR"))if(roles.findByName(name).isEmpty()){Role role=new Role();role.setName(name);roles.saveAndFlush(role);}
  memberId=users.create(new AdminCreateUserRequest("librarymember","librarymember@example.test",PASSWORD,Set.of("USER"))).id();
  otherId=users.create(new AdminCreateUserRequest("libraryother","libraryother@example.test",PASSWORD,Set.of("USER"))).id();
  category=new Category();category.setName("IT library category");category.setSlug("it-library-category");category.setSortOrder(0);category.setActive(true);categories.saveAndFlush(category);
  free=document("free","FREE","PUBLISHED");paid=document("paid","PAID","PUBLISHED");draft=document("draft","FREE","DRAFT");
  freeFile=file(free,"fixture-free.md");paidFile=file(paid,"fixture-paid.md");file(draft,"fixture-draft.md");
 }
 @Test void publicCatalogueFiltersAndHidesDrafts() throws Exception {
  mvc.perform(get("/api/documents").param("q","IT library")).andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(2));
  mvc.perform(get("/api/documents").param("q","IT library").param("access","FREE")).andExpect(jsonPath("$.data.totalElements").value(1));
  mvc.perform(get("/api/documents/"+free.getId())).andExpect(status().isOk()).andExpect(jsonPath("$.data.files[0].storageKey").doesNotExist()).andExpect(jsonPath("$.data.canDownload").value(false));
  mvc.perform(get("/api/documents/"+draft.getId())).andExpect(status().isNotFound());
  mvc.perform(get("/api/documents").param("size","500")).andExpect(status().isBadRequest());
 }
 @Test void downloadRequiresJwtAndCorrectFile() throws Exception {
  mvc.perform(post(url(free,freeFile))).andExpect(status().isUnauthorized());
  var session=login("librarymember");
  mvc.perform(post(url(free,paidFile)).header("Authorization","Bearer "+session)).andExpect(status().isNotFound());
  assertEquals(0,downloads.countByUserId(memberId));
 }
 @Test void freeDownloadsReturnRealUtf8BytesAndPrivateHistory() throws Exception {
  var session=login("librarymember");
  var result=mvc.perform(post(url(free,freeFile)).header("Authorization","Bearer "+session)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andReturn();
  assertTrue(result.getResponse().getHeader("Content-Disposition").startsWith("attachment;"));
  assertTrue(new String(result.getResponse().getContentAsByteArray(),java.nio.charset.StandardCharsets.UTF_8).contains("Tiếng Việt 📚"));
  mvc.perform(post(url(free,freeFile)).header("Authorization","Bearer "+session)).andExpect(status().isOk());
  mvc.perform(get("/api/users/me/downloads").header("Authorization","Bearer "+session).param("userId",String.valueOf(otherId))).andExpect(jsonPath("$.data.totalElements").value(2)).andExpect(jsonPath("$.data.content[0].title").value("IT library free"));
  mvc.perform(get("/api/users/me/library/stats").header("Authorization","Bearer "+session)).andExpect(jsonPath("$.data.downloadedDocuments").value(1)).andExpect(jsonPath("$.data.downloads").value(2));
  mvc.perform(get("/api/users/me/downloads").header("Authorization","Bearer "+login("libraryother"))).andExpect(jsonPath("$.data.totalElements").value(0));
 }
 @Test void paidFilesNeedAnUnexpiredNonRevokedGrant() throws Exception {
  var session=login("librarymember");mvc.perform(post(url(paid,paidFile)).header("Authorization","Bearer "+session)).andExpect(status().isForbidden());
  var grant=new DocumentAccess();grant.setDocument(paid);grant.setUser(newUserReference(memberId));grant.setGrantType("MANUAL");grant.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1));access.saveAndFlush(grant);
  mvc.perform(post(url(paid,paidFile)).header("Authorization","Bearer "+session)).andExpect(status().isForbidden());
  grant.setExpiresAt(null);access.flush();mvc.perform(post(url(paid,paidFile)).header("Authorization","Bearer "+session)).andExpect(status().isOk());
  grant.setRevokedAt(LocalDateTime.now(ZoneOffset.UTC));access.flush();mvc.perform(post(url(paid,paidFile)).header("Authorization","Bearer "+session)).andExpect(status().isForbidden());
 }
 @Test void onlyAdminCanGrantPaidAccess() throws Exception {
  String payload="{\"userId\":"+memberId+"}";
  mvc.perform(post("/api/admin/documents/"+paid.getId()+"/access").header("Authorization","Bearer "+login("librarymember")).contentType("application/json").content(payload)).andExpect(status().isForbidden());
  users.create(new AdminCreateUserRequest("libraryadmin","libraryadmin@example.test",PASSWORD,Set.of("ADMIN")));
  mvc.perform(post("/api/admin/documents/"+paid.getId()+"/access").header("Authorization","Bearer "+login("libraryadmin")).contentType("application/json").content(payload)).andExpect(status().isOk());
  mvc.perform(post(url(paid,paidFile)).header("Authorization","Bearer "+login("librarymember"))).andExpect(status().isOk());
 }
 @Test void bookmarksAreIdempotentAndBelongToCurrentUser() throws Exception {
  var session=login("librarymember");for(int i=0;i<2;i++)mvc.perform(post("/api/users/me/bookmarks/"+free.getId()).header("Authorization","Bearer "+session).contentType("application/json").content("{\"saved\":true}")).andExpect(status().isOk());
  mvc.perform(get("/api/users/me/bookmarks").header("Authorization","Bearer "+session)).andExpect(jsonPath("$.data.totalElements").value(1));
  mvc.perform(get("/api/users/me/bookmarks").header("Authorization","Bearer "+login("libraryother"))).andExpect(jsonPath("$.data.totalElements").value(0));
  mvc.perform(post("/api/users/me/bookmarks/"+free.getId()).header("Authorization","Bearer "+session).contentType("application/json").content("{\"saved\":false}")).andExpect(status().isOk());
  assertFalse(bookmarks.existsByUserIdAndDocumentId(memberId,free.getId()));
 }
 @Test void unavailableFilesNeverCreateDownloadHistory() throws Exception {
  freeFile.setStorageKey("missing-test-resource.md");files.flush();var session=login("librarymember");mvc.perform(post(url(free,freeFile)).header("Authorization","Bearer "+session)).andExpect(status().isNotFound());
  freeFile.setStorageKey("../application.yml");files.flush();mvc.perform(post(url(free,freeFile)).header("Authorization","Bearer "+session)).andExpect(status().isNotFound());
  assertEquals(0,downloads.countByUserId(memberId));
 }
 @Autowired UserRepository userRepository;
 private User newUserReference(Long id){return userRepository.getReferenceById(id);}
 private Document document(String key,String type,String status){var d=new Document();d.setCategory(category);d.setTitle("IT library "+key);d.setSlug("it-library-"+key);d.setSummary("Tài liệu thử nghiệm");d.setDescription("Nội dung tiếng Việt");d.setAccessType(type);d.setPrice(new BigDecimal("PAID".equals(type)?"49000":"0"));d.setCurrency("VND");d.setLevel("BEGINNER");d.setLanguage("vi");d.setStatus(status);d.setFeatured(false);d.setPublishedAt(LocalDateTime.now(ZoneOffset.UTC));d.setCoverStyle("violet");return documents.saveAndFlush(d);}
 private DocumentFile file(Document doc,String key){var f=new DocumentFile();f.setDocument(doc);f.setOriginalName(key);f.setStorageKey(key);f.setContentType("text/markdown; charset=UTF-8");f.setSizeBytes(34L);f.setActive(true);return files.saveAndFlush(f);}
 private String url(Document d,DocumentFile f){return "/api/documents/"+d.getId()+"/files/"+f.getId()+"/download";}
 private String login(String name) throws Exception {var result=mvc.perform(post("/api/auth/login").param("username",name).param("password",PASSWORD)).andExpect(status().isOk()).andReturn();return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(),"$.data.accessToken");}
}
