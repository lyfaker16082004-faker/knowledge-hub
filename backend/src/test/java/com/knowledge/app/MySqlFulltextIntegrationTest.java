package com.knowledge.app;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("dev")
@TestPropertySource(properties="spring.datasource.url=jdbc:mysql://localhost:3306/knowledge_hub_account_test?createDatabaseIfNotExist=true&serverTimezone=Asia/Bangkok&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci")
@EnabledIfSystemProperty(named="mysql.it",matches="true")
class MySqlFulltextIntegrationTest {
 @Autowired JdbcTemplate jdbc;@Autowired MockMvc mvc;
 @Test void realMatchAgainstFindsNoncontiguousTermsAfterCommit()throws Exception{
  String key="sprint2-fulltext-"+UUID.randomUUID();
  assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='documents' AND index_name='ft_documents_content' AND index_type='FULLTEXT'",Integer.class));
  long category=jdbc.queryForObject("SELECT id FROM categories WHERE active=true ORDER BY id LIMIT 1",Long.class);
  jdbc.update("INSERT INTO documents(created_at,updated_at,version,category_id,title,slug,summary,description,access_type,price,currency,level,language,status,featured,published_at,cover_style) VALUES(UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0,?,?,?,'Neural lecture','Semantic study material','FREE',0,'VND','BEGINNER','vi','PUBLISHED',false,UTC_TIMESTAMP(6),'violet')",category,"Practical ranking methods",key);
  try{long id=jdbc.queryForObject("SELECT id FROM documents WHERE slug=?",Long.class,key);assertTrue(jdbc.queryForObject("SELECT MATCH(title,summary,description) AGAINST ('neural ranking' IN NATURAL LANGUAGE MODE) FROM documents WHERE id=?",Double.class,id)>0);mvc.perform(get("/api/documents").param("q","neural ranking").param("global","true")).andExpect(status().isOk()).andExpect(jsonPath("$.data.content[?(@.slug == '"+key+"')]").isNotEmpty());}
  finally{jdbc.update("DELETE FROM documents WHERE slug=? AND author_id IS NULL",key);}
 }
}
