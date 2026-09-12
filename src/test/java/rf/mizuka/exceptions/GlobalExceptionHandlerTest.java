package rf.mizuka.exceptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import rf.mizuka.web.GlobalExceptionHandler;
import rf.mizuka.web.application.configurations.security.SecurityConfig;
import rf.mizuka.web.application.database.repository.user.UserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {GlobalExceptionHandlerTest.FakeController.class})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(SecurityConfig.class)
public class GlobalExceptionHandlerTest
{
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @MockitoBean
    private org.springframework.security.authentication.AuthenticationManager authenticationManager;
    @MockitoBean
    private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    @MockitoBean
    private CacheManager cacheManager;

    @BeforeEach
    void setUp()
    {
        CacheManager realCacheManager = new ConcurrentMapCacheManager();
        Mockito.when(cacheManager.getCache(Mockito.anyString()))
                .thenAnswer(invocation -> realCacheManager.getCache(invocation.getArgument(0)));

        this.mockMvc = MockMvcBuilders
                .standaloneSetup(new FakeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @RestController
    static class FakeController
    {
        @GetMapping("/auth/login/test-exception")
        public void triggerException()
        {
            throw new RuntimeException("Fake");
        }

        @GetMapping("/auth/login/test-4x-exception")
        public void trigger4XException()
        {
            throw new BadCredentialsException("Fake");
        }
    }

    /*
    * 1. Any undefined exceptions should return '500' error code and JSON answer for error subscription
    * */
    @Test
    @DisplayName("Обработчик исключений должен возвращать статус 500 и кастомный JSON при любой ошибке")
    void shouldHandleAnyExceptionAndReturnCustomJson()
            throws Exception
    {
        mockMvc.perform(get("/auth/login/test-exception"))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.error").value("The server was rude to me :("))
                .andExpect(jsonPath("$.message").value("ask the administrator to make the server apologize!"));
    }

    /*
     * 1. Any bad credentials exceptions should return '4xx' error code and JSON answer for error subscription
     * */
    @Test
    @DisplayName("Обработчик исключений должен возвращать статус 500 и кастомный JSON при любой ошибке")
    void shouldBadCredentialsHandleAnyExceptionAndReturnCustomJson()
            throws Exception
    {
        mockMvc.perform(get("/auth/login/test-4x-exception"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.error").value("Entity is unauthorized"));
    }
}