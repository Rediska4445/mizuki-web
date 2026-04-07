package rf.mizuka.application.auth.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import rf.mizuka.web.application.auth.controllers.AuthController;
import rf.mizuka.web.application.auth.service.UserService;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit-тесты для {@link rf.mizuka.web.application.auth.controllers.AuthController}.
 *
 * <p>Класс использует аннотацию {@link WebMvcTest} для изолированного тестирования веб-слоя аутентификационного контроллера:
 * <ul>
 *   <li>Загружает только {@link AuthController} и связанные компоненты Spring MVC</li>
 *   <li>Мокирует зависимости через {@link MockitoBean}: {@link UserService} и {@link AuthenticationManager}</li>
 *   <li>Не запускает реальную базу данных, сервисы или security-контекст</li>
 *   <li>Предоставляет {@link MockMvc} для имитации HTTP GET-запросов к страницам авторизации</li>
 *   <li>Обеспечивает высокую скорость выполнения (менее 1 секунды на тест)</li>
 * </ul>
 *
 * <p><b><u>Основные (может содержать другие)</u> тестируемые сценарии:</b></p>
 * <ul>
 *   <li>GET /auth/login → возвращает view "auth/login" с атрибутом "loginForm"</li>
 *   <li>GET /auth/register → возвращает view "auth/register" с атрибутом "registerForm"</li>
 * </ul>
 *
 * @see WebMvcTest
 * @see MockMvc
 * @see MockitoBean
 */
@WebMvcTest(AuthController.class)
public class AuthControllerTest {
    /**
     * {@code MockMvc} instance для выполнения HTTP-запросов к контроллерам.
     *
     * <p>Объект имитирует полный стек Spring MVC без запуска реального HTTP-сервера:
     * <ul>
     *   <li>Вызов {@link org.springframework.web.servlet.DispatcherServlet} и обработка
     *       полной цепочки: {@link org.springframework.test.web.servlet.request.MockMvcRequestBuilders}, {@link org.springframework.test.web.servlet.result.MockMvcResultMatchers}</li>
     *   <li>Маршрутизация к {@link AuthController} через {@link org.springframework.web.servlet.HandlerMapping}</li>
     *   <li>Построение {@link org.springframework.ui.Model} и рендеринг view-шаблонов (thymeleaf, jsp и т.п.)</li>
     *   <li>Проверка статуса, заголовков, view-name и модели через {@link org.springframework.test.web.servlet.result.MockMvcResultMatchers}</li>
     * </ul>
     * {@link WebMvcTest} автоматически создает и внедряет этот бин в контекст теста.
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * Mockito-_mock для бина {@link UserService} в контексте приложения.
     *
     * <p>Аннотация {@link MockitoBean} создает и заменяет оригинальный бин:
     * <ul>
     *   <li>Создает {@link org.mockito.Mockito} мок для {@link UserService}</li>
     *   <li>Считается в качестве Spring-бина, доступного через {@link Autowired}</li>
     *   <li>Все вызовы методов сервиса (например, загрузка профиля пользователя при входе)
     *       направляются в мок, а не в реальную реализацию</li>
     *   <li>Позволяет задать ожидаемое поведение через {@link org.mockito.Mockito#when(Object)}
     *       и проверить взаимодействия через {@link org.mockito.Mockito#verify(Object)}}</li>
     * </ul>
     * В тестах страницы аутентификации это позволяет изолировать контроллер от реального доступа к БД.
     */
    @MockitoBean
    private UserService userService;

    /**
     * Mockito-_mock для бина {@link AuthenticationManager} в контексте приложения.
     *
     * <p>Аннотация {@link MockitoBean} мокирует менеджер аутентификации Spring Security:
     * <ul>
     *   <li>Позволяет контролировать результаты проверки логина/пароля без реального вызова
     *       провайдеров аутентификации (LDAP, JDBC, UserDetails и т.д.)</li>
     *   <li>Делает методы {@link org.springframework.security.authentication.AuthenticationManager#authenticate(Authentication)}}
     *       возвращать заранее подготовленные {@link org.springframework.security.core.Authentication}</li>
     *   <li>Это дает возможность тестировать, как контроллер реагирует на успешную аутентификацию
     *       и на ошибки (BadCredentialsException и т.п.)</li>
     * </ul>
     * Вместе с Mockito это позволяет целиком изолировать логику контроллера страницы
     * аутентификации от реального security-стека.
     */
    @MockitoBean
    private AuthenticationManager authenticationManager;

    /**
     * Тест страницы входа в систему.
     *
     * <p>Проверяет базовый сценарий отображения страницы аутентификации:
     * <ul>
     *   <li>HTTP GET-запрос на путь {@code /auth/login}</li>
     *   <li>Ожидаемый статус ответа: 200 OK (страница успешно возвращена)</li>
     *   <li>Ожидаемое имя view: {@code "auth/login"} (совпадает с возвращаемым значением контроллера)</li>
     *   <li>В модели присутствует атрибут {@code "loginForm"} (объект формы для входа в систему)</li>
     * </ul>
     * Тест проверяет, что контроллер корректно обслуживает GET-запросы и
     * передает в шаблон корректную форму для входа.
     */
    @Test
    void testLoginPageReturnsCorrectViewAndModel() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attributeExists("loginForm"));
    }

    /**
     * Тест страницы регистрации.
     *
     * <p>Проверяет базовый сценарий отображения страницы регистрации:
     * <ul>
     *   <li>HTTP GET-запрос на путь {@code /auth/register}</li>
     *   <li>Ожидаемый статус ответа: 200 OK (страница успешно возвращена)</li>
     *   <li>Ожидаемое имя view: {@code "auth/register"} (совпадает с возвращаемым значением контроллера)</li>
     *   <li>В модели присутствует атрибут {@code "registerForm"} (объект формы для регистрации)</li>
     * </ul>
     * Тест проверяет, что контроллер корректно обслуживает GET-запросы регистрации и
     * передает в шаблон корректную форму для регистрации.
     */
    @Test
    void testRegisterPageReturnsCorrectViewAndModel() throws Exception {
        mockMvc.perform(get("/auth/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("registerForm"));
    }

    /**
     * Тест успешной аутентификации при логине.
     *
     * <p>Проверяет сценарий корректного входа пользователя:
     * <ul>
     *   <li>Настройка мока {@link AuthenticationManager}: успешный результат {@link Authentication}</li>
     *   <li>POST-запрос на {@code /auth/login} с корректными параметрами username/password и CSRF</li>
     *   <li>Ожидаемый статус: 3xx (редирект) и перенаправление на главную ({@code /})</li>
     * </ul>
     * Тест проверяет, что контроллер корректно обрабатывает успешную аутентификацию
     * и переводит пользователя на главную страницу.
     */
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

    /**
     * Тест неудачной аутентификации при логине.
     *
     * <p>Проверяет сценарий неверного логина/пароля:
     * <ul>
     *   <li>Мок {@link AuthenticationManager} выбрасывает
     *       {@link org.springframework.security.authentication.BadCredentialsException}</li>
     *   <li>POST-запрос на {@code /auth/login} с неверными учетными данными и CSRF</li>
     *   <li>Ожидаемый результат: 200 OK, возврат той же страницы {@code auth/login}</li>
     *   <li>В модели присутствует атрибут {@code "loginError"} с сообщением об ошибке</li>
     * </ul>
     * Тест проверяет, что контроллер корректно обрабатывает ошибки входа
     * и отображает сообщение об ошибке на странице логина.
     */
    @Test
    void testLoginIncorrect() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Invalid username or password."));

        mockMvc.perform(post("/auth/login")
                        .param("username", "wrong_name")
                        .param("password", "wrong_pass")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attributeExists("loginError"))
                .andExpect(model().attribute("loginError", "Invalid username or password."));
    }

    /**
     * Тест успешной регистрации пользователя.
     *
     * <p>Проверяет сценарий корректной регистрации:
     * <ul>
     *   <li>POST-запрос на {@code /auth/register} с совпадающими паролями и CSRF</li>
     *   <li>Ожидаемый статус: 3xx (редирект) и перенаправление на {@code /auth/login}</li>
     *   <li>Убедиться, что реальный вызов сервиса {@link UserService#registerUser(String, String)}
     *       произошел с правильными параметрами</li>
     * </ul>
     * Тест проверяет, что контроллер корректно обслуживает регистрацию,
     * передает данные в {@link UserService} и редиректит на страницу входа.
     */
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

    /**
     * Тест ошибки при регистрации при несовпадении паролей.
     *
     * <p>Проверяет сценарий, когда введенные пароли не совпадают:
     * <ul>
     *   <li>POST-запрос на {@code /auth/register} с разными значениями password/confirmPassword</li>
     *   <li>Ожидаемый результат: 200 OK, возврат страницы {@code auth/register}</li>
     *   <li>В модели присутствует атрибут {@code "registerError"} с сообщением "Passwords do not match."</li>
     * </ul>
     * Тест проверяет, что контроллер корректно проверяет совпадение паролей
     * и отображает связанное сообщение об ошибке.
     */
    @Test
    void testRegisterPasswordsDoNotMatch() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .param("username", "user")
                        .param("password", "pass1")
                        .param("confirmPassword", "pass2")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attribute("registerError", "Passwords do not match."));
    }

    /**
     * Тест ошибки при регистрации существующего пользователя.
     *
     * <p>Проверяет сценарий, когда имя пользователя уже занято:
     * <ul>
     *   <li>Настройка мока {@link UserService}: выбрасывает {@code IllegalArgumentException}
     *       "User already exists" при вызове {@link UserService#registerUser(String, String)}</li>
     *   <li>POST-запрос на {@code /auth/register} с корректными, но уже существующими учетными данными</li>
     *   <li>Ожидаемый результат: 200 OK, возврат страницы {@code auth/register}</li>
     *   <li>В модели присутствует атрибут {@code "registerError"} с сообщением "User already exists"</li>
     * </ul>
     * Тест проверяет, что контроллер корректно обрабатывает ошибку существующего пользователя
     * и отображает соответствующее сообщение.
     */
    @Test
    void testRegisterUserExists() throws Exception {
        doThrow(new IllegalArgumentException("User already exists"))
                .when(userService).registerUser(anyString(), anyString());

        mockMvc.perform(post("/auth/register")
                        .param("username", "existing_user")
                        .param("password", "password")
                        .param("confirmPassword", "password")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attribute("registerError", "User already exists"));
    }
}
