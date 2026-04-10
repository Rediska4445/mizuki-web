package rf.mizuka.application.rest;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;

import org.springframework.security.test.context.support.WithMockUser;
import rf.mizuka.web.application.services.rest.AuthApiService;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthApiServiceTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthApiService authApiService;

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    @WithMockUser
    void getToken_ShouldReturnToken_WhenCredentialsAreValid() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("clientId", "test-client");
        request.put("clientSecret", "test-secret");

        String fakeToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.fake.token";

        when(authApiService.login("test-client", "test-secret")).thenReturn(fakeToken);

        mockMvc.perform(post("/api/auth/token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value(fakeToken))
                .andExpect(jsonPath("$.token_type").value("Bearer"));
    }

    @Test
    void getToken_ShouldReturn401_WhenCredentialsAreInvalid() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("clientId", "wrong-client");
        request.put("clientSecret", "wrong-secret");

        when(authApiService.login("wrong-client", "wrong-secret"))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }
}
