package com.pashumandi;

import com.pashumandi.entity.User;
import com.pashumandi.enums.UserRole;
import com.pashumandi.enums.UserStatus;
import com.pashumandi.repository.UserRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class EnvironmentProfileAndSchemaValidationTest {

    @Autowired(required = false)
    private Flyway flyway;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.pashumandi.security.JwtService jwtService;

    @Test
    @DisplayName("Dev profile boots successfully with Flyway enabled and Hibernate schema validation passing")
    void testDevProfileLoadsWithFlywayAndValidatesHibernateSchema() {
        assertNotNull(flyway, "Flyway bean must be configured and active");
        assertNotNull(flyway.info().current(), "Flyway migration state must not be null");
        assertTrue(flyway.info().current().getVersion().compareTo(
                org.flywaydb.core.api.MigrationVersion.fromVersion("2")) >= 0,
                "Schema must be migrated to at least version 2");
    }

    @Test
    @DisplayName("Existing user records in MySQL remain intact and accessible after migration")
    void testExistingUserDataIntactAndQueryable() {
        Optional<User> existingUserOpt = userRepository.findByPhone("9876543210");
        assertTrue(existingUserOpt.isPresent(), "Existing user record must be preserved");

        User user = existingUserOpt.get();
        assertEquals("Anil", user.getFirstName());
        assertTrue("Gupta".equals(user.getLastName()) || "Gupta Ji".equals(user.getLastName()), "Expected last name to match existing data");
        assertEquals(UserRole.BUYER, user.getRole());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    @DisplayName("Existing user can authenticate with JWT and retrieve profile from MySQL")
    void testExistingUserJwtAuthenticationAndProfileFetch() throws Exception {
        User user = userRepository.findByPhone("9876543210").orElseThrow();
        String accessToken = jwtService.generateAccessToken(user);

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.phone", is("9876543210")))
                .andExpect(jsonPath("$.data.firstName", is("Anil")));
    }

    @Test
    @DisplayName("Application fails startup when PROD profile is activated without PROD_DB_* environment variables")
    void testProdProfileFailsStartupWhenCredentialsMissing() {
        Exception exception = assertThrows(Exception.class, () -> {
            new SpringApplicationBuilder(PashuMandiApplication.class)
                    .profiles("prod")
                    .properties("server.port=0", "jwt.secret=dGVzdF9zZWNyZXRfa2V5X2Zvcl9wYXNodW1hbmRpX2F1dGhlbnRpY2F0aW9uX3N5c3RlbV8yNTZiaXRzIQ==")
                    .run();
        });
        assertNotNull(exception);
    }

    @Test
    @DisplayName("Application fails startup when UAT profile is activated without UAT_DB_* environment variables")
    void testUatProfileFailsStartupWhenCredentialsMissing() {
        Exception exception = assertThrows(Exception.class, () -> {
            new SpringApplicationBuilder(PashuMandiApplication.class)
                    .profiles("uat")
                    .properties("server.port=0", "jwt.secret=dGVzdF9zZWNyZXRfa2V5X2Zvcl9wYXNodW1hbmRpX2F1dGhlbnRpY2F0aW9uX3N5c3RlbV8yNTZiaXRzIQ==")
                    .run();
        });
        assertNotNull(exception);
    }

    @Test
    @DisplayName("Application fails initialization when JWT signing secret is missing or blank")
    void testStartupFailsWhenJwtSecretMissing() {
        com.pashumandi.config.JwtConfig config = new com.pashumandi.config.JwtConfig();
        config.setSecret(null);
        com.pashumandi.security.JwtService serviceWithNull = new com.pashumandi.security.JwtService(config);
        assertThrows(IllegalStateException.class, serviceWithNull::init);

        config.setSecret("   ");
        com.pashumandi.security.JwtService serviceWithBlank = new com.pashumandi.security.JwtService(config);
        assertThrows(IllegalStateException.class, serviceWithBlank::init);
    }
}
