package com.knowledge.app;
import com.knowledge.app.model.entity.*;
import com.knowledge.app.repository.*;
import com.knowledge.app.model.dto.request.user.*;
import com.knowledge.app.service.user.*;
import java.math.BigDecimal;
import java.time.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class OrganizationIntegrationTest {
 @Autowired MockMvc mvc;@Autowired RoleRepository roles;@Autowired UserService users;@Autowired ProfileService profiles;
 @Autowired InstitutionRepository institutions;@Autowired FacultyRepository faculties;@Autowired CategoryRepository categories;
 @Autowired DocumentRepository documents;@Autowired DocumentFileRepository files;@Autowired DocumentDownloadRepository downloads;
 @Autowired UserRepository userRepository;@Autowired UserProfileRepository userProfiles;@Autowired JdbcTemplate jdbc;
 private Institution schoolA,schoolB,other;private Faculty facultyA,facultyA2,facultyB;private Category category;
 private Long memberId;private Document docA,docA2,docB,globalDoc;
 private static final String PASSWORD="Organization-test-password!";
 @BeforeEach void setup(){
  for(String name:Set.of("ADMIN","USER","MODERATOR"))if(roles.findByName(name).isEmpty()){var r=new Role();r.setName(name);roles.saveAndFlush(r);}
  schoolA=institution("IT Institution A","UNIVERSITY");schoolB=institution("IT Institution B","UNIVERSITY");other=institution("IT Institution Other","OTHER");
  facultyA=faculty(schoolA,"IT Faculty A");facultyA2=faculty(schoolA,"IT Faculty A2");facultyB=faculty(schoolB,"IT Faculty B");
  category=new Category();category.setName("IT Organization topic");category.setSlug("it-organization-topic");category.setActive(true);category.setSortOrder(0);categories.saveAndFlush(category);
  memberId=users.create(new AdminCreateUserRequest("orgmember","orgmember@example.test",PASSWORD,Set.of("USER"))).id();
  profiles.update(memberId,new UpdateProfileRequest(null,null,schoolA.getId(),facultyA.getId()));
  Long second=users.create(new AdminCreateUserRequest("orgother","orgother@example.test",PASSWORD,Set.of("USER"))).id();profiles.update(second,new UpdateProfileRequest(null,null,other.getId(),null));
  docA=document("a",schoolA,facultyA);docA2=document("a2",schoolA,facultyA2);docB=document("b",schoolB,facultyB);globalDoc=document("global",null,null);
 }
 @Test void publicDirectoryAndOtherHasNoFacultyOptions()throws Exception{
  mvc.perform(get("/api/institutions")).andExpect(status().isOk());
  mvc.perform(get("/api/institutions/"+schoolA.getId()+"/faculties")).andExpect(jsonPath("$.data.length()").value(2));
  mvc.perform(get("/api/institutions/"+other.getId()+"/faculties")).andExpect(jsonPath("$.data.length()").value(0));
 }
 @Test void registrationStoresOrganizationAndRejectsMismatchedFacultyAtomically()throws Exception{
  String valid="{\"username\":\"orgregistered\",\"email\":\"orgregistered@example.test\",\"password\":\""+PASSWORD+"\",\"institutionId\":"+schoolA.getId()+",\"facultyId\":"+facultyA.getId()+"}";
  mvc.perform(post("/api/auth/register").contentType("application/json").content(valid)).andExpect(status().isCreated()).andExpect(jsonPath("$.data.organization.institutionId").value(schoolA.getId().intValue()));
  String invalid=valid.replace("orgregistered","orginvalid").replace("\"facultyId\":"+facultyA.getId(),"\"facultyId\":"+facultyB.getId());
  mvc.perform(post("/api/auth/register").contentType("application/json").content(invalid)).andExpect(status().isBadRequest());assertFalse(userRepository.existsByUsernameIgnoreCase("orginvalid"));
 }
 @Test void profileUpdatesValidatePairAndOtherClearsFaculty()throws Exception{
  var session=login("orgmember");
  mvc.perform(put("/api/users/me/profile").header("Authorization","Bearer "+session).contentType("application/json").content("{\"institutionId\":"+schoolA.getId()+",\"facultyId\":"+facultyB.getId()+"}")).andExpect(status().isBadRequest());
  mvc.perform(put("/api/users/me/profile").header("Authorization","Bearer "+session).contentType("application/json").content("{\"institutionId\":"+other.getId()+",\"facultyId\":"+facultyA.getId()+"}")).andExpect(status().isBadRequest());
  mvc.perform(put("/api/users/me/profile").header("Authorization","Bearer "+session).contentType("application/json").content("{\"institutionId\":"+other.getId()+",\"facultyId\":null}")).andExpect(status().isOk()).andExpect(jsonPath("$.data.organization.institutionType").value("OTHER")).andExpect(jsonPath("$.data.organization.facultyId").isEmpty());
 }
 @Test void catalogueDefaultsToProfileSchoolAndCanSwitchToGlobalOrFaculty()throws Exception{
  var session=login("orgmember");
  mvc.perform(get("/api/documents").header("Authorization","Bearer "+session).param("q","ORG TEST")).andExpect(jsonPath("$.data.totalElements").value(2));
  mvc.perform(get("/api/documents").header("Authorization","Bearer "+session).param("q","ORG TEST").param("facultyId",facultyA.getId().toString())).andExpect(jsonPath("$.data.totalElements").value(1));
  mvc.perform(get("/api/documents").header("Authorization","Bearer "+session).param("q","ORG TEST").param("global","true")).andExpect(jsonPath("$.data.totalElements").value(4));
  mvc.perform(get("/api/documents").param("q","ORG TEST").param("institutionId",schoolB.getId().toString())).andExpect(jsonPath("$.data.totalElements").value(1));
  mvc.perform(get("/api/documents").param("q","ORG TEST").param("institutionId",schoolA.getId().toString()).param("facultyId",facultyB.getId().toString())).andExpect(status().isBadRequest());
  mvc.perform(get("/api/documents").header("Authorization","Bearer "+login("orgother")).param("q","ORG TEST")).andExpect(jsonPath("$.data.totalElements").value(4));
 }
 @Test void popularityUsesOnlyLastSevenDaysWithinScope()throws Exception{
  addDownload(docA,false);addDownload(docA,false);addDownload(docA2,false);for(int i=0;i<4;i++)addDownload(docA2,true);addDownload(docB,false);addDownload(docB,false);addDownload(docB,false);
  mvc.perform(get("/api/documents").header("Authorization","Bearer "+login("orgmember")).param("q","ORG TEST").param("sort","popular")).andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].id").value(docA.getId().intValue()));
  mvc.perform(get("/api/documents").param("q","ORG TEST").param("institutionId",schoolA.getId().toString()).param("facultyId",facultyA2.getId().toString()).param("sort","popular")).andExpect(jsonPath("$.data.totalElements").value(1)).andExpect(jsonPath("$.data.content[0].id").value(docA2.getId().intValue()));
 }
 @Test void uploadStoresSchoolAndReturnsRealPrivateFile()throws Exception{
  var session=login("orgmember");byte[] bytes="# Đề thi Java\nTiếng Việt 📚".getBytes(StandardCharsets.UTF_8);
  var result=mvc.perform(multipart("/api/documents").file(new MockMultipartFile("metadata","metadata","application/json",metadata(schoolA.getId(),facultyA.getId()).getBytes(StandardCharsets.UTF_8))).file(new MockMultipartFile("file","de-thi-java.txt","text/plain",bytes)).header("Authorization","Bearer "+session)).andExpect(status().isCreated()).andExpect(jsonPath("$.data.institutionId").value(schoolA.getId().intValue())).andExpect(jsonPath("$.data.facultyId").value(facultyA.getId().intValue())).andExpect(jsonPath("$.data.fileFormat").value("TXT")).andReturn();
  long id=((Number)JsonPath.read(result.getResponse().getContentAsString(),"$.data.id")).longValue();var file=files.findByDocumentIdAndActiveTrueOrderByIdAsc(id).get(0);
  mvc.perform(post("/api/documents/"+id+"/files/"+file.getId()+"/download").header("Authorization","Bearer "+session)).andExpect(status().isOk()).andExpect(content().bytes(bytes));
  mvc.perform(get("/api/documents/"+id)).andExpect(jsonPath("$.data.files[0].storageKey").doesNotExist());
 }
 @Test void uploadRejectsWrongSchoolUnsafeFileAndAnonymous()throws Exception{
  var session=login("orgmember");
  mvc.perform(multipart("/api/documents").file(new MockMultipartFile("metadata","metadata","application/json",metadata(schoolA.getId(),facultyB.getId()).getBytes(StandardCharsets.UTF_8))).file(new MockMultipartFile("file","notes.txt","text/plain","hello".getBytes())).header("Authorization","Bearer "+session)).andExpect(status().isBadRequest());
  mvc.perform(multipart("/api/documents").file(new MockMultipartFile("metadata","metadata","application/json",metadata(null,null).getBytes())).file(new MockMultipartFile("file","evil.exe","application/octet-stream",new byte[]{1,2,3})).header("Authorization","Bearer "+session)).andExpect(status().isBadRequest());
  mvc.perform(multipart("/api/documents").file(new MockMultipartFile("metadata","metadata","application/json",metadata(null,null).getBytes())).file(new MockMultipartFile("file","fake.pdf","application/pdf","not a PDF".getBytes())).header("Authorization","Bearer "+session)).andExpect(status().isBadRequest());
  mvc.perform(multipart("/api/documents")).andExpect(status().isUnauthorized());
 }
 private String metadata(Long institution,Long faculty){return "{\"title\":\"ORG TEST Upload\",\"summary\":\"Tài liệu thử nghiệm\",\"description\":\"Nội dung\",\"categoryId\":"+category.getId()+",\"level\":\"BEGINNER\",\"accessType\":\"FREE\",\"price\":0,\"institutionId\":"+institution+",\"facultyId\":"+faculty+"}";}
 private Institution institution(String name,String type){var i=new Institution();i.setName(name);i.setType(type);return institutions.saveAndFlush(i);}
 private Faculty faculty(Institution institution,String name){var f=new Faculty();f.setInstitution(institution);f.setName(name);return faculties.saveAndFlush(f);}
 private Document document(String key,Institution institution,Faculty faculty){var d=new Document();d.setCategory(category);d.setTitle("ORG TEST "+key);d.setSlug("org-test-"+key);d.setSummary("Tài liệu tổ chức");d.setDescription("Nội dung");d.setAccessType("FREE");d.setPrice(BigDecimal.ZERO);d.setCurrency("VND");d.setLevel("BEGINNER");d.setLanguage("vi");d.setStatus("PUBLISHED");d.setFeatured(false);d.setPublishedAt(LocalDateTime.now(ZoneOffset.UTC));d.setCoverStyle("violet");d.setInstitution(institution);d.setFaculty(faculty);documents.saveAndFlush(d);var f=new DocumentFile();f.setDocument(d);f.setOriginalName("org-scope-"+key+".md");f.setStorageKey(f.getOriginalName());f.setContentType("text/markdown; charset=UTF-8");f.setSizeBytes(30L);f.setActive(true);files.saveAndFlush(f);return d;}
 private void addDownload(Document doc,boolean old){var d=new DocumentDownload();d.setDocument(doc);d.setFile(files.findByDocumentIdAndActiveTrueOrderByIdAsc(doc.getId()).get(0));d.setUser(userRepository.getReferenceById(memberId));d.setDocumentTitle(doc.getTitle());d.setFileName("test.md");downloads.saveAndFlush(d);if(old)jdbc.update("UPDATE document_downloads SET created_at=? WHERE id=?",LocalDateTime.now(ZoneOffset.UTC).minusDays(8),d.getId());}
 private String login(String name) throws Exception {var result=mvc.perform(post("/api/auth/login").param("username",name).param("password",PASSWORD)).andExpect(status().isOk()).andReturn();return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(),"$.data.accessToken");}
}