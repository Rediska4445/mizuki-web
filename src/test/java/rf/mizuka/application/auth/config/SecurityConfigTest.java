package rf.mizuka.application.auth.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import rf.mizuka.web.application.security.config.SecurityConfig;
import rf.mizuka.web.application.database.user.repository.UserRepository;
import rf.mizuka.web.application.services.user.UserService;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Интеграционные тесты конфигурации безопасности {@link SecurityConfig}.
 *
 * <p>Класс использует {@link SpringBootTest} совместно с {@link WebMvcTest}-подобным контекстом:
 * <ul>
 *   <li>{@link SpringBootTest} запускает полный контекст Spring Boot с профилем {@code "test"}</li>
 *   <li>{@link ActiveProfiles} активирует тестовый профиль для настроек безопасности и базы данных</li>
 *   <li>{@link AutoConfigureMockMvc} предоставляет {@link MockMvc} для имитации HTTP‑запросов</li>
 *   <li>{@link Import} подключает {@link SecurityConfig}, чтобы тестировать реально применённую конфигурацию</li>
 * </ul>
 *
 * <p>В тесте мокируются зависимые компоненты через {@link MockitoBean}:
 * <ul>
 *   <li>{@link UserRepository} — для изоляции от реальной БД</li>
 *   <li>{@link PasswordEncoder} (BCrypt) — для тестов хеширования паролей</li>
 *   <li>{@link UserService} и {@link AuthenticationManager} — для изоляции логики аутентификации</li>
 * </ul>
 *
 * <p><b>Тестируемые сценарии безопасности:</b></p>
 * <ul>
 *   <li>Проверка, что публичные страницы {@code /auth/login} и {@code /auth/register}
 *       доступны без аутентификации</li>
 *   <li>Проверка корректной работы CSRF‑защиты: запросы {@code POST /auth/register}
 *       с токеном CSRF завершаются успешно</li>
 * </ul>
 *
 * @see SpringBootTest
 * @see ActiveProfiles
 * @see AutoConfigureMockMvc
 * @see Import
 * @see SecurityConfig
 * @see MockMvc
 * @see MockitoBean
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
public class SecurityConfigTest {
    /**
     * {@code MockMvc} instance для выполнения HTTP‑запросов в контексте безопасности.
     *
     * <p>Используется для проверки политики доступа и защиты Spring Security:
     * <ul>
     *   <li>Имитация GET‑запросов к страницам {@code /auth/login} и {@code /auth/register}</li>
     *   <li>Проверка, что эти страницы доступны без аутентификации ({@code 200 OK})</li>
     *   <li>Имитация POST‑запросов с CSRF‑токеном через {@link org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors#csrf()}</li>
     *   <li>Проверка, что CSRF‑защита корректно пропускает запросы с валидным токеном</li>
     * </ul>
     * Объект автоматически создается и настраивается {@link AutoConfigureMockMvc}.
     */
    @Autowired
    private MockMvc mockMvc;
    /**
     * Mockito‑mock для репозитория {@link UserRepository}.
     *
     * <p>Аннотация {@link MockitoBean} обеспечивает:
     * <ul>
     *   <li>Замену реального {@link UserRepository} на мок для тестов</li>
     *   <li>Доступность мока через контекст Spring и внедрение в {@link UserService} и {@link SecurityConfig}</li>
     *   <li>Возможность моделировать поиск пользователей, проверку существования логина и т.п.</li>
     * </ul>
     * Это позволяет тестировать конфигурацию безопасности, не обращаясь к реальной базе данных.
     */
    @MockitoBean
    private UserRepository userRepository;
    /**
     * Mockito‑mock для {@link PasswordEncoder} с именем бина {@code "bCryptPasswordEncoder"}.
     *
     * <p>Аннотация {@link MockitoBean} гарантирует:
     * <ul>
     *   <li>Замену именно BCrypt‑кодировщика, который используется в {@link SecurityConfig}</li>
     *   <li>Возможность задать мок‑поведение (например, имитировать хеширование пароля)</li>
     *   <li>Проверку корректной передачи паролей и их преобразования в хеши в контексте безопасности</li>
     * </ul>
     * Такой мок нужен для тестирования логики кодирования паролей без реального hash‑вычисления.
     */
    @MockitoBean(name = "bCryptPasswordEncoder")
    private PasswordEncoder passwordEncoder;
    /**
     * Mockito‑mock для {@link UserService} в контексте Spring Security.
     *
     * <p>Аннотация {@link MockitoBean} обеспечивает:
     * <ul>
     *   <li>Замену реального {@link UserService} на мок, который используется в {@link SecurityConfig}</li>
     *   <li>Проверку сценариев: регистрация пользователя, проверка логина и т.п., без реальной БД</li>
     *   <li>Изоляцию тестов безопасности от реализаций бизнес‑логики</li>
     * </ul>
     * Это позволяет тестировать поведение безопасности, а не реализацию сервиса.
     */
    @MockitoBean
    private UserService userService;
    /**
     * Mockito‑mock для {@link AuthenticationManager} в контексте Spring Security.
     *
     * <p>Аннотация {@link MockitoBean} обеспечивает:
     * <ul>
     *   <li>Замену реального менеджера аутентификации на мок, подключенный в {@link SecurityConfig}</li>
     *   <li>Проверку корректного взаимодействия контроллеров и фильтров безопасности с провайдером аутентификации</li>
     * </ul>
     * Это позволяет тестировать механизм безопасности, а не реальные провайдеры (LDAP, JDBC и т.д.).
     */
    @MockitoBean
    private AuthenticationManager authenticationManager;
    /**
     * Тест сценария открытого доступа к страницам аутентификации.
     *
     * <p>Проверяет, что:
     * <ul>
     *   <li>GET‑запрос на {@code /auth/login} возвращает {@code 200 OK}</li>
     *   <li>GET‑запрос на {@code /auth/register} возвращает {@code 200 OK}</li>
     * </ul>
     * Тест гарантирует, что {@link SecurityConfig} разрешает доступ к страницам входа и регистрации
     * для всех пользователей без аутентификации.
     */
    @Test
    @DisplayName("1. Доступ к логину и регистрации разрешен всем")
    void shouldAllowAccessToAuthPages() throws Exception {
        mockMvc.perform(get("/auth/login")).andExpect(status().isOk());
        mockMvc.perform(get("/auth/register")).andExpect(status().isOk());
    }
    /**
     * Тест возможности выполнить POST‑запрос с CSRF‑токеном.
     *
     * <p>Проверяет, что:
     * <ul>
     *   <li>POST‑запрос на {@code /auth/register} с CSRF‑токеном через
     *       {@link org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors#csrf()}</li>
     *   <li>При передаче корректных параметров {@code username} и {@code password}</li>
     *   <li>Ожидаемый результат: {@code 200 OK}</li>
     * </ul>
     * Тест проверяет, что Spring Security и CSRF‑фильтр корректно пропускают запрос с валидным токеном.
     */
    @Test
    void shouldAllowPostWithCsrf() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("username", "test")
                        .param("password", "123"))
                .andExpect(status().isOk());
    }
    /**
     * Тест корректной работы CSRF‑защиты при наличии токена.
     *
     * <p>Проверяет, что:
     * <ul>
     *   <li>POST‑запрос на {@code /auth/register} с валидным CSRF‑токеном</li>
     *   <li>При передаче корректных параметров логина и пароля</li>
     *   <li>Ожидаемый результат: {@code 200 OK}</li>
     * </ul>
     * Тест подтверждает, что CSRF‑защита в {@link SecurityConfig} корректно обрабатывает
     * веб‑запросы с токеном и не блокирует их.
     */
    @Test
    @DisplayName("3. CSRF защита пропускает запрос с токеном")
    void shouldAllowPostRequestWithCsrfOnWeb() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .with(csrf()) // Добавляем валидный токен
                        .param("username", "test")
                        .param("password", "123"))
                .andExpect(status().isOk());
    }
}
