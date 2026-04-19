package rf.mizuka.web.application.database.rest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rf.mizuka.web.application.models.rest.Application;
import rf.mizuka.web.application.models.user.User;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA репозиторий для CRUD операций с сущностью {@link Application}.
 *
 * <p>Помечен аннотацией {@link org.springframework.stereotype.Repository @Repository},
 * что делает его Spring-управляемым компонентом. Автоматически регистрируется
 * в ApplicationContext и доступен для инъекции через конструктор (@Autowired).</p>
 *
 * <p><b>Наследование:</b> {@link org.springframework.data.jpa.repository.JpaRepository JpaRepository}&lt;Application, Long&gt;
 * предоставляет стандартные методы бесплатно:
 * <ul>
 *   <li>{@code save(Application)}, {@code saveAll(List)} — INSERT/UPDATE;</li>
 *   <li>{@code findById(Long)}, {@code findAll()} — SELECT;</li>
 *   <li>{@code deleteById(Long)}, {@code existsById(Long)} — DELETE;</li>
 *   <li>Pagination, sorting, batch operations.</li>
 * </ul>
 * </p>
 *
 * <h3>Кастомные методы репозитория:</h3>
 *
 * <h4>{@code Optional&lt;Application&gt; findByClientId(String clientId)}</h4>
 * <p>Spring Data JPA парсит имя метода → генерирует JPQL автоматически:
 * <pre>
 * SELECT a FROM Application a WHERE a.clientId = ?1
 * </pre>
 * Возвращает {@link Optional} — пустой при отсутствии приложения с данным clientId.
 * Используется {@link rf.mizuka.web.application.services.rest.AuthApiService#login(String, String)}.</p>
 *
 * <h4>{@code @Query("SELECT d FROM Application d LEFT JOIN FETCH d.scopes WHERE d.owner = :owner")}</h4>
 * <p>Ручной JPQL запрос с параметром:
 * <ul>
 *   <li>{@code LEFT JOIN FETCH d.scopes} — EAGER загрузка @ElementCollection (избегает N+1);</li>
 *   <li>{@code WHERE d.owner = :owner} — фильтр по владельцу;</li>
 *   <li>{@code @Param("owner")} — именованный параметр.</li>
 * </ul>
 *
 * <h3>Hibernate SQL под капотом:</h3>
 * <pre>
 * -- findByClientId("app123")
 * SELECT * FROM applications WHERE client_id = ?
 *
 * -- findAllByOwner(user)
 * SELECT a.*, s.scopes
 * FROM applications a
 * LEFT JOIN applications_scopes s ON a.id = s.application_id
 * WHERE a.owner_id = ?
 * </pre>
 *
 * <h3>Транзакционность (по умолчанию):</h3>
 * <ul>
 *   <li>Все методы в {@link org.springframework.transaction.annotation.Transactional @Transactional(readOnly = true)};</li>
 *   <li>{@code save()} — {@code @Transactional(propagation = REQUIRED)};</li>
 *   <li>Автоматический rollback при исключениях БД.</li>
 * </ul>
 *
 *
 * <p><b>Производительность:</b>
 * <ul>
 *   <li>Обязателен индекс {@code CREATE INDEX ON applications(client_id)};</li>
 *   <li>{@code findAllByOwner()} оптимизирован JOIN FETCH — нет N+1 проблемы.</li>
 * </ul>
 * </p>
 */
@Repository
public interface DeveloperRepository
        extends JpaRepository<Application, Long>
{
    /**
     * Поиск приложения по уникальному идентификатору clientId.
     *
     * <p><b>Spring Data JPA Query Method:</b> Автоматический парсинг имени метода → JPQL:
     * <pre>
     * SELECT a FROM Application a WHERE a.clientId = ?1
     * </pre>
     * Hibernate SQL: {@code SELECT * FROM applications WHERE client_id = ? LIMIT 1}.</p>
     *
     * <h3>Возврат:</h3>
     * <ul>
     *   <li>Найдено → {@link Optional#of(Object)};</li>
     *   <li>Не найдено → {@link Optional#empty()}.</li>
     * </ul>
     *
     * <h3>Использование в AuthApiService:</h3>
     * <pre>
     * public String login(String clientId, String clientSecret) {
     *   Application app = developerRepository
     *     .findByClientId(clientId)     ← Этот метод
     *     .orElseThrow(() → new BadCredentialsException("Invalid Client ID"));
     *   // Дальше проверка clientSecret...
     * }
     * </pre>
     *
     * <h3>Производительность:</h3>
     * <ul>
     *   <li>Ожидается индекс {@code CREATE INDEX idx_applications_client_id ON applications(client_id)};</li>
     *   <li>Один SELECT с LIMIT 1 — оптимально для аутентификации.</li>
     * </ul>
     *
     * <p><b>Транзакционность:</b> {@code @Transactional(readOnly = true)} — только чтение,
     * Hibernate использует кэш первого уровня при повторных вызовах.</p>
     *
     * @param clientId уникальный идентификатор приложения
     * @return Optional с Application или пустой при отсутствии
     */
    Optional<Application> findByClientId(String clientId);
    /**
     * Получение всех приложений конкретного владельца с scopes (оптимизированный запрос).
     *
     * <p><b>JPQL запрос:</b>
     * <pre>
     * SELECT d FROM Application d
     * LEFT JOIN FETCH d.scopes
     * WHERE d.owner = :owner
     * </pre>
     * </p>
     *
     * <h3>Ключевые особенности запроса:</h3>
     * <ul>
     *   <li>{@code LEFT JOIN FETCH d.scopes} — <b>критично важно!</b> Загружает @ElementCollection
     *      в одном SQL запросе, избегает N+1 проблемы;</li>
     *   <li>{@code d.owner = :owner} — фильтр по внешнему ключу owner_id;</li>
     *   <li>{@code @Param("owner")} — именованный параметр (Spring Data).</li>
     * </ul>
     *
     * <h3>Hibernate генерируемый SQL:</h3>
     * <pre>
     * SELECT a.*, s.scopes
     * FROM applications a
     * LEFT JOIN applications_scopes s ON a.id = s.application_id
     * WHERE a.owner_id = ?  ← user.id
     * </pre>
     *
     * <h3>N+1 проблема (БЕЗ FETCH):</h3>
     * <pre>
     * НЕПРАВИЛЬНО: SELECT * FROM applications WHERE owner_id = ?
     *    → 1 SQL (список приложений)
     *    → N SQL (по 1 для scopes каждого app)
     *
     * ПРАВИЛЬНО: 1 SQL загружает ВСЁ сразу
     * </pre>
     *
     * @param owner владелец приложения ({@link User})
     * @return список всех приложений владельца с загруженными scopes
     */
    @Query("SELECT d FROM Application d LEFT JOIN FETCH d.scopes WHERE d.owner = :owner")
    List<Application> findAllByOwner(@Param("owner") User owner);
}
