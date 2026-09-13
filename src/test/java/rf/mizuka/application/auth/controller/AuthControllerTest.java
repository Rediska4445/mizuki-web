package rf.mizuka.application.auth.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationListener;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import rf.mizuka.web.application.configurations.brokers.KafkaConfig;
import rf.mizuka.web.application.controllers.auth.AuthController;
import rf.mizuka.web.application.services.user.UserService;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
public class AuthControllerTest
{
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private CacheManager cacheManager;

    @MockitoBean
    private KafkaConfig kafkaConfig;

    @MockitoBean(name = "kafkaTemplate")
    private org.springframework.context.ApplicationListener<?> fakeKafkaTemplate;

    @BeforeEach
    void setUp()
    {
        CacheManager realCacheManager = new ConcurrentMapCacheManager();
        Mockito.when(cacheManager.getCache(Mockito.anyString()))
                .thenAnswer(invocation -> realCacheManager.getCache(invocation.getArgument(0)));
    }

    @Test
    void testLoginPageReturnsCorrectViewAndModel()
            throws Exception
    {
        mockMvc.perform(get("/auth/login")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/login"))
                .andExpect(model().attributeExists("loginForm"));
    }

    @Test
    void testRegisterPageReturnsCorrectViewAndModel()
            throws Exception
    {
        mockMvc.perform(get("/auth/register")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/register"))
                .andExpect(model().attributeExists("registerForm"));
    }

    @Test
    void testLoginSuccess() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken("user", "pass", new ArrayList<>());
        when(authenticationManager.authenticate(any())).thenReturn(auth);

        mockMvc.perform(post("/auth/login")
                .param("username", "user")
                .param("password", "pass")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void testLoginIncorrect() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Invalid username or password."));

        mockMvc.perform(post("/auth/login")
                        .param("username", "wrong_name")
                        .param("password", "wrong_pass")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/login"))
                .andExpect(model().attributeExists("loginError"));
    }

    @Test
    void testRegisterSuccess() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .param("username", "newuser")
                        .param("password", "password123")
                        .param("confirmPassword", "password123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));

        verify(userService).registerUser("newuser", "password123");
    }

    @Test
    void testRegisterPasswordsDoNotMatch() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .param("username", "user")
                        .param("password", "pass1")
                        .param("confirmPassword", "pass2")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/register"))
                .andExpect(model().attribute("registerError", "Passwords do not match."));
    }

    @Test
    void testRegisterUserExists()
            throws Exception
    {
        doThrow(new IllegalArgumentException("User already exists"))
                .when(userService).registerUser(anyString(), anyString());

        mockMvc.perform(post("/auth/register")
                        .param("username", "existing_user")
                        .param("password", "password")
                        .param("confirmPassword", "password")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/register"))
                .andExpect(model().attributeExists("registerError"));
    }
}