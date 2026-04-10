package rf.mizuka.application.auth.controller.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import rf.mizuka.web.application.controllers.rest.AuthRestController;
import rf.mizuka.web.application.dto.auth.LoginForm;
import rf.mizuka.web.application.dto.auth.RegisterForm;
import rf.mizuka.web.application.services.user.UserService;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционные unit-тесты для REST‑контроллера аутентификации {@link AuthRestController}.
 *
 * <p>Класс использует аннотацию {@link WebMvcTest} для изолированного тестирования веб‑слоя:
 * <ul>
 *   <li>Загружает только REST‑контроллер {@link AuthRestController} и связанные компоненты Spring MVC</li>
 *   <li>Подтягивает в контекст {@link ObjectMapper} для сериализации/десериализации JSON</li>
 *   <li>Мокирует зависимости через {@link MockitoBean}: {@code UserService} и {@link AuthenticationManager}</li>
 *   <li>Не запускает реальную базу данных, полноценный security‑контекст и лишние бины</li>
 *   <li>Предоставляет {@link MockMvc} для имитации HTTP‑запросов к REST‑эндпоинтам</li>
 *   <li>Тестирует API-поведение контроллера, а не шаблонные страницы (HTML)</li>
 * </ul>
 *
 * <p><b><u>Основные (может содержать другие)</u> тестируемые сценарии:</b></p>
 * <ul>
 *   <li>{@code POST /api/auth/register} с корректной DTO {@link RegisterForm} → 200 OK и сообщение "User registered successfully."</li>
 *   <li>{@code POST /api/auth/register} с разными паролями в форме → 400 Bad Request и сообщение "Passwords do not match."</li>
 * </ul>
 *
 * @see WebMvcTest
 * @see MockMvc
 * @see MockitoBean
 * @see ObjectMapper
 */
@WebMvcTest(AuthRestController.class)
public class AuthRestControllerTest {
    /**
     * {@code MockMvc} instance для выполнения HTTP‑запросов к REST‑контроллеру.
     *
     * <p>Предоставляет эмуляцию полного стека Spring MVC без запуска реального HTTP‑сервера:
     * <ul>
     *   <li>Разбор {@link org.springframework.test.web.servlet.result.MockMvcResultMatchers} и маршрутизация к {@link AuthRestController}</li>
     *   <li>Выполнение {@link org.springframework.web.servlet.DispatcherServlet} и обработка POST‑запросов с JSON‑телом</li>
     *   <li>Проверка статуса, заголовков и текстового тела ответа через {@link org.springframework.test.web.servlet.result.MockMvcResultMatchers}</li>
     * </ul>
     * Объект автоматически создается {@link WebMvcTest} и внедряется в поля теста.
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * Jackson {@code ObjectMapper} для работы с JSON‑представлением DTO {@link RegisterForm}.
     *
     * <p>Используется для:
     * <ul>
     *   <li>Сериализации объекта {@link RegisterForm} в JSON‑строку через
     *       {@link tools.jackson.databind.ObjectMapper#writeValueAsString(Object)}</li>
     *   <li>Отправки JSON‑тела в {@code POST /api/auth/register} через {@link org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder#content(String)}</li>
     * </ul>
     * Автонастроенный {@link ObjectMapper} применяет те же правила конвертации,
     * что и реальный Spring Boot‑контекст (имя полей, аннотации Jackson и т.п.).
     */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Mockito‑mock для бина {@link UserService} в контексте REST‑контроллера.
     *
     * <p>Аннотация {@link MockitoBean} гарантирует:
     * <ul>
     *   <li>Замену реального {@link UserService} временной реализацией Mockito</li>
     *   <li>Доступность мока через {@link Autowired} внутри {@link AuthRestController}</li>
     *   <li>Возможность настройки поведения метода {@link UserService#registerUser(String, String)}
     *       (успешная регистрация или выброс исключения)</li>
     * </ul>
     * Это позволяет тестировать API‑контроллер независимо от реального доступа к базе данных.
     */
    @MockitoBean
    private UserService userService;

    /**
     * Mockito‑mock для {@link AuthenticationManager} в контексте REST‑контроллера.
     *
     * <p>Аннотация {@link MockitoBean} обеспечивает:
     * <ul>
     *   <li>Замену реального менеджера аутентификации на мок для теста</li>
     *   <li>Возможность моделировать успешную или неудачную аутентификацию через
     *       {@link org.mockito.Mockito#when(Object)}}</li>
     *   <li>Изоляцию логики {@link AuthRestController}
     *       от реальных провайдеров аутентификации (Ldap, JDBC и т.д.)</li>
     * </ul>
     * В данном тесте активно используется для проверки сценариев логина через REST‑API.
     */
    @MockitoBean
    private AuthenticationManager authenticationManager;
    /**
     * Тест успешной регистрации пользователя через REST API.
     *
     * <p>Проверяет сценарий корректной регистрации:
     * <ul>
     *   <li>Создается валидная DTO {@link RegisterForm}
     *       с совпадающими паролями</li>
     *   <li>POST‑запрос на {@code /api/auth/register} с телом в формате JSON и
     *       {@link MediaType#APPLICATION_JSON}</li>
     *   <li>Ожидаемый результат: {@code 200 OK} и текстовый ответ "User registered successfully."</li>
     * </ul>
     * Тест проверяет, что REST‑контроллер корректно принимает JSON‑форму,
     * обрабатывает запрос и возвращает сообщение об успешной регистрации.
     */
    @Test
    void testRegisterSuccess() throws Exception {
        RegisterForm form = new RegisterForm("newuser", "password123", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully."));
    }
    /**
     * Тест регистрации с несовпадающими паролями через REST API.
     *
     * <p>Проверяет сценарий ошибки на стороне сервера:
     * <ul>
     *   <li>Создается некорректная DTO {@link RegisterForm}
     *       с разными значениями {@code password} и {@code confirmPassword}</li>
     *   <li>POST‑запрос на {@code /api/auth/register} с JSON‑телом и типом контента {@code application/json}</li>
     *   <li>Ожидаемый результат: {@code 400 Bad Request} и текстовый ответ "Passwords do not match."</li>
     * </ul>
     * Тест проверяет, что REST‑контроллер корректно валидирует пароли
     * и возвращает клиенту ошибку с описанием причины.
     */
    @Test
    void testRegisterPasswordsDoNotMatch() throws Exception {
        RegisterForm form = new RegisterForm("user", "pass1", "pass2");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Passwords do not match."));
    }
    /**
     * Тест успешного логина через REST API.
     *
     * <p>Проверяет сценарий корректной аутентификации:
     * <ul>
     *   <li>Создается валидная DTO {@link LoginForm} с логином и паролем</li>
     *   <li>Мок {@link AuthenticationManager} возвращает {@link org.springframework.security.core.Authentication}
     *       с корректными учетными данными и ролью {@code ROLE_USER}</li>
     *   <li>POST‑запрос на {@code /api/auth/login} с телом в формате JSON</li>
     *   <li>Ожидаемый результат: {@code 200 OK} и текстовый ответ "Login successful."</li>
     * </ul>
     * Тест проверяет, что REST‑контроллер корректно принимает JSON‑форму логина,
     * делегирует аутентификацию моку и возвращает клиенту сообщение об успехе.
     */
    @Test
    void testLoginSuccess() throws Exception {
        LoginForm form = new LoginForm("user", "pass");

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user", "pass", Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

        when(authenticationManager.authenticate(any())).thenReturn(auth);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isOk())
                .andExpect(content().string("Login successful."));
    }
    /**
     * Тест логина с невалидными учетными данными через REST API.
     *
     * <p>Проверяет сценарий ошибочного входа:
     * <ul>
     *   <li>Создается DTO {@link LoginForm} с несуществующими/неправильными данными</li>
     *   <li>Мок {@link AuthenticationManager} выбрасывает {@link org.springframework.security.authentication.BadCredentialsException}
     *       с сообщением "Invalid username or password"</li>
     *   <li>POST‑запрос на {@code /api/auth/login} с JSON‑телом</li>
     *   <li>Ожидаемый результат: {@code 400 Bad Request} и тело ответа, содержащее текст "Authentication failed"</li>
     * </ul>
     * Тест проверяет, что REST‑контроллер корректно обрабатывает ошибки аутентификации
     * и возвращает клиенту понятное сообщение об ошибке.
     */
    @Test
    void testLoginInvalidCredentials() throws Exception {
        LoginForm form = new LoginForm("wrong_user", "wrong_pass");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Invalid username or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Authentication failed")));
    }
}