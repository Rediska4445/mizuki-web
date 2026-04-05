package rf.mizuka.web.application.auth.models;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import rf.mizuka.web.application.auth.service.CustomUserDetailsService;
import rf.mizuka.web.application.auth.service.UserService;

import java.util.Collection;
import java.util.Collections;

/**
 * JPA сущность пользователя БД, реализующая UserDetails для Spring Security аутентификации.
 *
 * <p>Помечена {@link jakarta.persistence.Entity @Entity} и {@link jakarta.persistence.Table @Table(name = "users")},
 * что делает ее JPA-сущностью для маппинга на таблицу БД. Одновременно реализует
 * {@link org.springframework.security.core.userdetails.UserDetails} — **обязательный интерфейс** для
 * {@link org.springframework.security.core.userdetails.UserDetailsService}, возвращающий данные пользователя
 * в формате, понятном Spring Security.</p>
 *
 * <h3>Поля БД + Spring Security</h3>
 * <ul>
 *   <li>{@code @Id @GeneratedValue long id} — первичный ключ автоинкремент</li>
 *   <li>{@code @Column(unique = true, nullable = false) String username} — логин, уникальный индекс БД.
 *       Соответствует {@link UserDetails#getUsername()} для сравнения при логине</li>
 *   <li>{@code @Column(nullable = false) String password} — BCrypt хэш пароля.
 *       Соответствует {@link UserDetails#getPassword()} для {@link org.springframework.security.crypto.password.PasswordEncoder#matches}</li>
 * </ul>
 *
 * <h3>Автоматическая роль Spring Security</h3>
 * <p>{@link UserDetails#getAuthorities()} всегда возвращает {@code Collections.singleton("ROLE_USER")}:
 * <ul>
 *   <li>Автоматически доступны все ресурсы с {@code hasRole("USER")}</li>
 *   <li>Совместимо с {@code .anyRequest().authenticated()} в SecurityFilterChain</li>
 *   <li>Расширяемо: переопределить метод для ролей из БД</li>
 * </ul>
 * </p>
 *
 * <h3>Статусы аккаунта (всегда активен)</h3>
 * <table border="1">
 *   <tr><th>Метод</th><th>Возвращает</th><th>Назначение</th></tr>
 *   <tr><td>{@code isAccountNonExpired()}</td><td>{@code true}</td><td>Аккаунт не истек</td></tr>
 *   <tr><td>{@code isAccountNonLocked()}</td><td>{@code true}</td><td>Аккаунт не заблокирован</td></tr>
 *   <tr><td>{@code isCredentialsNonExpired()}</td><td>{@code true}</td><td>Пароль не истек</td></tr>
 *   <tr><td>{@code isEnabled()}</td><td>{@code true}</td><td>Аккаунт включен</td></tr>
 * </table>
 *
 * <h3>Полный поток User в Spring Security</h3>
 * <pre>
 * UserService.registerUser() → INSERT users(username, password=BCrypt)
 * ↓
 * CustomUserDetailsService.loadUserByUsername()
 * ↓
 * userRepository.findByUsername() → User (this class)
 * ↓
 * return User (implements UserDetails) → DaoAuthenticationProvider
 * ↓
 * passwordEncoder.matches(rawPassword, user.getPassword()) ✓
 * ↓
 * Authentication(principal=User, authorities=[ROLE_USER])
 * </pre>
 *
 * <h3>Fluent setters (builder pattern)</h3>
 * <p>Методы {@code setUsername()}, {@code setPassword()}, {@code setId()} возвращают {@code this}:
 * <pre>
 * User user = new User().setUsername("user").setPassword(bcryptHash);
 * // UserService: userRepository.save(user);
 * </pre>
 * Удобно для цепочек, но в данном коде не используется (прямой вызов {@code setUsername()}).
 * </p>
 *
 * <p><b>Ключ совместимости Spring Security + JPA:</b><br/>
 * User одновременно:
 * <ul>
 *   <li>JPA Entity (persist в БД)</li>
 *   <li>UserDetails (аутентификация)</li>
 * </ul>
 * После {@code userRepository.save()} объект сразу готов для {@code loadUserByUsername()}.</p>
 *
 * @see org.springframework.security.core.userdetails.UserDetails
 * @see org.springframework.security.authentication.dao.DaoAuthenticationProvider
 */
@Entity
@Table(name = "users")
public class User
    implements UserDetails
{
    /**
     * Первичный ключ БД с автоинкрементом для уникальной идентификации пользователя.
     *
     * <p>{@link jakarta.persistence.Id @Id} обозначает первичный ключ JPA сущности.
     * {@link jakarta.persistence.GeneratedValue @GeneratedValue(strategy = GenerationType.IDENTITY)}:
     * <ul>
     *   <li>БД сама генерирует значения (MySQL AUTO_INCREMENT, PostgreSQL SERIAL)</li>
     *   <li>После {@code userRepository.save()} поле {@code id} заполняется автоматически</li>
     *   <li>Не используется Spring Security (только {@code username} как principal)</li>
     * </ul>
     * <p>Тип {@code long} — стандарт для ID (поддерживает >2^31 записей).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    /**
     * Логин пользователя — уникальный идентификатор для Spring Security аутентификации.
     *
     * <p>{@link jakarta.persistence.Column @Column(unique = true, nullable = false)}:
     * <ul>
     *   <li>{@code unique = true} — БД создает UNIQUE INDEX (предотвращает дубликаты)</li>
     *   <li>{@code nullable = false} — NOT NULL constraint</li>
     *   <li>Соответствует {@link org.springframework.security.core.userdetails.UserDetails#getUsername()}
     *       для сравнения в {@code loadUserByUsername(username)}</li>
     * </ul>
     *
     * <p><b>Критическая роль:</b> {@code DaoAuthenticationProvider} сравнивает:
     * <pre>
     * UserDetails user = userDetailsService.loadUserByUsername(formUsername);
     * if (!user.getUsername().equals(formUsername)) → провал
     * </pre>
     */
    @Column(unique = true, nullable = false)
    private String username;
    /**
     * BCrypt хэш пароля для проверки Spring Security аутентификации.
     *
     * <p>{@link jakarta.persistence.Column @Column(nullable = false)}:
     * <ul>
     *   <li>{@code nullable = false} — NOT NULL constraint</li>
     *   <li>Содержит результат {@code passwordEncoder.encode(rawPassword)}:
     *       <br>Формат: {@code $2a$10$NCl4jK8...} (alg+cost+salt+hash)</li>
     *   <li>Соответствует {@link org.springframework.security.core.userdetails.UserDetails#getPassword()}
     *       для {@code passwordEncoder.matches(rawInput, user.getPassword())}</li>
     * </ul>
     *
     * <p><b>ЖИЗНЕННО ВАЖНО:</b> Содержит **только** BCrypt хэш, никогда plain text.</p>
     */
    @Column(nullable = false)
    private String password;
    /**
     * Конструктор по умолчанию JPA — обязательный для Hibernate/JPA провайдеров.
     *
     * <p>Пустой конструктор {@code public User() {}} **обязателен** для JPA:
     * <ul>
     *   <li>Hibernate использует reflection для создания экземпляров при {@code findByUsername()}</li>
     *   <li>Заполняет поля через setters или direct field access</li>
     *   <li>Без public no-arg конструктора → {@code org.hibernate.InstantiationException}</li>
     * </ul>
     *
     * <p>Используется внутренне Spring Data JPA:
     * <pre>
     * userRepository.findByUsername("user") →
     * Hibernate: SELECT * FROM users WHERE username = ?
     * ↓
     * new User() → user.setId(1).setUsername("user").setPassword("$2a$10$...")
     * ↓
     * Optional<User>
     * </pre>
     */
    public User() {}
    /**
     * Конструктор для удобного создания User (в основном для тестов).
     *
     * <p>Инициализирует {@code username} и {@code password} напрямую:
     * <ul>
     *   <li>{@code username} — plain text или уже хешированный (зависит от вызывающего)</li>
     *   <li>{@code password} — ожидается BCrypt хэш от {@code passwordEncoder.encode()}</li>
     *   <li>{@code id} остается 0 (генерируется БД при {@code save()})</li>
     * </ul>
     *
     * <p><b>НЕ используется в продакшене:</b> {@link UserService#registerUser(String, String)} ()} создает через
     * {@code new User().setUsername().setPassword()}. Конструктор полезен для:
     * <pre>
     * // Unit тесты
     * User testUser = new User("test", "$2a$10$testHash");
     * when(userRepository.findByUsername("test")).thenReturn(Optional.of(testUser));
     *
     * // Mock данные
     * User admin = new User("admin", bcryptHash);
     * </pre>
     */
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }
    /**
     * Стандартный JPA геттер для первичного ключа — не используется Spring Security.
     *
     * <p>Возвращает автоинкрементный {@code id} из БД, заполняемый после {@code userRepository.save()}.
     * Используется только для бизнес-логики (поиск по ID, связи с другими таблицами).
     *
     * <p>Spring Security игнорирует {@code id} — аутентификация работает только с {@code username}.
     */
    public long getId() {
        return id;
    }
    /**
     * Fluent setter для ID — устанавливает первичный ключ и возвращает this (builder pattern).
     *
     * <p>Вызывается Hibernate при загрузке из БД:
     * <pre>
     * new User() → user.setId(1) → user.setUsername("user") → user.setPassword(bcrypt)
     * </pre>
     *
     * <p>В продакшене используется только JPA/Hibernate. Ручное установление ID не требуется
     * ({@link GenerationType#IDENTITY IDENTITY} генерирует автоматически).
     */
    public User setId(long id) {
        this.id = id;
        return this;
    }
    /**
     * {@link UserDetails#getUsername()} — критически важен для Spring Security аутентификации.
     *
     * <p>Возвращает {@code username} для сравнения в {@link CustomUserDetailsService#loadUserByUsername(String)} ()}:
     * <pre>
     * DaoAuthenticationProvider:
     * UserDetails user = userDetailsService.loadUserByUsername(formUsername)
     * if (!user.getUsername().equals(formUsername)) → AuthenticationException
     * </pre>
     *
     * <p>Служит {@code Authentication.principal} после успешного логина.
     */
    @Override
    public String getUsername() {
        return username;
    }
    /**
     * {@link UserDetails#getPassword()} — возвращает BCrypt хэш для проверки пароля.
     *
     * <p>Используется {@link org.springframework.security.crypto.password.PasswordEncoder#matches()}:
     * <pre>
     * DaoAuthenticationProvider:
     * if (!passwordEncoder.matches(rawPassword, userDetails.getPassword())) → провал
     * </pre>
     *
     * <p>Содержит хэш формата {@code $2a$10$NCl4jK8...} от {@code UserService#registerUser()}.
     * НИКОГДА не plain text.
     */
    @Override
    public String getPassword() {
        return password;
    }
    /**
     * {@link UserDetails#isAccountNonExpired()} — аккаунт не истек (всегда активен).
     *
     * <p>Проверяет срок действия аккаунта. Spring Security блокирует логин если {@code false}.
     * Возврат {@code true} означает: аккаунт бессрочный, нет автоматической деактивации.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    /**
     * {@link UserDetails#isAccountNonLocked()} — аккаунт не заблокирован (всегда доступен).
     *
     * <p>Проверяет блокировку (админ, brute-force). Spring Security блокирует логин если {@code false}.
     * Возврат {@code true} означает: нет механизма блокировки учетной записи.
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    /**
     * {@link UserDetails#isCredentialsNonExpired()} — пароль не истек (всегда действителен).
     *
     * <p>Проверяет срок действия пароля (корпоративная политика). Spring Security блокирует логин если {@code false}.
     * Возврат {@code true} означает: пароли не требуют периодической смены.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    /**
     * {@link UserDetails#isEnabled()} — аккаунт включен (всегда активен).
     *
     * <p>Финальная проверка активности пользователя. Spring Security блокирует логин если {@code false}.
     * Возврат {@code true} означает: все зарегистрированные пользователи сразу активны.
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
    /**
     * {@link UserDetails#getAuthorities()} — возвращает роли Spring Security для авторизации.
     *
     * <p>Все пользователи автоматически получают роль {@code "ROLE_USER"}:
     * <ul>
     *   <li>{@link org.springframework.security.core.authority.SimpleGrantedAuthority SimpleGrantedAuthority}</li>
     *   <li>{@code Collections.singleton()} — неизменяемая коллекция из одной роли</li>
     *   <li>Совместимо с {@code .hasRole("USER") } и {@code .anyRequest().authenticated()}}</li>
     * </ul>
     *
     * <p>Spring Security использует authorities для FilterSecurityInterceptor:
     * <pre>
     * .authorizeHttpRequests(auth -> auth
     *   .requestMatchers("/admin/**").hasRole("ADMIN")
     *   .anyRequest().hasRole("USER") // ← Эта роль
     * )
     * </pre>
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));
    }
    /**
     * Fluent setter для username — устанавливает логин и возвращает this (builder pattern).
     *
     * <p>Вызывается:
     * <ul>
     *   <li>Hibernate при загрузке из БД (field access)</li>
     *   <li>{@link UserService#registerUser(String, String)} ()} напрямую</li>
     * </ul>
     *
     * <p>Пример цепочки: {@code new User().setUsername("user").setPassword(bcryptHash)}
     */
    public User setUsername(String username) {
        this.username = username;
        return this;
    }
    /**
     * Fluent setter для пароля — устанавливает BCrypt хэш и возвращает this.
     *
     * <p>Вызывается {@link UserService#registerUser(String, String)} ()} после:
     * <pre>
     * user.setPassword(passwordEncoder.encode(rawPassword)) // ← BCrypt хэш
     * </pre>
     *
     * <p>Hibernate использует при {@code userRepository.findByUsername()} для заполнения поля.
     */
    public User setPassword(String password) {
        this.password = password;
        return this;
    }
}
