package com.knowledge.app;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import static org.junit.jupiter.api.Assertions.assertThrows;
@ActiveProfiles(profiles="dev",inheritProfiles=false)
@TestPropertySource(properties="spring.datasource.url=jdbc:mysql://localhost:3306/knowledge_hub_account_test?createDatabaseIfNotExist=true&serverTimezone=Asia/Bangkok&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci")
@EnabledIfSystemProperty(named="mysql.it",matches="true")
class MySqlOrganizationIntegrationTest extends OrganizationIntegrationTest {
 @Test void databaseRejectsCrossInstitutionFacultyWithoutService(){
  Long school=jdbc.queryForObject("SELECT id FROM institutions WHERE name='IT Institution A'",Long.class);
  Long faculty=jdbc.queryForObject("SELECT f.id FROM faculties f JOIN institutions i ON i.id=f.institution_id WHERE i.name='IT Institution B'",Long.class);
  assertThrows(DataIntegrityViolationException.class,()->jdbc.update("UPDATE user_profiles SET institution_id=?,faculty_id=? WHERE user_id=(SELECT id FROM users WHERE username='orgmember')",school,faculty));
 }
 @Test void documentAndPostFacultyPairsAreAlsoProtectedByDatabase(){
  Long school=jdbc.queryForObject("SELECT id FROM institutions WHERE name='IT Institution A'",Long.class);
  Long faculty=jdbc.queryForObject("SELECT f.id FROM faculties f JOIN institutions i ON i.id=f.institution_id WHERE i.name='IT Institution B'",Long.class);
  assertThrows(DataIntegrityViolationException.class,()->jdbc.update("UPDATE documents SET institution_id=?,faculty_id=? WHERE slug='org-test-a'",school,faculty));
  Long author=jdbc.queryForObject("SELECT id FROM users WHERE username='orgmember'",Long.class);
  assertThrows(DataIntegrityViolationException.class,()->jdbc.update("INSERT INTO posts(author_id,title,slug,content,status,visibility,institution_id,faculty_id,created_at,updated_at,version) VALUES (?,'Org post','org-fk-test','Content','DRAFT','PUBLIC',?,?,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0)",author,school,faculty));
 }
}