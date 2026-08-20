package rf.mizuka.application.auth.config;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import rf.mizuka.web.application.database.repository.UserRepository;
import rf.mizuka.web.application.configurations.security.SecurityConfig;
import rf.mizuka.web.application.services.audio.AudioService;
import rf.mizuka.web.application.services.storage.StorageService;
import rf.mizuka.web.application.services.tracks.TrackService;
import rf.mizuka.web.application.services.user.UserService;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
@TestPropertySource(locations = "classpath:settings-test.properties")
public class SecurityConfigTest
{
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrackService trackService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private StorageService service;

    @MockitoBean
    private AudioService audioService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean(name = "bCryptPasswordEncoder")
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @BeforeEach
    void setUp()
    {
        Mockito.when(passwordEncoder.encode(Mockito.anyString()))
                .thenReturn("$2a$10$wXvPj3.xGz23A1234567890abcdefghijklmnopqrstuvw");
    }

    @Test
    @DisplayName("All users can join to auth pages")
    void shouldAllowAccessToAuthPages()
            throws Exception
    {
        mockMvc.perform(
                // Send req to login page
                get("/auth/login")
                // Should return 200 (1 rule)
        ).andExpect(status().isOk());
        mockMvc.perform(
                // Send req to login page
                get("/auth/register")
                // Should return 200 (1 rule)
        ).andExpect(status().isOk());
    }

    @Test
    @DisplayName("All users can register in auth pages")
    void shouldAllowPostRequestWithCsrfOnWeb()
            throws Exception
    {
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("username", "test")
                        .param("password", "123"))
                .andExpect(status().isOk());
    }

    @Nested
    @DisplayName("1. Basic security tests")
    class BaseSecurityDefaultsTests
    {
        @Test
        @DisplayName("Protect to Session Fixation")
        void shouldEnableSessionFixationProtection()
                throws Exception
        {
            mockMvc.perform(post("/auth/login")
                            .with(csrf())
                            .param("username", "user")
                            .param("password", "password"))
                    .andExpect(status().is3xxRedirection());
        }

        @Test
        @DisplayName("Safe headers")
        void shouldIncludeDefaultSecurityHeaders()
                throws Exception
        {
            mockMvc.perform(get("/auth/login"))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("X-Frame-Options", "DENY"))
                    .andExpect(header().exists("X-XSS-Protection"));
        }
    }

    @Nested
    @DisplayName("2. Rules defined in class")
    class DeveloperRulesTests
    {
        // Rule 1: Unauthorized users cannot access any service pages, except for the login page.
        @Test
        @DisplayName("Правило 1: Неавторизованные пользователи перенаправляются на страницу логина")
        void rule1_unauthorizedUsersRedirectedToLogin()
                throws Exception
        {
            mockMvc.perform(get("/track/all"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/auth/login"));

            mockMvc.perform(get("/some-random-private-page"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/auth/login"));
        }

        // Rule 2: Authorized users can access the following pages: home, tracks.
        @Test
        @WithMockUser
        @DisplayName("Правило 2: Авторизованные пользователи имеют доступ к трекам и домашней странице")
        void rule2_authorizedUsersCanAccessTracksAndHome()
                throws Exception
        {
            mockMvc.perform(get("/track/1"))
                    .andExpect(status().is(Matchers.in(
                            // Only not forbidden or authorized
                            List.of(200, 302, 404)
                    )));
            mockMvc.perform(get("/"))
                    .andExpect(status().is(Matchers.in(
                            // Only not forbidden or authorized
                            List.of(200, 302, 404)
                    )));
        }

        // Rule 3: All users have access to specific static assets (js, css, html, ico, img).
        @Test
        @DisplayName("Правило 3: Все пользователи (даже гости) имеют доступ к статическим файлам")
        void rule3_allUsersHaveAccessToStaticAssets()
                throws Exception
        {
            mockMvc.perform(get("/css")).andExpect(status().isOk());
            mockMvc.perform(get("/js")).andExpect(status().isOk());
            mockMvc.perform(get("/favicon.ico")).andExpect(status().isOk());
        }

        // Rule 4: All operations must be performed using CSRF token technology.
        @Test
        @DisplayName("Правило 4: Любые изменяющие состояние операции (POST) требуют валидный CSRF токен")
        void rule4_allStateChangingOperationsRequireCsrf()
                throws Exception
        {
            mockMvc.perform(post("/auth/register")
                            .param("username", "test")
                            .param("password", "123"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/auth/register")
                            .with(csrf())
                            .param("username", "test")
                            .param("password", "123"))
                    .andExpect(status().isOk());
        }

        // Rule 5: API access is prohibited for everyone.
        @Test
        @WithMockUser
        @DisplayName("Правило 5: Доступ к /api/** и /.well-known/** запрещен абсолютно всем")
        void rule5_apiAccessIsProhibitedForEveryone()
                throws Exception
        {
            mockMvc.perform(get("/api"))
                    .andExpect(status().is(Matchers.in(
                            java.util.List.of(401, 403, 404)
                    )));
            mockMvc.perform(get("/.well-known"))
                    .andExpect(status().is(Matchers.in(java.util.List.of(401, 403, 302))));
        }

        // Rule 6: Authorized users can terminate their session (logout).
        @Test
        @WithMockUser
        @DisplayName("Правило 6: Авторизованные пользователи могут завершить сессию (logout)")
        void rule6_authorizedUsersCanLogout()
                throws Exception
        {
            mockMvc.perform(logout("/auth/logout"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/auth/login"))
                    .andExpect(cookie().exists("JSESSIONID"));
        }
    }
}
