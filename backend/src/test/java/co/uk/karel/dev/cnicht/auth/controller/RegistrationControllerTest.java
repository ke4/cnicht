package co.uk.karel.dev.cnicht.auth.controller;

import co.uk.karel.dev.cnicht.TestcontainersConfiguration;
import co.uk.karel.dev.cnicht.auth.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class RegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    private static final String NEW_EMAIL = "newuser@cnicht.com";
    private static final String NEW_PASSWORD = "password123";

    @BeforeEach
    void setUp() {
        userAccountRepository.findByEmail(NEW_EMAIL)
                .ifPresent(userAccountRepository::delete);
    }

    @Test
    void registeringValidUser_CreatesRecordAndReturns201() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", NEW_EMAIL)
                        .param("password", NEW_PASSWORD))
                .andExpect(status().isCreated());

        assertThat(userAccountRepository.existsByEmail(NEW_EMAIL)).isTrue();
    }

    @Test
    void registeringDuplicateEmail_Returns409() throws Exception {
        // First registration succeeds
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", NEW_EMAIL)
                        .param("password", NEW_PASSWORD))
                .andExpect(status().isCreated());

        // Second registration with the same email is rejected
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", NEW_EMAIL)
                        .param("password", NEW_PASSWORD))
                .andExpect(status().isConflict());
    }
}
