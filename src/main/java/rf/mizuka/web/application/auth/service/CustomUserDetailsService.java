package rf.mizuka.web.application.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rf.mizuka.web.application.auth.database.repository.UserRepository;

/**
 * Обязательная реализация UserDetailsService для Spring Security DAO-аутентификации из БД.
 *
 * <p>Класс помечен {@link org.springframework.stereotype.Service @Service} и реализует
 * {@link org.springframework.security.core.userdetails.UserDetailsService} — центральный интерфейс Spring Security,
 * который {@link org.springframework.security.authentication.dao.DaoAuthenticationProvider} вызывает при каждом логине
 * для загрузки данных пользователя из источника (БД, LDAP, etc.).</p>
 *
 * <p><b>Критическая роль в потоке аутентификации:</b>
 * <pre>
 * POST /auth/login → UsernamePasswordAuthenticationFilter
 * ↓
 * DaoAuthenticationProvider.authenticate()
 * ↓
 * userDetailsService.loadUserByUsername(username) ← ЭТОТ КЛАСС
 * ↓
 * User из БД → UserDetails (с authorities, enabled, etc.)
 * ↓
 * passwordEncoder.matches(rawPassword, userDetails.getPassword())
 * ↓
 * Authentication успех → SecurityContext
 * </pre>
 * </p>
 *
 * <h3>Почему обязателен для БД аутентификации</h3>
 * <p>Spring Security по умолчанию использует {@code InMemoryUserDetailsManager}. Для JPA/БД требуется
 * кастомная реализация, которая:
 * <ul>
 *   <li>Выполняет {@code userRepository.findByUsername(username)} — SELECT из БД</li>
 *   <li>Возвращает {@link org.springframework.security.core.userdetails.UserDetails} с полями:
 *     <ul>
 *       <li>{@code getUsername()} — для сравнения</li>
 *       <li>{@code getPassword()} — BCrypt хэш для проверки</li>
 *       <li>{@code getAuthorities()} — роли/права</li>
 *       <li>{@code isEnabled(), isAccountNonExpired(), etc.} — статус аккаунта</li>
 *     </ul>
 *   </li>
 *   <li>Выбрасывает {@link org.springframework.security.core.userdetails.UsernameNotFoundException}
 *       при отсутствии пользователя → логин проваливается с 401</li>
 * </ul>
 * </p>
 *
 * <h3>Конструкторная инъекция (рекомендуемая практика)</h3>
 * <p>{@code @Autowired public CustomUserDetailsService(UserRepository userRepository)}:
 * <ul>
 *   <li>{@code final UserRepository} — иммутабельность + null-safety</li>
 *   <li>Spring проваливается при запуске при отсутствии бина (не в runtime)</li>
 *   <li>Простота unit-тестов: {@code new CustomUserDetailsService(mockRepository)}</li>
 * </ul>
 * </p>
 *
 * <h3>Транзакционность оптимизирована</h3>
 * <p>Метод {@code loadUserByUsername()} помечен {@link org.springframework.transaction.annotation.Transactional @Transactional(readOnly = true)}:
 * <ul>
 *   <li><b>Только чтение</b> — Hibernate отключает dirty checking (flush)</li>
 *   <li>Автоматическая транзакция для {@code findByUsername()} (изоляция READ_COMMITTED)</li>
 *   <li>Оптимизация производительности (нет write lock)</li>
 * </ul>
 * </p>
 *
 * <p><b>Регистрация → Логин цепочка:</b><br/>
 * 1. {@code UserService.registerUser()} сохраняет User с BCrypt паролем<br/>
 * 2. {@code CustomUserDetailsService.loadUserByUsername()} находит его<br/>
 * 3. {@code DaoAuthenticationProvider} проверяет пароль → успех
 * </p>
 *
 * @see org.springframework.security.authentication.dao.DaoAuthenticationProvider
 * @see org.springframework.security.core.userdetails.UserDetailsService#loadUserByUsername(String)
 */
@Service
public class CustomUserDetailsService
        implements UserDetailsService
{
    /**
     * Spring Data JPA репозиторий, инжектируемый через конструкторную инъекцию.
     *
     * <p>Объявлено {@code final private} для иммутабельности — стандартная практика Spring:
     * <ul>
     *   <li>Инициализируется только в конструкторе</li>
     *   <li>Null-safety: Spring проваливается при запуске при отсутствии бина</li>
     *   <li>Простота тестирования: {@code new CustomUserDetailsService(mock(UserRepository.class))}</li>
     * </ul>
     *
     * <p>Предоставляет метод {@code findByUsername(String)} — генерируется Spring Data JPA:
     * {@code SELECT * FROM users WHERE username = ?1} → {@code Optional<User>}</p>
     */
    private final UserRepository userRepository;
    /**
     * Конструктор с конструкторной инъекцией (рекомендуемая практика Spring Boot 2.6+).
     *
     * <p>{@link org.springframework.beans.factory.annotation.Autowired @Autowired} на конструкторе
     * автоматически добавляется Spring при наличии одного конструктора (implicit constructor injection).
     *
     * <p>Преимущества над полевой инъекцией {@code @Autowired private UserRepository}:
     * <ul>
     *   <li>Иммутабельность через {@code final}</li>
     *   <li>Обязательная зависимость (компилятор проверит)</li>
     *   <li>Простота mock'ов в тестах</li>
     *   <li>Fail-fast при запуске (не в runtime)</li>
     * </ul>
     */
    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    /**
     * Обязательный метод Spring Security {@link UserDetailsService}, вызываемый при каждом логине.
     *
     * <p>{@link org.springframework.transaction.annotation.Transactional @Transactional(readOnly = true)}
     * создает транзакцию только для чтения:
     * <ul>
     *   <li>Hibernate отключает dirty checking (нет auto-flush)</li>
     *   <li>Изоляция READ_COMMITTED для {@code findByUsername()}</li>
     *   <li>Оптимизация производительности (нет write locks)</li>
     * </ul>
     *
     * <p><b>Алгоритм (вызывается DaoAuthenticationProvider):</b>
     * <ol>
     *   <li>{@code userRepository.findByUsername(username)} → {@code Optional<User>}</li>
     *   <li>{@code Optional.orElseThrow()} — если пустой → {@link UsernameNotFoundException}<br/>
     *       <small>Логин проваливается → HTTP 401 Unauthorized</small></li>
     *   <li>Возвращает {@link UserDetails} (User реализует интерфейс) с:<br/>
     *       {@code username, password (BCrypt), authorities, enabled, accountNonExpired, etc.}
     *   </li>
     * </ol>
     * </p>
     *
     * <p><b>Полный поток аутентификации:</b>
     * <pre>
     * POST /auth/login → UsernamePasswordAuthenticationFilter
     * ↓
     * DaoAuthenticationProvider.authenticate()
     * ↓ loadUserByUsername(username) ← ЭТОТ МЕТОД
     * ↓ User из БД → UserDetails
     * ↓ passwordEncoder.matches(rawPassword, userDetails.getPassword())
     * ↓ Authentication успех → SecurityContext
     * </pre>
     * </p>
     *
     * @param username логин из формы логина (principal)
     * @return UserDetails для проверки пароля и authorities
     * @throws UsernameNotFoundException если пользователь не найден → логин проваливается
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
