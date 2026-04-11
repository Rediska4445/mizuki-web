package rf.mizuka.application.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import rf.mizuka.web.application.services.rest.AuthApiService;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class RestApiControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthApiService authApiService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Доступ разрешен: возвращает 418 I'm a teapot при наличии валидного JWT")
    void shouldAccessTeapotWithMockedJwt() throws Exception {
        mockMvc.perform(get("/api/teapot")
                        .with(jwt()
                                .jwt(builder -> builder.subject("test-client"))
                        ))
                .andExpect(status().is(418))
                .andExpect(jsonPath("$.access").value("GRANTED_BUT_NO_CAFFEINE"));
    }

    @Test
    @DisplayName("Должен вернуть ошибку вместо редиректа")
    void shouldGetTheError() throws Exception {
        mockMvc.perform(get("/api/unknown_page_which_exactly_not_will_be_exist")
                        .with(jwt()
                                .jwt(builder -> builder.subject("test-client"))
                        ))
                .andExpect(status().is(404));
    }

    @Test
    @DisplayName("Доступ запрещен: редирект (3xx) при отсутствии токена")
    void shouldReturnUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/teapot"))
                .andExpect(status().is3xxRedirection());
    }
}
