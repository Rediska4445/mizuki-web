package rf.mizuka.web.application.security.config;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import rf.mizuka.web.application.database.rest.repository.DeveloperRepository;
import rf.mizuka.web.application.database.user.repository.UserRepository;
import rf.mizuka.web.application.models.rest.Developer;
import rf.mizuka.web.application.services.user.CustomUserDetailsService;

import java.util.Set;

/**
 * Централизованная конфигурация безопасности Spring Security для веб‑приложения.
 *
 * <p>Класс помечен аннотациями:
 * <ul>
 *   <li>{@link org.springframework.context.annotation.Configuration @Configuration} —
 *   указывает, что этот класс содержит декларации бинов ({@code @Bean}), управляемых Spring‑контейнером;</li>
 *   <li>{@link org.springframework.security.config.annotation.web.configuration.EnableWebSecurity @EnableWebSecurity} —
 *   включает базовую конфигурацию Spring Security для веб‑слоя (автоматическую регистрацию
 *   фильтров, {@code SecurityFilterChain}, по умолчанию и т.п.).</li>
 * </ul>
 * </p>
 *
 * <p>Основная роль этого класса — настроить:
 * <ul>
 *   <li>правила доступа к HTTP‑путям (кто может заходить куда — аноним, authenticated);</li>
 *   <li>механизм аутентификации на основе логин/пароль и хеша BCrypt
 *   через {@link org.springframework.security.authentication.dao.DaoAuthenticationProvider DaoAuthenticationProvider}
 *   и {@link org.springframework.security.core.userdetails.UserDetailsService UserDetailsService};</li>
 *   <li>обработку сессий и контекста безопасности (через HttpSessionSecurityContextRepository);</li>
 *   <li>обработку ошибок (access denied, неавторизованный доступ) и перенаправление на форму логина.</li>
 * </ul>
 * </p>
 *
 * <p>Конкретные компоненты, которые он конфигурирует:
 * <ul>
 *   <li>{@link org.springframework.security.web.SecurityFilterChain SecurityFilterChain} —
 *   задаёт цепочку фильтров Spring Security для HTTP‑запросов. В этом случае:
 *   <ul>
 *     <li>включён {@link org.springframework.security.web.context.HttpSessionSecurityContextRepository HttpSessionSecurityContextRepository}
 *     для хранения {@link org.springframework.security.core.context.SecurityContext SecurityContext}
 *     в HTTP‑сессии, что позволяет сохранять состояние аутентификации между запросами;</li>
 *     <li>разрешены публичные доступы к {@code /auth/login}, {@code /auth/register} и любым
 *     путям {@code /api/auth/**}, а все остальные пути требуют аутентификации;</li>
 *     <li>анонимный доступ отключён
 *     (через {@link org.springframework.security.config.annotation.web.configurers.AnonymousConfigurer AnonymousConfigurer})
 *     и вместо стандартной формы логина, управляемой Spring Security, используется своя
 *     (через отключение formLogin);</li>
 *     <li>произведена настройка CSRF: используется
 *     {@link org.springframework.security.web.csrf.CookieCsrfTokenRepository CookieCsrfTokenRepository}
 *     с HTTP‑только cookie, чтобы смягчить влияние XSS‑атак, сохраняя CSRF‑токен
 *     в клиентском хранилище.</li>
 *   </ul>
 *   </li>
 *   <li>Бин {@link org.springframework.security.core.userdetails.UserDetailsService UserDetailsService},
 *   возвращаемый методом {@code userDetailsService()}, — это кастомный сервис, основанный
 *   на {@code CustomUserDetailsService}, который читает данные пользователя из
 *   {@link UserRepository UserRepository}.
 *   Это стандартный паттерн Spring Security: информация о пользователе извлекается
 *   через {@code UserDetailsService}, а уже сама аутентификация выполняется через
 *   {@code DaoAuthenticationProvider}.</li>
 *   <li>Бин {@link org.springframework.security.authentication.dao.DaoAuthenticationProvider DaoAuthenticationProvider},
 *   возвращаемый методом {@code daoAuthenticationProvider(UserDetailsService)},
 *   связывает {@code UserDetailsService} с {@link org.springframework.security.crypto.password.PasswordEncoder PasswordEncoder}
 *   (который, как правило, является BCrypt) и обеспечивает базовую проверку логина/пароля
 *   через стандартный механизм Spring Security.</li>
 *   <li>Бин {@link org.springframework.security.authentication.AuthenticationManager AuthenticationManager},
 *   возвращаемый методом {@code authenticationManager(DaoAuthenticationProvider)},
 *   оборачивает {@code DaoAuthenticationProvider} в {@link org.springframework.security.authentication.ProviderManager ProviderManager},
 *   который последовательно применяет провайдеры для аутентификации и является центральным
 *   компонентом, к которому обращаются фильтры и аутентификаторы при входе пользователя.</li>
 *   <li>Обработчик доступа {@link org.springframework.security.web.access.AccessDeniedHandler AccessDeniedHandler},
 *   возвращаемый методом {@code accessDeniedHandler()}, — это кастомный обработчик,
 *   который при попытке доступа к защищённому ресурсу без прав отправляет пользователя
 *   на страницу логина через {@link org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint LoginUrlAuthenticationEntryPoint},
 *   как и стандартный {@code AuthenticationEntryPoint}, но с явным контролем перенаправления.</li>
 * </ul>
 * </p>
 *
 * <p>Таким образом, данный класс:
 * <ul>
 *   <li>является корневой конфигурацией Spring Security для веб‑приложения;</li>
 *   <li>настраивает фильтрацию запросов, сессии, CSRF и обработку ошибок;</li>
 *   <li>определяет цепочку компонентов аутентификации (UserDetailsService, DaoAuthenticationProvider,
 *   AuthenticationManager), работающих на основе логина/пароля и хеширования BCrypt;</li>
 *   <li>подготавливает безопасную инфраструктуру, в которой веб‑страницы логина и регистрации
 *   и последующие страницы приложения корректно защищены правилами доступа и механизмами
 *   аутентификации.</li>
 * </ul>
 * </p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    /**
     * Репозиторий пользователей, используемый для загрузки данных пользователей из базы данных.
     *
     * <p>Поле вводится в контекст контроллера через {@link org.springframework.beans.factory.annotation.Autowired @Autowired},
     * что позволяет Spring Security и {@link CustomUserDetailsService CustomUserDetailsService}
     * получать доступ к данным пользователей (логин, пароль, роли). В сочетании с интерфейсом
     * {@link UserRepository UserRepository}
     * этот репозиторий обеспечивает стандартный доступ к сущностям {@code User} в базе данных,
     * предоставляя операции поиска и загрузки по идентификатору и логину.</p>
     *
     * <p>Использование {@code @Autowired} здесь:
     * <ul>
     *   <li>упрощает зависимость от данных пользователей, избавляя от необходимости создавать
     *   объект {@code UserRepository} вручную;</li>
     *   <li>позволяет контроллеру и сервисам аутентификации легко интегрироваться с
     *   базой данных и обеспечивает централизованное управление данных пользователей.</li>
     * </ul>
     * </p>
     */
    @Autowired
    private UserRepository userRepository;
    /**
     * Шифровальщик паролей, используемый для безопасного хранения и проверки паролей.
     *
     * <p>Поле помечено аннотациями {@link org.springframework.beans.factory.annotation.Autowired @Autowired}
     * и {@link org.springframework.beans.factory.annotation.Qualifier @Qualifier("bCryptPasswordEncoder")},
     * что указывает на использование именно этого бина {@code PasswordEncoder}
     * для кодирования и сравнения хэшей паролей. В контексте Spring Security это обеспечивает
     * равномерное использование одного и того же шифровальщика для всех операций с паролями.</p>
     *
     * <p>Использование {@code @Qualifier} гарантирует:
     * <ul>
     *   <li>что в контексте используется именно BCrypt шифровальщик, а не какой-либо другой
     *   энкодер;</li>
     *   <li>что хранение и проверка паролей происходят единообразно и совместимо с базой данных.</li>
     * </ul>
     * </p>
     */
    @Autowired
    @Qualifier("bCryptPasswordEncoder")
    private PasswordEncoder passwordEncoder;

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(path -> {
                    System.out.println(path.getRequestURI());

                    return !path.getRequestURI().startsWith("/api") && !path.getRequestURI().startsWith("/error");
                })
                .securityContext(securityContext -> securityContext
                        .securityContextRepository(new HttpSessionSecurityContextRepository())
                        .requireExplicitSave(false)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/favicon.ico", "/css/**", "/js/**").permitAll()
                        .requestMatchers("/auth/login", "/auth/register").permitAll()
                        .requestMatchers("/dashboard/developers/**").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> new LoginUrlAuthenticationEntryPoint("/auth/login")
                                .commence(request, response, authException))
                        .accessDeniedHandler((request, response, accessDeniedException) -> response.sendRedirect("/auth/login"))
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                )
                .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }

    /**
     * Создает кастомный обработчик отказа в доступе (AccessDeniedHandler) для различения API и HTML запросов.
     *
     * <p>Реализует интерфейс {@link org.springframework.security.web.access.AccessDeniedHandler AccessDeniedHandler},
     * который Spring Security вызывает в следующих случаях:
     * <blockquote>
     * У пользователя есть действительная {@link org.springframework.security.core.Authentication Authentication}
     * (он аутентифицирован), НО у него недостаточно прав/ролей для доступа к ресурсу
     * </blockquote>
     *
     * <p>Обработчик активируется {@link org.springframework.security.web.access.ExceptionTranslationFilter}
     * при перехвате {@link org.springframework.security.access.AccessDeniedException}. Это отличается от
     * {@link org.springframework.security.web.AuthenticationEntryPoint}, который вызывается при ОТСУТСТВИИ
     * аутентификации ({@code 401}).</p>
     *
     * <h3>Логика обработки запросов</h3>
     * <table border="1" style="width:100%">
     *   <tr><th>Тип запроса</th><th>Условие</th><th>Действие</th><th>HTTP статус</th></tr>
     *   <tr><td>API</td><td>{@code request.getServletPath().startsWith("/api/")}</td><td>{@code response.setStatus(403)}</td><td>FORBIDDEN</td></tr>
     *   <tr><td>HTML (веб)</td><td>иначе</td><td>редирект через EntryPoint</td><td>302 Found</td></tr>
     * </table>
     *
     * <p><b>LoginUrlAuthenticationEntryPoint</b> — стандартный компонент Spring Security
     * ({@link org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint}):
     * <ul>
     *   <li>Создается с URL {@code "/auth/login"}</li>
     *   <li>{@code entryPoint.commence(request, response, null)} генерирует редирект:
     *     <ul>
     *       <li>{@code Location: /auth/login}</li>
     *       <li>Поскольку передается {@code null} вместо {@link org.springframework.security.access.AccessDeniedException},
     *           параметры {@code ?error} или {@code SpringSecurityAccessDeniedException} НЕ добавляются</li>
     *     </ul>
     *   </li>
     *   <li>Автоматически сохраняет исходный URL в сессии под ключом
     *       {@code "SPRING_SECURITY_SAVED_REQUEST"} для возврата после логина</li>
     * </ul>
     * </p>
     *
     * <h3>Интеграция в SecurityFilterChain</h3>
     * <p>Этот бин может использоваться:
     * <ul>
     *   <li>В {@code .exceptionHandling().accessDeniedHandler(accessDeniedHandler())} — как замена inline-лямбды</li>
     *   <li>В кастомных фильтрах через {@code SecurityContextHolder}</li>
     *   <li>Глобально через {@link org.springframework.security.config.annotation.web.builders.HttpSecurity#exceptionHandling}
     *       для всех SecurityFilterChain</li>
     * </ul>
     *
     * <p><b>Полный поток при AccessDeniedException:</b><br/>
     * 1. FilterSecurityInterceptor проверяет authorities → AccessDeniedException<br/>
     * 2. ExceptionTranslationFilter перехватывает → вызывает AccessDeniedHandler<br/>
     * 3. API: setStatus(403) → ответ клиенту<br/>
     * 4. HTML: EntryPoint.commence() → redirect /auth/login → новый запрос
     * </p>
     *
     * @return {@link AccessDeniedHandler} lambda-реализация для обработки 403 ошибок
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        LoginUrlAuthenticationEntryPoint entryPoint =
                new LoginUrlAuthenticationEntryPoint("/auth/login");

        return (request, response, accessDeniedException) -> {
            if (request.getServletPath().startsWith("/api/")) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
            } else {
                entryPoint.commence(request, response, null);
            }
        };
    }

    /**
     * Возвращает сервис пользовательских данных, который загружает информацию о пользователях из базы данных.
     *
     * <p>Аннотация {@link org.springframework.context.annotation.Bean @Bean} указывает, что
     * этот метод создает бин {@link org.springframework.security.core.userdetails.UserDetailsService UserDetailsService},
     * используемый Spring Security для аутентификации и загрузки данных пользователя.</p>
     *
     * <p>Сервис {@code CustomUserDetailsService} реализует интерфейс {@link org.springframework.security.core.userdetails.UserDetailsService UserDetailsService}
     * и использует {@link UserRepository UserRepository}
     * для загрузки данных пользователя по его логину. Это стандартный паттерн для работы с пользовательскими
     * данными в Spring Security.</p>
     *
     * @return экземпляр {@link org.springframework.security.core.userdetails.UserDetailsService UserDetailsService},
     *         который загружает данные пользователя из базы данных.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return new CustomUserDetailsService(userRepository);
    }
    /**
     * Возвращает провайдер аутентификации DAO, который проверяет логин и пароль пользователей
     * через {@link org.springframework.security.core.userdetails.UserDetailsService UserDetailsService} и {@link org.springframework.security.crypto.password.PasswordEncoder PasswordEncoder}.
     *
     * <p>Аннотация {@link org.springframework.context.annotation.Bean @Bean} указывает, что
     * этот метод создает бин {@link org.springframework.security.authentication.dao.DaoAuthenticationProvider DaoAuthenticationProvider},
     * используемый для аутентификации на основе логина и пароля.</p>
     *
     * <p>Метод принимает в качестве параметра {@code UserDetailsService}, который загружает данные пользователя,
     * и настраивает его через {@code DaoAuthenticationProvider}. Пароль кодируется с помощью
     * {@link org.springframework.security.crypto.password.PasswordEncoder PasswordEncoder}, что обеспечивает безопасное хранение и проверку паролей.</p>
     *
     * @param userDetailsService экземпляр {@link org.springframework.security.core.userdetails.UserDetailsService UserDetailsService},
     *                           используемый для загрузки данных пользователя.
     * @return экземпляр {@link org.springframework.security.authentication.dao.DaoAuthenticationProvider DaoAuthenticationProvider},
     *         который проверяет аутентификацию через логин и пароль.
     */
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(
            UserDetailsService userDetailsService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }
    /**
     * Возвращает менеджер аутентификации, который управляется через {@link org.springframework.security.authentication.ProviderManager ProviderManager}
     * и использует {@link org.springframework.security.authentication.dao.DaoAuthenticationProvider DaoAuthenticationProvider}
     * для аутентификации пользователей.
     *
     * <p>Аннотация {@link org.springframework.context.annotation.Bean @Bean} указывает, что
     * этот метод создает бин {@link org.springframework.security.authentication.AuthenticationManager AuthenticationManager},
     * используемый для обработки всех запросов аутентификации в приложении.</p>
     *
     * <p>Менеджер {@code AuthenticationManager} оборачивает {@code DaoAuthenticationProvider},
     * добавляя дополнительные функции, такие как обработка нескольких провайдеров и логирование,
     * обеспечивая централизованное управление аутентификацией.</p>
     *
     * @param daoAuthenticationProvider экземпляр {@link org.springframework.security.authentication.dao.DaoAuthenticationProvider DaoAuthenticationProvider},
     *                                  используемый для аутентификации пользователей.
     * @return экземпляр {@link org.springframework.security.authentication.AuthenticationManager AuthenticationManager},
     *         который управляет аутентификацией через заданный провайдер.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            DaoAuthenticationProvider daoAuthenticationProvider) {
        return new ProviderManager(daoAuthenticationProvider);
    }
}