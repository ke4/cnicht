package co.uk.karel.dev.cnicht.auth.controller;

import co.uk.karel.dev.cnicht.TestcontainersConfiguration;
import co.uk.karel.dev.cnicht.auth.entity.UserAccount;
import co.uk.karel.dev.cnicht.auth.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TEST_EMAIL = "test@cnicht.com";
    private static final String TEST_PASSWORD = "secret123";

    @BeforeEach
    void setUp() {
        userAccountRepository.findByEmail(TEST_EMAIL)
                .ifPresent(userAccountRepository::delete);
        userAccountRepository.save(
                new UserAccount(TEST_EMAIL, passwordEncoder.encode(TEST_PASSWORD)));
    }

    @Test
    void accessingProtectedEndpoint_WithoutSession_Returns401() throws Exception {
        mockMvc.perform(get("/api/auth/status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithValidCredentials_Returns200AndSetsSessionCookie() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", TEST_EMAIL)
                        .param("password", TEST_PASSWORD))
                .andExpect(status().isOk())
                .andReturn();

        // MockMvc tracks sessions via MockHttpSession rather than Set-Cookie headers;
        // assert that an authenticated session was created.
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        assertThat(session.isInvalid()).isFalse();
        assertThat(session.getId()).isNotBlank();
    }

    @Test
    void statusWithValidSession_ReturnsUserAndOk() throws Exception {
        // Login first to obtain an authenticated session
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", TEST_EMAIL)
                        .param("password", TEST_PASSWORD))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(get("/api/auth/status").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(TEST_EMAIL)));
    }

    @Test
    void loginWithInvalidCredentials_Returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", TEST_EMAIL)
                        .param("password", "wrongpassword"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutInvalidatesSession_AndReturns200() throws Exception {
        // First login to get a session
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", TEST_EMAIL)
                        .param("password", TEST_PASSWORD))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        // Logout using the same session
        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .session(session))
                .andExpect(status().isOk())
                // JSESSIONID cookie is destroyed (maxAge=0)
                .andExpect(cookie().maxAge("JSESSIONID", 0));

        // Session should now be invalidated on the server
        assertThat(session.isInvalid()).isTrue();

        // Using the invalidated session should return 401
        mockMvc.perform(get("/api/auth/status")
                        .session(session))
                .andExpect(status().isUnauthorized());
    }
}
