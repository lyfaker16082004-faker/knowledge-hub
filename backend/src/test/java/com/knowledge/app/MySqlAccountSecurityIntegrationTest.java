package com.knowledge.app;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles(profiles="dev",inheritProfiles=false)
@TestPropertySource(properties="spring.datasource.url=jdbc:mysql://localhost:3306/knowledge_hub_account_test?createDatabaseIfNotExist=true&serverTimezone=Asia/Bangkok&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci")
@EnabledIfSystemProperty(named="mysql.it",matches="true")
class MySqlAccountSecurityIntegrationTest extends AccountSecurityIntegrationTest {}