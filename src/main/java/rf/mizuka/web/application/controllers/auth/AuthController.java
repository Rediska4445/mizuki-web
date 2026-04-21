package rf.mizuka.web.application.controllers.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import rf.mizuka.web.application.services.user.CustomUserDetailsService;
import rf.mizuka.web.application.services.user.UserService;
import rf.mizuka.web.application.dto.auth.LoginForm;
import rf.mizuka.web.application.dto.auth.RegisterForm;
import rf.mizuka.web.application.security.config.SecurityConfig;
import rf.mizuka.web.application.models.user.User;

/**
 * MVC контроллер HTML-страниц аутентификации (login/register) для Spring Security.
 *
 * <p>Помечен {@link org.springframework.stereotype.Controller @Controller} и
 * {@link org.springframework.web.bind.annotation.RequestMapping @RequestMapping("/auth")} — обрабатывает
 * GET/POST запросы к {@code /auth/login}, {@code /auth/register}. Работает с Thymeleaf шаблонами
 * ({@code "auth/login"}, {@code "auth/register"}) и HTML формами.</p>
 *
 * <p><b>НЕ REST API:</b> Возвращает view names ("auth/login") → Thymeleaf → HTML, а не JSON.
 * Совместим с {@link org.springframework.security.config.http.SessionCreationPolicy#STATELESS STATELESS}
 * через ручную установку {@link org.springframework.security.core.context.SecurityContext}.</p>
 *
 * <h3>Зависимости Spring Security</h3>
 * <ul>
 *   <li>{@link org.springframework.security.authentication.AuthenticationManager @Autowired} — центральный
 *       компонент Spring Security для аутентификации. Вызывает цепочку
 *       {@link org.springframework.security.authentication.ProviderManager} → {@link org.springframework.security.authentication.dao.DaoAuthenticationProvider}</li>
 *   <li>{@link UserService @Autowired} — регистрация пользователей в БД</li>
 * </ul>
 *
 * <h3>Полный поток аутентификации (ручной, без formLogin):</h3>
 * <pre>
 * GET /auth/login → auth() → Thymeleaf "auth/login" + empty LoginForm
 * ↓
 * POST /auth/login → login() →
 *   ↓ authenticationManager.authenticate(UsernamePasswordAuthenticationToken)
 *     ↓ DaoAuthenticationProvider → CustomUserDetailsService → UserRepository
 *     ↓ passwordEncoder.matches(rawPassword, BCrypt) ✓
 *   ↓ Authentication успех
 *   ↓ SecurityContextHolder.setContext() ← РУЧНОЙ security context
 * ↓ redirect: /
 * </pre>
 *
 * <h3>Обработка ошибок Spring Security</h3>
 * <p>{@link org.springframework.security.authentication.BadCredentialsException} и
 * {@link org.springframework.security.core.AuthenticationException} перехватываются:
 * <ul>
 *   <li>{@code model.addAttribute("loginError", "Invalid username or password.")}</li>
 *   <li>Thymeleaf отображает ошибку + сохраняет заполненную форму</li>
 *   <li>{@code return "auth/login"} — возврат на форму с ошибкой</li>
 * </ul>
 * </p>
 *
 * <h3>Регистрация с валидацией</h3>
 * <p>Проверяет {@code password.equals(confirmPassword)} ДО вызова {@link UserService#registerUser(String, String)} ()}:
 * <ul>
 *   <li>Несовпадение → {@code "Passwords do not match."} на форму</li>
 *   * <li>{@link UserService} исключение → {@code "User already exists: username"} на форму</li>
 *   <li>Успех → {@code redirect:/auth/login} (рекомендация Spring Security: Post/Redirect/Get)</li>
 * </ul>
 * </p>
 *
 * <p><b>Ключевой момент ручной аутентификации:</b><br/>
 * {@link org.springframework.security.config.annotation.web.builders.HttpSecurity#formLogin(Customizer)} ().disable() formLogin отключен},
 * поэтому {@code POST /login} НЕ обрабатывается Spring Security автоматически. Контроллер делает это вручную:
 * <pre>
 * 1. authenticationManager.authenticate(token)
 * 2. SecurityContextHolder.setContext(context.setAuthentication(authentication))
 * 3. redirect:/ (пользователь аутентифицирован)
 * </pre>
 * </p>
 *
 * <p><b>Совместимость с SecurityFilterChain:</b><br/>
 * {@code .requestMatchers("/auth/login", "/auth/register").permitAll()} разрешает анонимный доступ к страницам.</p>
 *
 * @see org.springframework.security.authentication.AuthenticationManager
 * @see org.springframework.security.core.context.SecurityContextHolder
 * @see UserService
 */
@Controller
@RequestMapping("/auth")
public class AuthController {
    /**
     * Сервис регистрации пользователей, инжектируемый для обработки POST /auth/register.
     *
     * <p>Spring {@link org.springframework.beans.factory.annotation.Autowired @Autowired} подставляет бин
     * {@link UserService} из {@link SecurityConfig#userDetailsService()}.
     * Используется исключительно в {@link #register(RegisterForm, Model)} для:
     * <ul>
     *   <li>Проверки уникальности username через {@code existsByUsername()}</li>
     *   <li>Хеширования пароля BCrypt через {@code passwordEncoder.encode()}</li>
     *   <li>Сохранения {@link User} в БД через {@code userRepository.save()}</li>
     * </ul>
     */
    @Autowired
    private UserService userService;
    /**
     * Центральный компонент Spring Security для ручной аутентификации пользователей.
     *
     * <p>{@link org.springframework.beans.factory.annotation.Autowired @Autowired} подставляет бин
     * {@link SecurityConfig#authenticationManager(DaoAuthenticationProvider)}} — {@link org.springframework.security.authentication.ProviderManager},
     * содержащий {@link org.springframework.security.authentication.dao.DaoAuthenticationProvider}.</p>
     *
     * <p><b>Критическая роль в {@link #login(LoginForm, Model)}:</b>
     * <pre>
     * Authentication auth = authenticationManager.authenticate(
     *     new UsernamePasswordAuthenticationToken(username, rawPassword)
     * );
     * </pre>
     *
     * <p><b>Внутренний поток:</b>
     * <ol>
     *   <li>Создает {@link UsernamePasswordAuthenticationToken} (unauthenticated)</li>
     *   <li>Вызывает {@link org.springframework.security.authentication.ProviderManager#authenticate(Authentication)} ()} → DaoAuthenticationProvider</li>
     *   <li>DaoAuthenticationProvider:
     *     <ol>
     *       <li>CustomUserDetailsService.loadUserByUsername(username) → User из БД</li>
     *       <li>passwordEncoder.matches(rawPassword, user.getPassword()) → true/false</li>
     *     </ol>
     *   </li>
     *   <li>Успех → {@link UsernamePasswordAuthenticationToken}(authenticated, authorities=[ROLE_USER])</li>
     * </ol>
     * </p>
     *
     * <p>После успеха: {@code SecurityContextHolder.setContext()} сохраняет аутентификацию для фильтров.
     */
    @Autowired
    private AuthenticationManager authenticationManager;
    /**
     * GET /auth/login — отображает пустую HTML форму логина (Thymeleaf шаблон).
     *
     * <p>Spring MVC {@link org.springframework.web.bind.annotation.GetMapping @GetMapping} обрабатывает запросы
     * к странице логина. Совместим с {@link org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint},
     * который редиректит неаутентифицированных пользователей сюда.</p>
     *
     * <p>{@link org.springframework.ui.Model model.addAttribute("loginForm", new LoginForm())}:
     * <ul>
     *   <li>Создает пустой DTO {@link LoginForm} (username="", password="")</li>
     *   <li>Thymeleaf шаблон {@code "auth/login"} автоматически связывает с {@code <form th:object="${loginForm}">}</li>
     *   <li>Предотвращает {@code NullPointerException} в Thymeleaf (поля формы доступны)</li>
     * </ul>
     *
     * <p><b>Автоматический вызов:</b>
     * <pre>
     * Неаутентифицированный GET /dashboard → SecurityFilterChain
     * ↓ .anyRequest().authenticated() → AuthenticationException
     * ↓ LoginUrlAuthenticationEntryPoint → 302 redirect:/auth/login ← ЭТОТ МЕТОД
     * </pre>
     * </p>
     *
     * @param model Spring MVC Model для передачи данных в Thymeleaf
     * @return {@code "auth/login"} — логический view name → Thymeleaf → HTML
     */
    @GetMapping("/login")
    public String auth(Model model) {
        model.addAttribute("loginForm", new LoginForm());

        return "auth/login";
    }
    /**
     * GET /auth/register — отображает пустую HTML форму регистрации.
     *
     * <p>Публичная страница (permitAll() в SecurityFilterChain). Аналогично {@link #auth(Model)}:
     * <ul>
     *   <li>Создает пустой {@link RegisterForm} (username="", password="", confirmPassword="")</li>
     *   <li>Thymeleaf {@code "auth/register"} связывает с {@code th:object="${registerForm}"}</li>
     *   <li>Готовит форму для POST /auth/register</li>
     * </ul>
     * </p>
     *
     * <p>Разрешен анонимным пользователям благодаря {@code .requestMatchers("/auth/register").permitAll()}.
     * После успешной регистрации редиректит на /auth/login (Post/Redirect/Get pattern).</p>
     *
     * @param model Spring MVC Model для Thymeleaf binding
     * @return {@code "auth/register"} — view name → Thymeleaf → HTML форма регистрации
     */
    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registerForm", new RegisterForm());

        return "auth/register";
    }
    /**
     * POST /auth/login — ручная аутентификация пользователя через AuthenticationManager.
     *
     * <p>{@link org.springframework.web.bind.annotation.PostMapping @PostMapping} обрабатывает форму логина
     * (form submit). Заменяет отключенный {@link org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer#loginProcessingUrl(String)} formLogin().disable()}.
     *
     * <p>{@link org.springframework.web.bind.annotation.ModelAttribute @ModelAttribute("loginForm") LoginForm loginForm}:
     * <ul>
     *   <li>Автоматически биндит HTML form поля в DTO: username/password</li>
     *   <li>Thymeleaf: {@code <input th:field="*{username}"> → loginForm.getUsername()}</li>
     * </ul>
     * </p>
     *
     * <h3>Поток Spring Security аутентификации</h3>
     * <pre>
     * 1. Создает UsernamePasswordAuthenticationToken(username, rawPassword) — unauthenticated
     * 2. authenticationManager.authenticate(token) → ProviderManager → DaoAuthenticationProvider:
     *    a) CustomUserDetailsService.loadUserByUsername() → User из БД
     *    b) passwordEncoder.matches(rawPassword, user.getPassword()) → true/false
     * 3. Успех → UsernamePasswordAuthenticationToken(authenticated, authorities=[ROLE_USER])
     * </pre>
     *
     * <h3>Ручная установка SecurityContext (STATELESS режим)</h3>
     * <p>Поскольку {@link SecurityConfig#webSecurityFilterChain(HttpSecurity)} () SessionCreationPolicy.STATELESS},
     * SecurityContext НЕ сохраняется автоматически:
     * <pre>
     * SecurityContext context = SecurityContextHolder.createEmptyContext();
     * context.setAuthentication(authentication);     ← Устанавливает аутентификацию
     * SecurityContextHolder.setContext(context);     ← Для текущего запроса + фильтров
     * </pre>
     * После redirect:/ фильтры увидят аутентифицированного пользователя.
     * </p>
     *
     * <h3>Обработка ошибок Spring Security</h3>
     * <table border="1">
     *   <tr><th>Исключение</th><th>Обработка</th><th>Результат</th></tr>
     *   <tr><td>{@link BadCredentialsException}</td><td>"Invalid username or password."</td><td>Возврат формы + ошибка</td></tr>
     *   <tr><td>{@link AuthenticationException}</td><td>"Authentication error: " + message</td><td>Возврат формы + ошибка</td></tr>
     * </table>
     *
     * <p>{@code model.addAttribute("loginForm", loginForm)} — сохраняет заполненные поля формы.
     * Thymeleaf отображает {@code th:if="${loginError}"} + ошибку.</p>
     *
     * <p><b>Успех:</b> {@code return "redirect:/"} — Post/Redirect/Get (PRG) pattern.
     * Пользователь аутентифицирован для всех последующих запросов.</p>
     *
     * @param loginForm DTO с данными формы (биндинг через @ModelAttribute)
     * @param model для ошибок и сохранения формы
     * @return "redirect:/" при успехе или "auth/login" при ошибке
     */
    @PostMapping("/login")
    public String login(
            @ModelAttribute("loginForm") LoginForm loginForm,
            Model model
    ) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginForm.getUsername(), loginForm.getPassword())
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            return "redirect:/";
        } catch (AuthenticationException e) {
            model.addAttribute("loginError", "Invalid entered data.");
            model.addAttribute("loginForm", loginForm);

            return "auth/login";
        }
    }
    /**
     * POST /auth/register — обработка HTML формы регистрации с валидацией паролей.
     *
     * <p>{@link org.springframework.web.bind.annotation.PostMapping @PostMapping} обрабатывает submit формы регистрации.
     * {@link org.springframework.web.bind.annotation.ModelAttribute @ModelAttribute("registerForm") RegisterForm registerForm}:
     * <ul>
     *   <li>Автоматически биндит HTML: {@code <input th:field="*{username}">} → DTO поля</li>
     *   <li>Thymeleaf: username, password, confirmPassword из формы</li>
     * </ul>
     * </p>
     *
     * <h3>Двухэтапная валидация</h3>
     * <ol>
     *   <li><b>Клиентская проверка паролей:</b>
     *     <pre>
     *     if (!registerForm.getPassword().equals(registerForm.getConfirmPassword()))
     *         model.addAttribute("registerError", "Passwords do not match.");
     *         return "auth/register"; // ← возврат формы с ошибкой
     *     </pre>
     *   </li>
     *   <li><b>Серверная регистрация:</b>
     *     <pre>
     *     userService.registerUser(username, rawPassword)
     *     ↓ @Transactional
     *       existsByUsername() → проверка уникальности
     *       passwordEncoder.encode(rawPassword) → BCrypt
     *       userRepository.save(User) → INSERT БД
     *     </pre>
     *   </li>
     * </ol>
     *
     * <h3>Обработка исключений UserService</h3>
     * <p>{@link UserService#registerUser(String, String)} ()} выбрасывает {@link IllegalArgumentException}:
     * <pre>
     * if (userRepository.existsByUsername(username))
     *     throw new IllegalArgumentException("User already exists: " + username);
     * </pre>
     * Контроллер ловит → {@code model.addAttribute("registerError", e.getMessage())} →
     * Thymeleaf показывает {@code "User already exists: username"}.
     * </p>
     *
     * <h3>Post/Redirect/Get (PRG) паттерн</h3>
     * <table border="1">
     *   <tr><th>Результат</th><th>Действие</th><th>View</th></tr>
     *   <tr><td>Пароли не совпадают</td><td>{@code model.addAttribute("registerError")}</td><td>{@code "auth/register"}</td></tr>
     *   <tr><td>UserService исключение</td><td>{@code model.addAttribute("registerError")}</td><td>{@code "auth/register"}</td></tr>
     *   <tr><td>Успех</td><td>{@code userService.registerUser() ✓}</td><td>{@code "redirect:/auth/login"}</td></tr>
     * </table>
     *
     * <p><b>После успешной регистрации:</b><br/>
     * 1. Пользователь создан в БД с BCrypt паролем<br/>
     * 2. Редирект на /auth/login для немедленного логина<br/>
     * 3. {@link CustomUserDetailsService#loadUserByUsername(String)} ()} найдет нового пользователя
     * </p>
     *
     * <p><b>Совместимость с SecurityFilterChain:</b><br/>
     * {@code .requestMatchers("/auth/register").permitAll()} разрешает анонимную регистрацию.
     * После регистрации пользователь должен залогиниться для доступа к защищенным ресурсам.</p>
     *
     * @param registerForm DTO с username, password, confirmPassword из HTML формы
     * @param model для ошибок и повторного отображения формы
     * @return "redirect:/auth/login" при успехе или "auth/register" при ошибке
     */
    @PostMapping("/register")
    public String register(
            @ModelAttribute("registerForm") RegisterForm registerForm,
            Model model
    ) {
        try {
            if (!registerForm.getPassword().equals(registerForm.getConfirmPassword())) {
                model.addAttribute("registerError", "Passwords do not match.");
                return "auth/register";
            }

            userService.registerUser(registerForm.getUsername(), registerForm.getPassword());

            return "redirect:/auth/login";
        } catch (IllegalArgumentException | UserExistException e) {
            model.addAttribute("registerError", "Incorrect entered data.");

            return "auth/register";
        }
    }
}