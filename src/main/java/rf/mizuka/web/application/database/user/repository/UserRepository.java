package rf.mizuka.web.application.database.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rf.mizuka.web.application.models.user.User;
import rf.mizuka.web.application.services.user.CustomUserDetailsService;
import rf.mizuka.web.application.services.user.UserService;

import java.util.Optional;
/**
 * Spring Data JPA репозиторий для CRUD операций с сущностью {@link User}.
 *
 * <p>Помечен {@link org.springframework.stereotype.Repository @Repository} (необязательно — Spring Data JPA
 * автоматически сканирует интерфейсы, расширяющие {@link JpaRepository}). Предоставляет стандартные методы
 * {@link org.springframework.data.jpa.repository.JpaRepository} + кастомные запросы для Spring Security.</p>
 *
 * <h3>Наследуемые методы JpaRepository&lt;User, Long&gt;</h3>
 * <ul>
 *   <li>{@code save(User)} — INSERT/UPDATE (используется {@link UserService#registerUser(String, String)} ()})</li>
 *   <li>{@code findById(Long)} — SELECT по ID</li>
 *   <li>{@code findAll()} — SELECT всех пользователей</li>
 *   <li>{@code delete(User)} — DELETE</li>
 * </ul>
 *
 * <h3>Критические методы Spring Security аутентификации</h3>
 * <table border="1">
 *   <tr><th>Метод</th><th>Назначение</th><th>Автогенерируемый SQL</th></tr>
 *   <tr><td>{@code findByUsername(String)}</td><td>{@link CustomUserDetailsService#loadUserByUsername(String)} ()}</td><td>{@code SELECT * FROM users WHERE username = ?1}</td></tr>
 *   <tr><td>{@code existsByUsername(String)}</td><td>{@link UserService#registerUser(String, String)} ()} уникальность</td><td>{@code SELECT COUNT(*) > 0 FROM users WHERE username = ?1}</td></tr>
 * </table>
 *
 * <h3>Автогенерация Spring Data JPA</h3>
 * <p>Методы названы по конвенции — Spring Data JPA парсит и генерирует JPQL/HQL:
 * <ul>
 *   <li>{@code findByUsername} → {@code findByProperty("username")}</li>
 *   <li>{@code existsByUsername} → {@code EXISTS (SELECT 1 FROM User u WHERE u.username = ?1)}</li>
 * </ul>
 * Тип возврата {@link java.util.Optional<User>} обрабатывается {@code .orElseThrow()}.
 * </p>
 *
 * <h3>Полный поток использования</h3>
 * <pre>
 * 1. Регистрация: UserService.registerUser()
 *    ↓ existsByUsername("user") → false
 *    ↓ save(new User("user", bcrypt)) → INSERT
 *
 * 2. Логин: CustomUserDetailsService.loadUserByUsername()
 *    ↓ findByUsername("user") → Optional[User]
 *    ↓ User → UserDetails → DaoAuthenticationProvider
 * </pre>
 *
 * <p><b>ID тип Long</b> соответствует {@code @Id private long id} в {@link User}.
 * Интерфейс автоматически реализуется Spring Data JPA proxy во время запуска.</p>
 *
 * @see org.springframework.data.jpa.repository.JpaRepository
 * @see UserService
 * @see CustomUserDetailsService
 */
@Repository
public interface UserRepository
        extends JpaRepository<User, Long>
{
    /**
     * Spring Data JPA запрос для загрузки пользователя по логину — критически важен для аутентификации.
     *
     * <p>Автоматически генерирует JPQL: {@code SELECT u FROM User u WHERE u.username = ?1}.
     * Возвращает {@link java.util.Optional<User>} для безопасной обработки отсутствия пользователя.</p>
     *
     * <p><b>Вызывается {@link CustomUserDetailsService#loadUserByUsername(String)} ()} при каждом логине:</b>
     * <pre>
     * DaoAuthenticationProvider.authenticate():
     * UserDetails user = userDetailsService.loadUserByUsername(formUsername)
     * ↓
     * userRepository.findByUsername(username) ← ЭТОТ МЕТОД
     * ↓
     * Optional[User] → User (implements UserDetails)
     * ↓
     * passwordEncoder.matches(rawPassword, user.getPassword())
     * </pre>
     * </p>
     *
     * <p>Используется в транзакции {@code @Transactional(readOnly = true)} для оптимизации чтения.</p>
     *
     * @param username логин из формы логина (form parameter)
     * @return {@code Optional<User>} — пустой при отсутствии пользователя
     */
    Optional<User> findByUsername(String username);
    /**
     * Spring Data JPA запрос для проверки уникальности username при регистрации.
     *
     * <p>Автоматически генерирует оптимизированный SQL: {@code SELECT COUNT(*) > 0 FROM users WHERE username = ?1}.
     * **Быстрее** чем {@code findByUsername().isPresent()} (нет полной загрузки сущности).</p>
     *
     * <p><b>Вызывается {@link UserService#registerUser(String, String)} ()} ДО сохранения:</b>
     * <pre>
     * if (userRepository.existsByUsername(username)) ← ЭТОТ МЕТОД
     *     throw new IllegalArgumentException("User already exists");
     * userRepository.save(user); // безопасно — дубликатов не будет
     * </pre>
     * </p>
     *
     * <p><b>Критично для предотвращения race conditions в {@code @Transactional}:</b>
     * <pre>
     * Т1: existsByUsername("user") → false → save() → COMMIT
     * Т2: existsByUsername("user") → true  → ROLLBACK
     * </pre>
     * </p>
     *
     * @param username проверяемый логин
     * @return {@code true} если пользователь существует в БД
     */
    boolean existsByUsername(String username);
}
