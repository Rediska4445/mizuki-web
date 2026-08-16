package rf.mizuka.application.auth.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import rf.mizuka.web.application.database.repository.UserRepository;
import rf.mizuka.web.application.security.config.SecurityConfig;
import rf.mizuka.web.application.services.user.UserService;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
public class SecurityConfigTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean(name = "bCryptPasswordEncoder")
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @Test
    @DisplayName("1. Доступ к логину и регистрации разрешен всем")
    void shouldAllowAccessToAuthPages() throws Exception {
        mockMvc.perform(get("/auth/login")).andExpect(status().isOk());
        mockMvc.perform(get("/auth/register")).andExpect(status().isOk());
    }

    @Test
    void shouldAllowPostWithCsrf() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("username", "test")
                        .param("password", "123"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("3. CSRF защита пропускает запрос с токеном")
    void shouldAllowPostRequestWithCsrfOnWeb() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("username", "test")
                        .param("password", "123"))
                .andExpect(status().isOk());
    }
}
