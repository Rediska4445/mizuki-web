package rf.mizuka.web.application.auth.config;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
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
import rf.mizuka.web.application.auth.database.repository.UserRepository;
import rf.mizuka.web.application.auth.service.CustomUserDetailsService;

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
 *   {@link rf.mizuka.web.application.auth.database.repository.UserRepository UserRepository}.
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
     * что позволяет Spring Security и {@link rf.mizuka.web.application.auth.service.CustomUserDetailsService CustomUserDetailsService}
     * получать доступ к данным пользователей (логин, пароль, роли). В сочетании с интерфейсом
     * {@link rf.mizuka.web.application.auth.database.repository.UserRepository UserRepository}
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

    /**
     * Конфигурирует и возвращает главную цепочку фильтров безопасности Spring Security с приоритетом 1.
     *
     * <p>В Spring Security 6+ используется компонент-бин {@link SecurityFilterChain @Bean SecurityFilterChain},
     * который представляет собой упорядоченную цепочку фильтров безопасности. Когда HTTP-запрос приходит в
     * контейнер сервлетов (Tomcat), он сначала проходит через фильтр {@link org.springframework.web.filter.DelegatingFilterProxy}
     * с именем "springSecurityFilterChain". Этот прокси делегирует обработку {@link org.springframework.security.web.FilterChainProxy},
     * который сканирует все доступные бины SecurityFilterChain, сортирует их по {@link Order @Order} и выбирает
     * первую (с наименьшим значением Order) цепочку, чьи requestMatchers() подходят под текущий запрос.
     * {@code @Order(1)} гарантирует высший приоритет среди других цепочек.</p>
     *
     * <h3>1. SecurityContext (Хранение контекста аутентификации)</h3>
     * <p>{@code .securityContext(securityContext -> ...)} настраивает {@link SecurityContextRepository},
     * который отвечает за персистентность {@link SecurityContext} между запросами:
     * <ul>
     *   <li>{@code new HttpSessionSecurityContextRepository()} — стандартный репозиторий Spring Security,
     *       сохраняет SecurityContext в атрибуте сессии {@code "SPRING_SECURITY_CONTEXT"} под ключом
     *       {@link HttpSessionSecurityContextRepository#SPRING_SECURITY_CONTEXT_KEY SPRING_SECURITY_CONTEXT_KEY}</li>
     *   <li>{@code requireExplicitSave(false)} — отключает требование явного вызова
     *       {@code securityContextRepository.saveContext()}. SecurityContext автоматически сохраняется:
     *       <ul>
     *         <li>при завершении запроса (postHandle/afterCompletion)</li>
     *         <li>при изменении аутентификации (setAuthentication())</li>
     *       </ul>
     *   </li>
     * </ul>
     * Если сессия не существует, создается новая. SecurityContext содержит {@link Authentication} текущего пользователя.
     * </p>
     *
     * <h3>2. Авторизация HTTP-запросов</h3>
     * <p>{@code .authorizeHttpRequests(auth -> ...)} заменяет устаревшие {@code antMatchers()} и определяет
     * правила доступа:
     * <table border="1">
     *   <tr><th>Путь</th><th>Доступ</th><th>Фильтр</th></tr>
     *   <tr><td>{@code /api/auth/**}</td><td>permitAll()</td><td>Все пользователи</td></tr>
     *   <tr><td>{@code /auth/login}, {@code /auth/register}</td><td>permitAll()</td><td>Все пользователи</td></tr>
     *   <tr><td>ВСЕ остальные</td><td>authenticated()</td><td>Только аутентифицированные</td></tr>
     * </table>
     * Проверка происходит последним фильтром цепочки. Если пользователь не аутентифицирован для protected-ресурса,
     * выбрасывается {@link org.springframework.security.access.AccessDeniedException} или
     * {@link org.springframework.security.core.AuthenticationException}.
     * </p>
     *
     * <h3>3. Обработка исключений безопасности</h3>
     * <p>{@code .exceptionHandling(ex -> ...)} перехватывает исключения ДО их передачи в контейнер сервлетов:
     * <ul>
     *   <li><b>{@link AuthenticationEntryPoint}</b> — вызывается при отсутствии аутентификации
     *       ({@code 401 Unauthorized}):
     *     <ul>
     *       <li>API-запросы ({@code /api/*}) → {@code response.setStatus(401)}</li>
     *       <li>HTML-запросы → {@link LoginUrlAuthenticationEntryPoint} редиректит на
     *           {@code /auth/login?error} с параметрами из {@link AuthenticationException}</li>
     *     </ul>
     *   </li>
     *   <li><b>{@link AccessDeniedHandler}</b> — вызывается при наличии аутентификации, но отсутствии прав
     *       ({@code 403 Forbidden}):
     *     <ul>
     *       <li>API-запросы ({@code /api/*}) → {@code response.setStatus(403)}</li>
     *       <li>HTML-запросы → {@code response.sendRedirect("/auth/login")}</li>
     *     </ul>
     *   </li>
     * </ul>
     * Обработчики регистрируются в {@link org.springframework.security.web.access.ExceptionTranslationFilter}.
     * </p>
     *
     * <h3>4. Управление сессиями (STATELESS)</h3>
     * <p>{@code .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))}
     * полностью отключает серверные сессии:
     * <ul>
     *   <li>НЕ создаются {@link HttpSession}</li>
     *   <li>{@link HttpSessionSecurityContextRepository} игнорируется</li>
     *   <li>НЕ устанавливаются куки {@code JSESSIONID}</li>
     *   <li>Каждый запрос должен содержать полную информацию аутентификации (JWT в Authorization header)</li>
     * </ul>
     * Идеально для REST API + SPA приложений. Конфликт с securityContext() игнорируется Spring Security.
     * </p>
     *
     * <h3>5. CSRF Защита</h3>
     * <p>{@code .csrf(csrf -> ...)} настраивает {@link org.springframework.security.web.csrf.CsrfFilter}:
     * <ul>
     *   <li>{@code .ignoringRequestMatchers("/api/**")} — API полностью игнорирует CSRF проверки
     *       (GET, HEAD, TRACE, OPTIONS автоматически игнорируются)</li>
     *   <li>{@code CookieCsrfTokenRepository.withHttpOnlyFalse()} — CSRF-токен хранится в куки:
     *     <table border="1">
     *       <tr><th>Параметр</th><th>Значение</th></tr>
     *       <tr><td>Имя куки</td><td>{@code XSRF-TOKEN}</td></tr>
     *       <tr><td>HttpOnly</td><td>{@code false} (доступно JavaScript)</td></tr>
     *       <tr><td>Заголовок</td><td>Клиент должен отправить {@code X-XSRF-TOKEN}</td></tr>
     *     </table>
     *     SPA (React/Angular) читает куки и добавляет заголовок автоматически.
     *   </li>
     * </ul>
     * </p>
     *
     * <h3>6. Отключение встроенной формы логина</h3>
     * <p>{@code .formLogin(AbstractHttpConfigurer::disable)} полностью отключает:
     * <ul>
     *   <li>{@link org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter}</li>
     *   <li>Автоматические HTML-страницы логина Spring Security</li>
     *   <li>POST {@code /login} эндпоинт</li>
     * </ul>
     * Логин реализуется вручную через контроллер + {@link AuthenticationManager}.
     * </p>
     *
     * <p><b>Полный поток обработки запроса:</b><br/>
     * 1. DelegatingFilterProxy → FilterChainProxy → SecurityFilterChain (@Order=1)<br/>
     * 2. SecurityContextPersistenceFilter (loadContext)<br/>
     * 3. ... другие фильтры ...<br/>
     * 4. FilterSecurityInterceptor (проверка authorizeHttpRequests)<br/>
     * 5. ExceptionTranslationFilter (обработка исключений)<br/>
     * 6. Ваш контроллер/ресурс<br/>
     * 7. SecurityContextPersistenceFilter (saveContext, если не STATELESS)
     * </p>
     *
     * @param http {@link HttpSecurity} для fluent-конфигурации цепочки фильтров
     * @return полностью настроенная {@link SecurityFilterChain} для регистрации Spring контейнером
     * @throws Exception при ошибках конфигурации фильтров
     */
    @Bean
    @Order(1)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityContext(securityContext -> securityContext
                        .securityContextRepository(new HttpSessionSecurityContextRepository())
                        .requireExplicitSave(false)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/auth/login", "/auth/register").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (request.getServletPath().startsWith("/api/")) {
                                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            } else {
                                new LoginUrlAuthenticationEntryPoint("/auth/login")
                                        .commence(request, response, authException);
                            }
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            if (request.getServletPath().startsWith("/api/")) {
                                response.setStatus(HttpStatus.FORBIDDEN.value());
                            } else {
                                response.sendRedirect("/auth/login");
                            }
                        })
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**")
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                )
                .formLogin(AbstractHttpConfigurer::disable);

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
     * и использует {@link rf.mizuka.web.application.auth.database.repository.UserRepository UserRepository}
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