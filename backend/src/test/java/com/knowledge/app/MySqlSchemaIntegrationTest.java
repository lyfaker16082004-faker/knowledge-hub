package com.knowledge.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import com.knowledge.app.model.entity.Category;
import com.knowledge.app.repository.CategoryRepository;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@EnabledIfSystemProperty(named = "mysql.it", matches = "true")
@Transactional
class MySqlSchemaIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired CategoryRepository categories;

    @Test void migrationsAndSeedsArePresent() {
        assertEquals(24, jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE' AND table_name <> 'flyway_schema_history'", Integer.class));
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM roles", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name IN ('permissions', 'role_permissions')", Integer.class));
    }
    @Test void optionalFeatureTablesAreAbsent() {
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name IN ('reports','moderation_actions','audit_logs','refresh_tokens','password_reset_tokens','email_verification_tokens','notifications','notification_preferences','collections','collection_posts','post_revisions')", Integer.class));
    }
    @Test void jpaPersistsAuditFieldsAndOptimisticVersion() {
        Category category = new Category();
        category.setName("Integration test"); category.setSlug("integration-test-" + java.util.UUID.randomUUID());
        category.setSortOrder(0); category.setActive(true);
        categories.saveAndFlush(category);
        assertNotNull(category.getId()); assertNotNull(category.getCreatedAt()); assertNotNull(category.getVersion());
    }
    @Test void duplicateRolesAreRejected() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("INSERT INTO roles(name,created_at,updated_at,version) VALUES ('USER',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0)"));
    }
    @Test void orphanRoleGrantsAreRejected() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("INSERT INTO user_roles(user_id,role_id,created_at,updated_at,version) VALUES (-1,-1,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0)"));
    }
    @Test void invalidPostStatusIsRejected() {
        long author = user("invalid-status");
        assertEquals(3819, assertThrows(UncategorizedSQLException.class, () -> jdbc.update("INSERT INTO posts(author_id,title,slug,content,status,visibility,created_at,updated_at,version) VALUES (?, 'Test', 'invalid-status', 'Content', 'INVALID', 'PUBLIC', UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0)", author)).getSQLException().getErrorCode());
    }
    @Test void replyCannotReferenceAnotherPost() {
        long author = user("comment-parent");
        long first = post(author, "post-one"); long second = post(author, "post-two");
        jdbc.update("INSERT INTO comments(post_id,author_id,content,status,created_at,updated_at,version) VALUES (?,?,'Parent','VISIBLE',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0)", first, author);
        Long parent = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("INSERT INTO comments(post_id,author_id,parent_id,content,status,created_at,updated_at,version) VALUES (?,?,?,'Reply','VISIBLE',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0)", second, author, parent));
    }
    @Test void allTextColumnsAndConnectionUseUtf8mb4() {
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name <> 'flyway_schema_history' AND character_set_name IS NOT NULL AND character_set_name <> 'utf8mb4'", Integer.class));
        assertEquals("utf8mb4", jdbc.queryForObject("SELECT @@character_set_connection", String.class));
        assertEquals("utf8mb4_unicode_ci", jdbc.queryForObject("SELECT default_collation_name FROM information_schema.schemata WHERE schema_name=DATABASE()", String.class));
    }
    @Test void vietnameseAndEmojiRoundTripThroughJpaAndMysql() {
        Category category = new Category();
        category.setName("Kiến thức tiếng Việt – Đặng Thị Hồng 🌸");
        category.setDescription("Cộng đồng chia sẻ: ă â đ ê ô ơ ư, Ắ Ằ Ặ Ế Ỗ Ở Ự 📚");
        category.setSlug("tieng-viet-" + java.util.UUID.randomUUID());
        category.setSortOrder(0); category.setActive(true);
        categories.saveAndFlush(category);
        assertEquals(category.getName(), jdbc.queryForObject("SELECT name FROM categories WHERE id=?", String.class, category.getId()));
        assertEquals(category.getDescription(), jdbc.queryForObject("SELECT description FROM categories WHERE id=?", String.class, category.getId()));
        long author=user("unicode");
        String content="Hướng dẫn lập trình: Tiếng Việt có dấu đầy đủ 🚀";
        jdbc.update("INSERT INTO posts(author_id,title,slug,summary,content,status,visibility,created_at,updated_at,version) VALUES (?,?,?,?,?,'DRAFT','PUBLIC',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0)",author,"Cơ sở dữ liệu tiếng Việt",java.util.UUID.randomUUID().toString(),content,content);
        Long post=jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        assertEquals(content, jdbc.queryForObject("SELECT content FROM posts WHERE id=?",String.class,post));
        jdbc.update("INSERT INTO comments(post_id,author_id,content,status,created_at,updated_at,version) VALUES (?, ?, ?, 'VISIBLE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0)",post,author,content);
        assertEquals(content,jdbc.queryForObject("SELECT content FROM comments WHERE id=LAST_INSERT_ID()",String.class));
    }
    private long user(String prefix) {
        String key = prefix + java.util.UUID.randomUUID().toString().substring(0,8);
        jdbc.update("INSERT INTO users(username,email,password_hash,status,email_verified,created_at,updated_at,version) VALUES (?,?,?,'ACTIVE',false,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0)", key, key+"@example.test", "test-only-placeholder-not-a-login-credential");
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
    private long post(long author, String prefix) {
        jdbc.update("INSERT INTO posts(author_id,title,slug,content,status,visibility,created_at,updated_at,version) VALUES (?,'Test',?,'Content','DRAFT','PUBLIC',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0)", author, prefix+java.util.UUID.randomUUID());
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
