package rf.mizuka.web.application.controllers.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rf.mizuka.web.application.dto.auth.LoginForm;
import rf.mizuka.web.application.dto.auth.RegisterForm;
import rf.mizuka.web.application.services.user.UserService;
import rf.mizuka.web.application.config.security.SecurityConfig;

/**
 * REST API контроллер JSON аутентификации для SPA/мобильных клиентов (не HTML).
 *
 * <p>Помечен {@link org.springframework.web.bind.annotation.RestController @RestController} и
 * {@link org.springframework.web.bind.annotation.RequestMapping @RequestMapping("/api/auth")} — возвращает JSON,
 * а не HTML views. Совместим с {@code .requestMatchers("/api/auth/**").permitAll()} в SecurityFilterChain.</p>
 *
 * <p><b>REST vs MVC:</b> {@link org.springframework.web.bind.annotation.RequestBody @RequestBody} → JSON парсинг в DTO,
 * {@link org.springframework.http.ResponseEntity ResponseEntity} → HTTP статус + JSON тело.
 * Отключен formLogin, CSRF игнорируется для {@code /api/**}.</p>
 *
 * <h3>Зависимости (аналогично AuthController)</h3>
 * <ul>
 *   <li>{@link UserService @Autowired} — регистрация в БД</li>
 *   <li>{@link org.springframework.security.authentication.AuthenticationManager @Autowired} — аутентификация</li>
 * </ul>
 *
 * <h3>JSON API контракт</h3>
 * <table border="1">
 *   <tr><th>Endpoint</th><th>Request</th><th>Success 200</th><th>Error 400</th></tr>
 *   <tr><td>POST /api/auth/register</td><td>{@code {"username":"user","password":"pass","confirmPassword":"pass"}}</td><td>{@code "User registered successfully."}</td><td>{@code "Passwords do not match."}</td></tr>
 *   <tr><td>POST /api/auth/login</td><td>{@code {"username":"user","password":"pass"}}</td><td>{@code "Login successful."}</td><td>{@code "Authentication failed: invalid username or password."}</td></tr>
 * </table>
 *
 * <h3>Регистрация (POST /api/auth/register)</h3>
 * <p>Клиентская валидация паролей + вызов {@link UserService#registerUser(String, String)} ()}:
 * <pre>
 * if (!form.getPassword().equals(form.getConfirmPassword()))
 *     return ResponseEntity.badRequest().body("Passwords do not match.");
 * ↓
 * userService.registerUser() → БД INSERT с BCrypt
 * ↓
 * ResponseEntity.ok("User registered successfully.")
 * </pre>
 * Нет catch — {@link UserService} исключения → Spring → HTTP 500 (TODO: @ExceptionHandler).
 * </p>
 *
 * <h3>Логин (POST /api/auth/login) — идентичен HTML контроллеру</h3>
 * <p>Ручная аутентификация через {@link AuthenticationManager}:
 * <pre>
 * 1. authenticationManager.authenticate(UsernamePasswordAuthenticationToken)
 *    ↓ DaoAuthenticationProvider → CustomUserDetailsService → UserRepository
 *    ↓ passwordEncoder.matches(rawPassword, BCrypt) ✓
 * 2. SecurityContextHolder.setContext() ← STATELESS сохранение аутентификации
 * 3. ResponseEntity.ok("Login successful.")
 * </pre>
 *
 * <p>{@link org.springframework.security.authentication.BadCredentialsException} и
 * {@link org.springframework.security.core.AuthenticationException} → HTTP 400 + JSON сообщение.</p>
 * </p>
 *
 * <p><b>STATELESS сессия:</b> После успешного /api/auth/login последующие API запросы увидят
 * аутентифицированного пользователя в SecurityContext (до истечения request scope).</p>
 *
 * <p><b>CSRF защита:</b> Отключена для {@code /api/**} через {@code .csrf().ignoringRequestMatchers("/api/**")}.
 * Для SPA добавить JWT токен в response вместо SecurityContext.</p>
 *
 * @see org.springframework.security.config.http.SessionCreationPolicy#STATELESS
 */
@RestController
@RequestMapping("/api/auth")
public class AuthRestController {
    /**
     * Сервис регистрации, идентичен HTML контроллеру — для POST /api/auth/register JSON.
     *
     * <p>{@link org.springframework.beans.factory.annotation.Autowired @Autowired} подставляет
     * {@link UserService} бин. Вызывается после проверки
     * паролей для создания пользователя в БД:
     * <pre>
     * userService.registerUser(form.getUsername(), form.getPassword())
     * ↓ @Transactional
     *   UserRepository.existsByUsername() → unique check
     *   passwordEncoder.encode(rawPassword) → BCrypt hash
     *   UserRepository.save() → INSERT users(username, password_hash)
     * </pre>
     *
     * <p>REST специфично: нет model attributes, исключения → HTTP 500 (нужен @ExceptionHandler).</p>
     */
    @Autowired
    private UserService userService;
    /**
     * Spring Security AuthenticationManager для JSON логина — идентичен HTML версии.
     *
     * <p>Инжектирует {@link SecurityConfig#authenticationManager(DaoAuthenticationProvider)} ()} бин ({@link org.springframework.security.authentication.ProviderManager} с
     * {@link org.springframework.security.authentication.dao.DaoAuthenticationProvider}).</p>
     *
     * <p><b>REST аутентификация поток:</b>
     * <pre>
     * POST /api/auth/login → @RequestBody LoginForm
     * ↓
     * authenticationManager.authenticate(UsernamePasswordAuthenticationToken)
     *   ↓ CustomUserDetailsService.loadUserByUsername()
     *   ↓ UserRepository.findByUsername()
     *   ↓ User → UserDetails
     *   ↓ passwordEncoder.matches(rawPassword, BCrypt hash)
     * ↓
     * SecurityContextHolder.setContext(authentication) ← STATELESS
     * ↓
     * ResponseEntity.ok("Login successful.")
     * </pre>
     * </p>
     *
     * <p>Ошибки → {@link ResponseEntity#badRequest() HTTP 400} + JSON сообщение.</p>
     */
    @Autowired
    private AuthenticationManager authenticationManager;
    /**
     * POST /api/auth/register — JSON API регистрация пользователя с валидацией паролей.
     *
     * <p>{@link org.springframework.web.bind.annotation.PostMapping @PostMapping} +
     * {@link org.springframework.web.bind.annotation.RequestBody @RequestBody RegisterForm form}:
     * <ul>
     *   <li>Jackson парсит JSON: {@code {"username":"user","password":"pass123","confirmPassword":"pass123"}}</li>
     *   <li>Заполняет DTO {@link RegisterForm} поля автоматически</li>
     *   <li>Content-Type: application/json</li>
     * </ul>
     * </p>
     *
     * <h3>Валидация + создание пользователя</h3>
     * <ol>
     *   <li><b>Проверка паролей:</b>
     *     <pre>
     *     if (!form.getPassword().equals(form.getConfirmPassword()))
     *         return ResponseEntity.badRequest().body("Passwords do not match.");
     *     </pre>
     *     HTTP 400 + JSON сообщение для клиента.
     *   </li>
     *   <li><b>Регистрация:</b>
     *     <pre>
     *     userService.registerUser(form.getUsername(), form.getPassword())
     *     ↓ @Transactional
     *       UserRepository.existsByUsername() → false
     *       User user = new User();
     *       user.setUsername(username);
     *       user.setPassword(passwordEncoder.encode(rawPassword)); ← BCrypt!
     *       UserRepository.save(user) → INSERT БД
     *     </pre>
     *   </li>
     * </ol>
     *
     * <h3>HTTP Response контракт</h3>
     * <table border="1">
     *   <tr><th>Статус</th><th>Сценарий</th><th>Response</th></tr>
     *   <tr><td>200 OK</td><td>Успех</td><td>{@code "User registered successfully."}</td></tr>
     *   <tr><td>400 Bad Request</td><td>Пароли не совпадают</td><td>{@code "Passwords do not match."}</td></tr>
     *   <tr><td>500 Internal Server Error</td><td>UserService исключение</td><td>(TODO: @ExceptionHandler)</td></tr>
     * </table>
     *
     * <p><b>После успеха:</b> Пользователь готов к {@code POST /api/auth/login}. Никакой автоматической аутентификации.</p>
     *
     * <p><b>CSRF:</b> Игнорируется ({@code .csrf().ignoringRequestMatchers("/api/**")}).</p>
     *
     * @param form JSON → RegisterForm (username, password, confirmPassword)
     * @return ResponseEntity&lt;String&gt; — статус + JSON сообщение
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterForm form) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            return ResponseEntity.badRequest()
                    .body("Passwords do not match.");
        }

        userService.registerUser(form.getUsername(), form.getPassword());

        return ResponseEntity.ok("User registered successfully.");
    }
    /**
     * POST /api/auth/login — JSON API аутентификация через AuthenticationManager.
     *
     * <p>{@link org.springframework.web.bind.annotation.PostMapping @PostMapping} + 
     * {@link org.springframework.web.bind.annotation.RequestBody @RequestBody LoginForm form}:
     * <ul>
     *   <li>Jackson парсит: {@code {"username":"user","password":"pass123"}}</li>
     *   <li>Content-Type: application/json</li>
     *   <li>SPA/mobile клиенты отправляют JSON, а не form-data</li>
     * </ul>
     * </p>
     *
     * <h3>Полный Spring Security поток аутентификации</h3>
     * <pre>
     * 1. UsernamePasswordAuthenticationToken(username, rawPassword) — unauthenticated
     * 2. authenticationManager.authenticate(token):
     *    ↓ ProviderManager → DaoAuthenticationProvider
     *    ↓ CustomUserDetailsService.loadUserByUsername(username)
     *    ↓ UserRepository.findByUsername() → User (UserDetails)
     *    ↓ passwordEncoder.matches(rawPassword, user.getPassword()) → true/false
     * 3. Успех → UsernamePasswordAuthenticationToken(authenticated, [ROLE_USER])
     * </pre>
     *
     * <h3>STATELESS SecurityContext (критично)</h3>
     * <p>Ручная установка для {@link SecurityConfig#securityFilterChain(HttpSecurity)} () SessionCreationPolicy.STATELESS}:
     * <pre>
     * SecurityContextHolder.createEmptyContext() → новый контекст
     * ↓ context.setAuthentication(authentication) ← сохраняет пользователя
     * ↓ SecurityContextHolder.setContext(context) ← для фильтров текущего request
     * </pre>
     * После этого API запросы увидят аутентифицированного пользователя.
     * </p>
     *
     * <h3>HTTP Response API контракт</h3>
     * <table border="1">
     *   <tr><th>Статус</th><th>Сценарий</th><th>Response Body</th></tr>
     *   <tr><td>200 OK</td><td>Успех</td><td>{@code "Login successful."}</td></tr>
     *   <tr><td>400 Bad Request</td><td>{@link BadCredentialsException}</td><td>{@code "Authentication failed: invalid username or password."}</td></tr>
     *   <tr><td>400 Bad Request</td><td>{@link AuthenticationException}</td><td>{@code "Authentication failed: " + message}</td></tr>
     * </table>
     *
     * <p><b>После успеха:</b> Клиент аутентифицирован. Для SPA добавить JWT токен в response.
     * CSRF игнорируется ({@code /api/**}).</p>
     *
     * @param form JSON → LoginForm (username, password)
     * @return ResponseEntity&lt;String&gt; — статус + сообщение
     */
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginForm form) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(form.getUsername(), form.getPassword())
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            return ResponseEntity.ok("Login successful.");
        } catch (BadCredentialsException e) {
            return ResponseEntity.badRequest()
                    .body("Authentication failed: invalid username or password.");
        } catch (AuthenticationException e) {
            return ResponseEntity.badRequest()
                    .body("Authentication failed: " + e.getMessage());
        }
    }
}