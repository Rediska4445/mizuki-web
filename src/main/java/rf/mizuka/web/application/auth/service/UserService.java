package rf.mizuka.web.application.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rf.mizuka.web.application.auth.database.repository.UserRepository;
import rf.mizuka.web.application.auth.models.User;

/**
 * Сервис для регистрации новых пользователей, интегрированный с Spring Security аутентификацией.
 *
 * <p>Класс помечен аннотацией {@link org.springframework.stereotype.Service @Service}, что делает его
 * Spring-управляемым компонентом сервисного слоя. Автоматически регистрируется в контексте приложений
 * и доступен для инъекции через {@link org.springframework.beans.factory.annotation.Autowired @Autowired}
 * в контроллерах и других сервисах.</p>
 *
 * <p><b>Основная роль в Spring Security:</b> Выполняет <i>регистрацию аккаунта</i> (создание записи в БД),
 * создавая пользователя в формате, совместимом с {@link org.springframework.security.core.userdetails.UserDetailsService}.
 * Зарегистрированный пользователь становится доступен для аутентификации через
 * {@link org.springframework.security.authentication.dao.DaoAuthenticationProvider}.</p>
 *
 * <h3>Зависимости Spring Security</h3>
 * <ul>
 *   <li>{@link org.springframework.security.crypto.password.PasswordEncoder @Autowired PasswordEncoder} —
 *       стандартный компонент Spring Security для безопасного хеширования паролей.
 *       <ul>
 *         <li>Автоматически инжектируется из конфигурации (BCryptPasswordEncoder)</li>
 *         <li>{@code passwordEncoder.encode(rawPassword)} генерирует хэш с salt'ом и work factor</li>
 *         <li>Хэш сохраняется в поле {@code User.password} для последующей проверки при логине</li>
 *       </ul>
 *   </li>
 *   <li>{@link rf.mizuka.web.application.auth.database.repository.UserRepository @Autowired UserRepository} —
 *       Spring Data JPA репозиторий для работы с сущностью {@link rf.mizuka.web.application.auth.models.User}.
 *       Предоставляет методы {@code existsByUsername()} и {@code save()} для CRUD операций.
 *   </li>
 * </ul>
 *
 * <h3>Транзакционность</h3>
 * <p>Аннотация {@link org.springframework.transaction.annotation.Transactional @Transactional} на классе
 * применяет декларативное управление транзакциями Spring:
 * <ul>
 *   <li>Каждый публичный метод автоматически оборачивается в транзакцию</li>
 *   <li>При {@code userRepository.save()} — атомарная запись в БД</li>
 *   <li>При исключении (например, {@code IllegalArgumentException}) — автоматический rollback</li>
 *   <li>READ_COMMITTED изоляция по умолчанию (конфигурируется через {@code @Transactional(readOnly = true)})</li>
 * </ul>
 * </p>
 *
 * <h3>Процесс регистрации</h3>
 * <ol>
 *   <li>Проверка уникальности: {@code userRepository.existsByUsername(username)} → исключение при дубликате</li>
 *   <li>Создание {@link rf.mizuka.web.application.auth.models.User}:</li>
 *     <ul>
 *       <li>{@code user.setUsername(username)} — логин пользователя</li>
 *       <li>{@code user.setPassword(passwordEncoder.encode(rawPassword))} — <b>BCrypt хэш</b> пароля</li>
 *     </ul>
 *   <li>Сохранение в БД: {@code userRepository.save(user)} → новая запись с ID</li>
 * </ol>
 *
 * <p><b>Интеграция с Spring Security аутентификацией:</b><br/>
 * После {@code registerUser()} пользователь доступен для {@link org.springframework.security.core.userdetails.UserDetailsService#loadUserByUsername(String)}:
 * <pre>
 * 1. Контроллер /auth/register → UserService.registerUser()
 * 2. User сохраняется в БД с BCrypt паролем
 * 3. Клиент делает POST /auth/login → UsernamePasswordAuthenticationFilter
 * 4. DaoAuthenticationProvider:
 *    a) UserDetailsService.loadUserByUsername() → User из БД
 *    b) passwordEncoder.matches(rawPassword, storedBCryptHash) → true ✓
 *    c) Authentication успех → SecurityContext
 * </pre>
 * </p>
 *
 * <p><b>Важные особенности Spring Security совместимости:</b>
 * <ul>
 *   <li>Пароль <b>всегда</b> хешируется через {@link PasswordEncoder} — никогда не сохраняется в plain text</li>
 *   <li>Поле {@code User.username} должно точно соответствовать параметру {@code principal} в Authentication</li>
 *   <li>После сохранения пользователь сразу готов к аутентификации (без дополнительных ролей/прав)</li>
 *   <li>{@code IllegalArgumentException} обрабатывается контроллером как HTTP 400 Bad Request</li>
 * </ul>
 * </p>
 *
 * @see org.springframework.security.crypto.password.PasswordEncoder
 * @see org.springframework.security.authentication.dao.DaoAuthenticationProvider
 * @see org.springframework.security.core.userdetails.UserDetailsService
 */
@Service
public class UserService {
    /**
     * Spring Data JPA репозиторий для операций CRUD с сущностью {@link User} при регистрации пользователей.
     *
     * <p>Автоматически инжектируется через {@link org.springframework.beans.factory.annotation.Autowired @Autowired}
     * в сервисах Spring. Spring Data JPA генерирует реализацию интерфейса {@link org.springframework.data.jpa.repository.JpaRepository}
     * с методами:
     * <ul>
     *   <li>{@code existsByUsername(String)} — проверяет существование пользователя по логину В БД</li>
     *   <li>{@code save(User)} — сохраняет/обновляет сущность {@link User} (генерирует INSERT/UPDATE SQL)</li>
     * </ul>
     * </p>
     *
     * <p><b>Критическая роль в Spring Security регистрации:</b><br/>
     * Выполняет проверку уникальности ДО создания записи:
     * <pre>
     * if (userRepository.existsByUsername(username)) // ← БД запрос SELECT
     *     throw new IllegalArgumentException("User already exists");
     * userRepository.save(user); // ← БД INSERT только для уникального username
     * </pre>
     * Предотвращает race conditions при параллельных регистрациях.</p>
     *
     * <p>Работает в рамках {@link org.springframework.transaction.annotation.Transactional @Transactional}:
     * <ul>
     *   <li>Атомарная проверка + сохранение (либо всё, либо ничего)</li>
     *   <li>Автоматический rollback при {@code IllegalArgumentException}</li>
     * </ul>
     * </p>
     */
    @Autowired
    private UserRepository userRepository;
    /**
     * BCrypt шифровальщик паролей Spring Security для безопасного хранения.
     *
     * <p>Инжектируется через {@link org.springframework.beans.factory.annotation.Autowired @Autowired}
     * из бина конфигурации (обычно {@code @Qualifier("bCryptPasswordEncoder")}).</p>
     *
     * <p><b>Обязательный компонент Spring Security аутентификации:</b>
     * <table border="1">
     *   <tr><th>Этапы</th><th>Метод</th><th>Назначение</th></tr>
     *   <tr><td>Регистрация</td><td>{@code passwordEncoder.encode(rawPassword)}</td><td>Хеширует пароль + salt → сохраняется в БД</td></tr>
     *   <tr><td>Логин</td><td>{@code passwordEncoder.matches(rawInput, storedHash)}</td><td>Проверяет введённый пароль против хэша из БД</td></tr>
     * </table>
     * </p>
     *
     * <p><b>Регистрация ({@code registerUser}):</b><br/>
     * {@code user.setPassword(passwordEncoder.encode(rawPassword))} — **НИКОГДА** plain text не сохраняется в БД.
     * BCrypt генерирует: {@code $2a$10$...} (algorithm + strength + salt + hash).</p>
     *
     * <p><b>DaoAuthenticationProvider при логине:</b><br/>
     * 1. {@code UserDetailsService.loadUserByUsername()} → User из БД с BCrypt хэшем<br/>
     * 2. {@code passwordEncoder.matches(submittedPassword, user.getPassword())} → true/false<br/>
     * 3. ✓ Authentication успех → SecurityContext
     * </p>
     *
     * <p><b>Почему @Autowired, а не @Bean прямо в сервисе:</b>
     * <ul>
     *   <li>Единый бин для всего приложения (конфигурация BCrypt strength в одном месте)</li>
     *   <li>Совместимость с {@link org.springframework.security.authentication.dao.DaoAuthenticationProvider}</li>
     * </ul>
     * </p>
     */
    @Autowired
    private PasswordEncoder passwordEncoder;
    /**
     * Регистрирует нового пользователя в системе, создавая запись в БД в формате Spring Security.
     *
     * <p>Аннотация {@link org.springframework.transaction.annotation.Transactional @Transactional} на методе
     * (наследуется от класса) активирует декларативное управление транзакциями Spring:
     * <ul>
     *   <li>Автоматически создается транзакция перед выполнением метода</li>
     *   <li>Атомарность: {@code existsByUsername()} + {@code save()} — либо всё, либо rollback</li>
     *   <li>Авто-коммит при успехе (HTTP 200)</li>
     *   <li>Авто-rollback при {@link IllegalArgumentException} (дубликат username)</li>
     *   <li>Изоляция: {@code READ_COMMITTED} (по умолчанию)</li>
     *   <li>Пропагация: {@code REQUIRED} (по умолчанию)</li>
     * </ul>
     * </p>
     *
     * <h3>Полный алгоритм регистрации (Spring Security совместимый):</h3>
     * <ol>
     *   <li>Проверка уникальности: {@code userRepository.existsByUsername(username)}<br/>
     *       Выполняет {@code SELECT COUNT(*) FROM users WHERE username = ?} → true/false</li>
     *   <li>Исключение при дубликате:<br/>
     *       {@code throw new IllegalArgumentException("User already exists: " + username)}<br/>
     *       Контроллер ловит → HTTP 400 Bad Request + сообщение</li>
     *   <li>Создание сущности User:
     *     <ul>
     *       <li>{@code new User()} — чистая JPA сущность</li>
     *       <li>{@code user.setUsername(username)} — логин для Authentication.principal</li>
     *       <li>{@code user.setPassword(passwordEncoder.encode(rawPassword))} — BCrypt хэш<br/>
     *           Формат: {@code $2a$10$NCl...} (alg+cost+salt+hash)
     *       </li>
     *     </ul>
     *   </li>
     *   <li>Сохранение в БД: {@code userRepository.save(user)}<br/>
     *       Выполняет {@code INSERT INTO users (username, password) VALUES(?, ?)} + генерирует ID</li>
     * </ol>
     *
     * <h3>Почему @Transactional критически важен:</h3>
     * <pre>
     * Без транзакции (race condition):
     * Т1: existsByUsername("user") → false
     * Т2: existsByUsername("user") → false
     * Т1: save("user") → OK
     * Т2: save("user") → дубликат! ← БД constraint violation
     *
     * С транзакцией (атомарно):
     * Т1: BEGIN → exists() → new User() → save() → COMMIT
     * Т2: BEGIN → exists() → true → ROLLBACK (IllegalArgumentException)
     * </pre>
     *
     * <h3>Интеграция с Spring Security аутентификацией:</h3>
     * <table border="1">
     *   <tr><th>Действие</th><th>Результат</th><th>Готов к логину</th></tr>
     *   <tr><td>После {@code registerUser("user", "pass123")}</td><td>БД: username="user", password="$2a$10$..."</td><td>Да</td></tr>
     * </table>
     *
     * <p>Немедленный логин работает:<br/>
     * {@code POST /auth/login {username:"user", password:"pass123"}} →<br/>
     * {@code DaoAuthenticationProvider}:
     * <pre>
     * 1. UserDetailsService.loadUserByUsername("user") → User из БД
     * 2. passwordEncoder.matches("pass123", "$2a$10$...") → true
     * 3. Authentication успех → SecurityContextHolder
     * </pre>
     * </p>
     *
     * <p>Обработка исключений контроллером:<br/>
     * {@code @ExceptionHandler(IllegalArgumentException.class)} → HTTP 400 + JSON {@code {"error": "User already exists: user"}}
     * </p>
     *
     * @param username логин пользователя (уникальный, используется в Authentication.principal)
     * @param rawPassword пароль в plain text (автоматически хешируется BCrypt)
     * @throws IllegalArgumentException если пользователь с таким username уже существует
     */
    @Transactional
    public void registerUser(String username, String rawPassword) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("User already exists: " + username);
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));

        userRepository.save(user);
    }
}