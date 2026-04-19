package rf.mizuka.web.application.services.rest;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import rf.mizuka.web.application.database.rest.repository.DeveloperRepository;
import rf.mizuka.web.application.models.rest.Application;

import java.time.Instant;

/**
 * Сервис OAuth2 авторизации для REST API клиентов (Client Credentials Flow).
 *
 * <p>Класс помечен аннотацией {@link org.springframework.stereotype.Service @Service}, что делает его
 * Spring-управляемым компонентом сервисного слоя. Автоматически регистрируется в ApplicationContext
 * и доступен для инъекции в контроллеры REST API.</p>
 *
 * <p><b>Основная роль:</b> Реализует <b>OAuth2 Client Credentials Grant</b> для машинной аутентификации
 * внешних REST клиентов (мобильные приложения, сервисы, Postman). Выдает JWT токен после проверки
 * {@code client_id} + {@code client_secret}.</p>
 *
 * <h3>Конструктор инъекция Spring (Lombok-free):</h3>
 * <ul>
 *   <li>{@link rf.mizuka.web.application.database.rest.repository.DeveloperRepository DeveloperRepository} —
 *      Spring Data JPA репозиторий для поиска приложения по {@code clientId};</li>
 *   <li>{@link org.springframework.security.crypto.password.PasswordEncoder PasswordEncoder} —
 *      BCrypt для проверки {@code clientSecret} (хэш в БД);</li>
 *   <li>{@link org.springframework.security.oauth2.jwt.JwtEncoder JwtEncoder} —
 *      Spring Security OAuth2 генератор JWT токенов (Nimbus JOSE + JWT).</li>
 * </ul>
 *
 * <h3>OAuth2 Client Credentials Flow (стандарт RFC 6749):</h3>
 * <pre>
 * 1. Клиент POST /oauth2/token:
 *    client_id=app123&client_secret=secret456&grant_type=client_credentials
 * 2. AuthApiService.login(clientId, clientSecret):
 *    a) findByClientId("app123") → Application из БД
 *    b) passwordEncoder.matches("secret456", BCrypt_hash) → true/false
 *    c) JwtEncoder → JWT токен с claims (sub=clientId, scp=scopes, exp=1h)
 * 3. Ответ: {"access_token": "eyJhbGciOiJSUzI1NiIs...", "token_type": "Bearer"}
 * </pre>
 *
 * <h3>Spring Security OAuth2 JWT механизм:</h3>
 * <ul>
 *   <li>{@link org.springframework.security.oauth2.jwt.JwtEncoder JwtEncoder} — бин из конфигурации
 *      (RSA ключи или HMAC secret). Генерирует signed JWT (JWS);</li>
 *   <li>{@link org.springframework.security.oauth2.jwt.JwtClaimsSet JwtClaimsSet} — стандартные claims:
 *      <ul>
 *        <li>{@code iss: "self"} — issuer приложения;</li>
 *        <li>{@code iat: Instant.now()} — issued at;</li>
 *        <li>{@code exp: now + 3600s} — expires in 1 час;</li>
 *        <li>{@code sub: clientId} — subject (идентификатор клиента);</li>
 *        <li>{@code scp: "read write"} — scopes из БД приложения.</li>
 *      </ul>
 *   </li>
 *   <li>{@link org.springframework.security.oauth2.jwt.JwtEncoderParameters JwtEncoderParameters.from(claims)}
 *      → encode() → компактный JWT string.</li>
 * </ul>
 *
 * <h3>Валидация Client Credentials:</h3>
 * <ul>
 *   <li>{@code developerRepository.findByClientId(clientId).orElseThrow()} —<br/>
 *      Spring Data JPA: {@code SELECT * FROM applications WHERE client_id = ?}<br/>
 *      → {@link BadCredentialsException} при отсутствии приложения (стандарт OAuth2);</li>
 *   <li>{@code passwordEncoder.matches(clientSecret, application.getClientSecret())} —<br/>
 *      BCrypt проверка: raw secret vs хэш из БД → {@link BadCredentialsException} при несовпадении.</li>
 * </ul>
 *
 * <p><b>GlobalExceptionHandler перехватывает:</b><br/>
 * {@code BadCredentialsException} → HTTP 401 {"error": "Invalid Client ID/Secret"}.</p>
 *
 * <h3>Scopes из БД приложения:</h3>
 * <ul>
 *   <li>{@code application.getScopes()} — список разрешений (["read", "write"]);</li>
 *   <li>{@code String.join(" ", scopes)} → {@code "read write"} (OAuth2 стандарт);</li>
 *   <li>JWT claim {@code "scp": "read write"} — используется ResourceServer при валидации Bearer токена.</li>
 * </ul>
 *
 * <p><b>Жизненный цикл JWT:</b>
 * <ol>
 *   <li>Клиент получает JWT на 1 час ({@code now.plusSeconds(3600)});</li>
 *   <li>Каждый защищенный запрос: {@code Authorization: Bearer eyJhbGciOiJSUzI1Ni...};</li>
 *   <li>{@link org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider JwtAuthenticationProvider}
 *      валидирует подпись, exp, iss → извлекает sub/scp;</li>
 *   <li>HTTP 401 при истечении или неверной подписи.</li>
 * </ol>
 * </p>
 *
 * <p><b>Преимущества архитектуры:</b>
 * <ul>
 *   <li>Стандарт OAuth2 Client Credentials — совместим с Postman, curl, любыми клиентами;</li>
 *   <li>BCrypt для client_secret — безопасное хранение (не plain text);</li>
 *   <li>JWT stateless — нет сессий в Redis;</li>
 *   <li>Scopes из БД — динамическое управление правами приложений;</li>
 *   <li>Spring Security OAuth2 — enterprise-grade безопасность.</li>
 * </ul>
 * </p>
 *
 * @see org.springframework.security.oauth2.jwt.JwtEncoder
 * @see org.springframework.security.crypto.password.PasswordEncoder
 * @see org.springframework.security.oauth2.core.OAuth2AccessToken
 */
@Service
public class AuthApiService {
    /**
     * Spring Data JPA репозиторий для поиска OAuth2 приложения по client_id.
     *
     * <p>Инжектируется через конструктор (final field). Spring создает прокси-объект
     * {@link org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean}.</p>
     *
     * <p><b>Критическая роль в OAuth2 Client Credentials:</b><br/>
     * {@code findByClientId(clientId).orElseThrow()} — метод, сгенерированный Spring Data JPA:
     * <ul>
     *   <li>Парсинг имени → JPQL: {@code SELECT a FROM Application a WHERE a.clientId = ?1};</li>
     *   <li>{@code SELECT * FROM applications WHERE client_id = ? LIMIT 1} → {@link java.util.Optional}&lt;Application&gt;;</li>
     *   <li>Автоматическая привязка параметра ({@code ?1 = clientId});</li>
     *   <li>Возврат пустого {@link java.util.Optional} при отсутствии записи → {@link BadCredentialsException}.</li>
     * </ul>
     * </p>
     *
     * <p><b>Транзакционность:</b> Методы репозитория по умолчанию в {@link org.springframework.transaction.annotation.Transactional @Transactional(readOnly=true)}.</p>
     *
     * <p><b>Производительность:</b> Обязателен индекс {@code CREATE INDEX ON applications(client_id)} для быстрого поиска.</p>
     */
    private final DeveloperRepository developerRepository;
    /**
     * BCrypt PasswordEncoder Spring Security для проверки client_secret.
     *
     * <p>Инжектируется из бина конфигурации ({@code @Bean BCryptPasswordEncoder}).</p>
     *
     * <p><b>OAuth2 роль:</b> {@code passwordEncoder.matches(clientSecret, application.getClientSecret())}:
     * <table border="1">
     *   <tr><th>Регистрация app</th><th>Логин</th></tr>
     *   <tr><td>{@code encode("secret123") → "$2a$10$..."}</td><td>{@code matches("secret123", "$2a$10$...") → true}</td></tr>
     * </table>
     * clientSecret в БД **всегда хэш**, никогда plain text.</p>
     *
     * <p>{@link BadCredentialsException} при несовпадении — стандарт OAuth2 для неверных учетных данных.</p>
     */
    private final PasswordEncoder passwordEncoder;
    /**
     * Spring Security OAuth2 JWT Encoder для генерации Bearer токенов.
     *
     * <p>Инжектируется из конфигурации ({@code NimbusJwtEncoder} с RSA/HMAC ключами).</p>
     *
     * <p><b>Механизм:</b> {@code jwtEncoder.encode(JwtEncoderParameters.from(claims))}:
     * <ul>
     *   <li>Принимает {@link org.springframework.security.oauth2.jwt.JwtClaimsSet JwtClaimsSet} (iss, sub, scp, exp);</li>
     *   <li>Подписывает JWS (RSA/EC или HMAC);</li>
     *   <li>Возвращает компактный JWT string ({@code eyJhbGciOiJSUzI1NiJ9...});</li>
     * </ul>
     * </p>
     *
     * <p><b>Стандартные claims в login():</b>
     * <ul>
     *   <li>{@code iss: "self"} — issuer;</li>
     *   <li>{@code sub: clientId} — идентификатор приложения;</li>
     *   <li>{@code scp: "read write"} — scopes из БД;</li>
     *   <li>{@code exp: +1h} — TTL токена.</li>
     * </ul>
     * </p>
     *
     * <p>Валидация токена на защищенных эндпоинтах: {@link org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider}.</p>
     */
    private final JwtEncoder jwtEncoder;
    /**
     * Конструктор сервисного слоя с инъекцией зависимостей Spring (Constructor Injection).
     *
     * <p><b>Почему конструктор, а не @Autowired поля:</b>
     * <ul>
     *   <li><strong>Immutable:</strong> {@code final} поля — гарантированно инициализированы;</li>
     *   <li><strong>Тестируемость:</strong> легко мокать в @MockBean или new AuthApiService(mockRepo, ...);</li>
     *   <li><strong>Fail-fast:</strong> Spring бросает {@code NoSuchBeanDefinitionException} при старте, если бин отсутствует;</li>
     *   <li><strong>Стандарт Spring Boot 2.5+:</strong> Авто-инъекция по типу без @Autowired.</li>
     * </ul>
     * </p>
     *
     * <h3>Инжектируемые бины Spring:</h3>
     * <table border="1">
     *   <tr><th>Параметр</th><th>Источник</th><th>Тип бины</th></tr>
     *   <tr><td>{@code DeveloperRepository}</td><td>{@link org.springframework.data.jpa.repository.JpaRepository @Repository}</td><td>Spring Data JPA Proxy</td></tr>
     *   <tr><td>{@code PasswordEncoder}</td><td>{@code @Bean BCryptPasswordEncoder() in SecurityConfig}</td><td>Spring Security Crypto</td></tr>
     *   <tr><td>{@code JwtEncoder}</td><td>{@code NimbusJwtEncoder(rsaKeySource) in OAuth2Config}</td><td>Spring OAuth2 JWT</td></tr>
     * </table>
     *
     * <p><b>Жизненный цикл создания Spring:</b>
     * <pre>
     * 1. Spring сканирует @Service → создает прокси;
     * 2. ApplicationContext.getBean(DeveloperRepository.class) → JPA proxy;
     * 3. ApplicationContext.getBean(PasswordEncoder.class) → BCrypt бин;
     * 4. ApplicationContext.getBean(JwtEncoder.class) → Nimbus encoder;
     * 5. new AuthApiService(repo, encoder, jwtEncoder) → готовый сервис;
     * 6. Регистрирует в контексте → инжектирует в контроллеры.
     * </pre>
     * </p>
     *
     * <p><b>Циклические зависимости:</b> Невозможны с final полями — компилятор не позволит.</p>
     *
     * <p><b>Ленивая инициализация:</b> По умолчанию EAGER. Для @Lazy — аннотация на поле/конструктор.</p>
     *
     *
     * @param developerRepository Spring Data JPA для поиска Application по client_id
     * @param passwordEncoder BCrypt для валидации client_secret
     * @param jwtEncoder OAuth2 JWT генератор с RSA/HMAC подписью
     */
    public AuthApiService(DeveloperRepository developerRepository,
                          PasswordEncoder passwordEncoder,
                          JwtEncoder jwtEncoder) {
        this.developerRepository = developerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
    }
    /**
     * OAuth2 Client Credentials Grant — выдача JWT Bearer токена для REST API клиентов.
     *
     * <p><b>Стандарт RFC 6749 §4.4 Client Credentials Flow:</b> Машинная аутентификация для сервисов/приложений
     * (не интерактивные пользователи). Вызывается контроллером {@code POST /oauth2/token}.</p>
     *
     * <h3>Полный алгоритм (OAuth2 + Spring Security):</h3>
     * <ol>
     *   <li><strong>Валидация Client ID:</strong><br/>
     *      {@code developerRepository.findByClientId(clientId).orElseThrow()} →<br/>
     *      Spring Data JPA: {@code SELECT * FROM applications WHERE client_id = ?1} → {@link Application}<br/>
     *      → {@link org.springframework.security.authentication.BadCredentialsException BadCredentialsException("Invalid Client ID")}</li>
     *   <li><strong>Валидация Client Secret:</strong><br/>
     *      {@code passwordEncoder.matches(clientSecret, application.getClientSecret())} → BCrypt check<br/>
     *      → {@link BadCredentialsException} при несовпадении</li>
     *   <li><strong>JWT Claims генерация:</strong>
     *      <ul>
     *        <li>{@code now = Instant.now()} — issued at (iat);</li>
     *        <li>{@code expiry = now.plusSeconds(3600)} — expires in 1 час (стандарт);</li>
     *        <li>{@code scope = String.join(" ", application.getScopes())} → "read write";</li>
     *        <li>{@link org.springframework.security.oauth2.jwt.JwtClaimsSet.Builder JwtClaimsSet.builder()}:
     *          <ul>
     *            <li>{@code .issuer("self")} — iss (issuer идентификатор);</li>
     *            <li>{@code .issuedAt(now)} — iat;</li>
     *            <li>{@code .expiresAt(expiry)} — exp;</li>
     *            <li>{@code .subject(application.getClientId())} — sub (client идентификатор);</li>
     *            <li>{@code .claim("scp", scope)} — scp (scopes OAuth2 стандарт).</li>
     *          </ul>
     *        </li>
     *      </ul>
     *   </li>
     *   <li><strong>JWT генерация:</strong><br/>
     *      {@code jwtEncoder.encode(JwtEncoderParameters.from(claims))} → JWS (signed JWT)<br/>
     *      → {@code .getTokenValue()} → компактная строка {@code eyJhbGciOiJSUzI1NiJ9...}</li>
     * </ol>
     *
     * <h3>Формат ответа контроллера:</h3>
     * <pre>
     * HTTP 200 OK
     * Content-Type: application/json
     * {
     *   "access_token": "eyJhbGciOiJSUzI1NiJ9...",
     *   "token_type": "Bearer",
     *   "expires_in": 3600,
     *   "scope": "read write"
     * }
     * </pre>
     *
     * <h3>Spring Security OAuth2 Resource Server валидация:</h3>
     * <pre>
     * Клиент → Bearer eyJhbGciOiJSUzI1NiJ9... → @PreAuthorize("hasAuthority('SCOPE_read')")
     * ↓
     * JwtAuthenticationProvider:
     * 1. jwtDecoder.decode(token) → Jwt (claims + подпись)
     * 2. Проверить iss="self", exp>now
     * 3. sub=clientId, scp="read write" → authorities
     * 4. Authentication в SecurityContext
     * </pre>
     *
     * <h3>Обработка ошибок GlobalExceptionHandler:</h3>
     * <table border="1">
     *   <tr><th>Ошибка</th><th>HTTP</th><th>JSON</th></tr>
     *   <tr><td>Invalid Client ID</td><td>401</td><td>{"error": "Invalid Client ID"}</td></tr>
     *   <tr><td>Invalid Client Secret</td><td>401</td><td>{"error": "Invalid Client Secret"}</td></tr>
     * </table>
     *
     * <p><b>Безопасность:</b>
     * <ul>
     *   <li>clientSecret в БД — BCrypt хэш (не plain text);</li>
     *   <li>JWT подписан (RSA/EC) — нельзя подделать;</li>
     *   <li>Ограниченный TTL (1 час) — минимизирует ущерб при компрометации;</li>
     *   <li>scopes из БД — динамический контроль прав приложения.</li>
     * </ul>
     * </p>
     *
     * <p><b>Тестирование curl:</b>
     * <pre>
     * curl -X POST http://localhost:8080/oauth2/token \
     *   -u app123:secret456 \
     *   -d "grant_type=client_credentials"
     * </pre>
     * </p>
     *
     * @param clientId идентификатор OAuth2 приложения (из БД)
     * @param clientSecret секрет приложения в plain text (проверяется против BCrypt хэша)
     * @return JWT Bearer токен (compact JWS string) на 1 час с claims
     * @throws BadCredentialsException при неверном client_id или client_secret (HTTP 401)
     */
    public String login(String clientId, String clientSecret) {
        Application application = developerRepository.findByClientId(clientId)
                .orElseThrow(() -> new BadCredentialsException("Invalid Client ID"));

        if (!passwordEncoder.matches(clientSecret, application.getClientSecret())) {
            throw new BadCredentialsException("Invalid Client Secret");
        }

        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(3600);

        String scope = String.join(" ", application.getScopes());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(now)
                .expiresAt(expiry)
                .subject(application.getClientId())
                .claim("scp", scope)
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}