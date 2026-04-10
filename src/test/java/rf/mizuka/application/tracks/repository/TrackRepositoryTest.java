package rf.mizuka.application.tracks.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import rf.mizuka.web.application.database.tracks.repository.TrackRepository;
import rf.mizuka.web.application.models.tracks.Track;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты для {@link TrackRepository}, проверяющие базовые CRUD-операции
 * через Spring Data JPA с использованием встроенного H2 (in-memory) вместо production БД.
 *
 * <p>@DataJpaTest автоматически:
 * <ul>
 *   <li>поднимает контекст Spring Boot только с JPA-компонентами (репозитории, сущности);</li>
 *   <li>настраивает H2 in-memory БД с автоматической очисткой между тестами;</li>
 *   <li>скрывает другие бины (контроллеры, сервисы), фокусируясь на persistence-уровне.</li>
 * </ul>
 * </p>
 *
 * <p>Тесты проверяют реальное взаимодействие с JPA/Hibernate: маппинг сущностей,
 * генерацию IDENTITY-ключей, ограничения NOT NULL и работу стандартных методов
 * {@link org.springframework.data.jpa.repository.JpaRepository}.</p>
 */
@DataJpaTest
public class TrackRepositoryTest {
    /**
     * Менеджер JPA-сущностей для тестирования, автоматически настроенный Spring Boot.
     *
     * <p><b>Что это и зачем нужно:</b> {@code TestEntityManager} — это специальный инструмент
     * Spring Boot для тестирования JPA-репозиториев. Он создаёт <u>полностью изолированную
     * тестовую базу данных в оперативной памяти</u> (H2 in-memory), чтобы тесты:</p>
     *
     * <ul>
     *   <li>НЕ трогали реальную production базу данных (PostgreSQL/MySQL);</li>
     *   <li>работали быстро (in-memory БД в 1000 раз быстрее файловой);</li>
     *   <li>имели полную изоляцию (каждый тест начинается с чистой БД).</li>
     * </ul>
     *
     * <p><b>Как работает механизм:</b></p>
     * <pre>
     * 1. Spring Boot TestContext → создаёт H2 БД в памяти процесса
     * 2. JPA/Hibernate → автоматически создаёт таблицы по вашим @Entity
     * 3. TestEntityManager → управляет persist/flush для тестовых объектов
     * 4. После каждого @Test → полная очистка (TRUNCATE всех таблиц)
     * </pre>
     *
     * <p><b>Основные возможности (используются редко):</b>
     * <ul>
     *   <li>{@code entityManager.persist(track)} — сохранить объект в тестовую БД</li>
     *   <li>{@code entityManager.flush()} — принудительно отправить SQL в БД</li>
     *   <li>{@code entityManager.clear()} — очистить persistence context</li>
     * </ul>
     * В 99% случаев для Spring Data JPA достаточно просто {@code trackRepository.save()}.</p>
     *
     * <p><b>Почему именно H2:</b> H2 полностью совместима с SQL-стандартом и поддерживает
     * все фичи production БД (IDENTITY, NOT NULL, FOREIGN KEY), но живёт только в памяти
     * процесса теста. Логи показывают: {@code jdbc:h2:mem:testdb}.</p>
     *
     * @see <a href="https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing.spring-boot-applications.data.jpa-and-spring-data">Spring Boot JPA Testing</a>
     * @see TestEntityManager
     */
    @Autowired
    private TestEntityManager entityManager;
    /**
     * Тестируемый Spring Data JPA репозиторий для сущности {@link Track}.
     *
     * <p><b>Что это и как создаётся:</b> Spring Boot автоматически генерирует полную
     * реализацию этого интерфейса на основе {@link org.springframework.data.jpa.repository.JpaRepository}.
     * Пишете только интерфейс — Spring создаёт класс с 20+ готовыми методами:
     * {@code save()}, {@code findById()}, {@code findAll()}, {@code delete()}, {@code count()} и др.</p>
     *
     * <p><b>Контекст работы в тестах:</b>
     * <pre>
     * Production: TrackRepository → PostgreSQL/MySQL
     * ════════════════╦═══════════════════════
     * Tests:    TrackRepository → H2 :memory:
     *           ↑
     *       DataJpaTest автоматически
     *       настраивает DataSource + EntityManager
     * </pre></p>
     *
     * <p><b>Автоматическая настройка @DataJpaTest:</b>
     * <ul>
     *   <li>создаёт H2 DataSource вместо production;</li>
     *   <li>сканирует @Entity и создаёт схему БД;</li>
     *   <li>поднимает только JPA-компоненты (НЕ контроллеры/сервисы);</li>
     *   <li>очищает БД после каждого @Test метода.</li>
     * </ul>
     * </p>
     *
     * <p><b>Типизация:</b> {@code JpaRepository<Track, Long>} означает, что:
     * <ul>
     *   <li>ID трека — {@code Long} (согласно @Id в классе Track);</li>
     *   <li>все методы {@code findById(Long)}, {@code deleteById(Long)} используют {@code Long}.</li>
     * </ul>
     * </p>
     *
     * <p><b>Пример SQL, генерируемого для save():</b>
     * <pre>
     * INSERT INTO tracks (name) VALUES ('Test Track')
     * → возвращает GENERATED 1 (IDENTITY ключ)
     * </pre>
     * Этот SQL выполняется против H2 тестовой БД, а НЕ production.</p>
     *
     * @see org.springframework.data.jpa.repository.JpaRepository
     * @see DataJpaTest
     */
    @Autowired
    private TrackRepository trackRepository;

    /**
     * Проверяет корректное сохранение нового трека через Spring Data JPA {@link TrackRepository#save(Object)}.
     *
     * <p><b>Цель теста:</b> Убедиться, что:
     * <ul>
     *   <li>новый {@link Track} с валидным {@code name} успешно сохраняется в тестовую H2 БД;</li>
     *   <li>H2 автоматически генерирует IDENTITY ключ для поля {@code id};</li>
     *   <li>сохранённый объект доступен через {@link TrackRepository#findById(Object)}};</li>
     *   <li>ограничение {@code @Column(nullable = false)} на {@code name} не нарушается.</li>
     * </ul>
     * </p>
     *
     * <p><b>AAA паттерн (Arrange-Act-Assert):</b></p>
     *
     * <pre>{@code
     * ┌──────────────┬──────────────────────────────┐
     * │    GIVEN     │ Подготовка тестовых данных   │
     * ├──────────────┼──────────────────────────────┤
     * │    WHEN      │ Выполнение операции save()   │
     * ├──────────────┼──────────────────────────────┤
     * │    THEN      │ Проверки результата         │
     * └──────────────┴──────────────────────────────┘
     * }</pre>
     *
     * <p><b>ШАГ 1: GIVEN — подготовка данных</b><br/>
     * Создаётся новый объект {@code Track} только с обязательным полем {@code name}.
     * На этом этапе {@code id == null} — объект ещё не сохранён в БД.
     * <pre>SQL не выполняется — только создание Java-объекта в памяти</pre></p>
     *
     * <p><b>ШАГ 2: WHEN — выполнение операции</b><br/>
     * {@code trackRepository.save(newTrack)} вызывает:
     * <pre>
     * 1. JPA/Hibernate проверяет @Column(nullable = false) → OK
     * 2. Генерирует SQL: INSERT INTO tracks (name) VALUES ('Test Track - Save Test')
     * 3. H2 выполняет INSERT → возвращает GENERATED 1 (IDENTITY)
     * 4. Обновляет объект: id = 1L, name = "Test Track - Save Test"
     * </pre>
     * Возвращается managed объект с заполненным {@code id > 0}.</p>
     *
     * <p><b>ШАГ 3: THEN — проверки результата</b><br/>
     * <table>
     *   <tr><th>Проверка</th><th>Что проверяем</th><th>Почему важно</th></tr>
     *   <tr><td>{@code assertNotNull(savedTrack)}</td>
     *       <td>Объект не потерялся</td>
     *       <td>Базовая проверка успешности save()</td></tr>
     *   <tr><td>{@code assertTrue(savedTrack.getId() > 0L)}</td>
     *       <td>IDENTITY ключ сгенерирован</td>
     *       <td>@GeneratedValue(strategy = IDENTITY) работает</td></tr>
     *   <tr><td>{@code assertEquals(name, savedTrack.getName())}</td>
     *       <td>Данные не исказились</td>
     *       <td>JPA маппинг работает корректно</td></tr>
     *   <tr><td>{@code findById(id).orElse(null)}</td>
     *       <td>Объект persisted в БД</td>
     *       <td>Проверяем реальное сохранение, а не кэш</td></tr>
     * </table></p>
     *
     * <p><b>Ожидаемый SQL трафик (в логах при debug):</b>
     * <pre>
     * INSERT INTO tracks (name) VALUES ('Test Track - Save Test') → 1 row inserted
     * SELECT t1_0.id,t1_0.name FROM tracks t1_0 WHERE t1_0.id=? → id=1, name="Test Track"
     * </pre></p>
     *
     * <p><b>В production это будет:</b> тот же код, но против PostgreSQL/MySQL вместо H2.</p>
     */
    @Test
    void shouldSaveTrackWithGeneratedId() {
        // GIVEN — подготовка тестового объекта (id == null)
        Track newTrack = new Track().setName("Test Track - Save Test");

        // WHEN — выполнение операции сохранения
        Track savedTrack = trackRepository.save(newTrack);

        // THEN — проверка результата save()
        assertNotNull(savedTrack);
        assertTrue(savedTrack.getId() > 0L); // IDENTITY сгенерировал ключ
        assertEquals("Test Track - Save Test", savedTrack.getName());

        // ДОПОЛНИТЕЛЬНАЯ ПРОВЕРКА: объект persisted и доступен через findById
        Track foundTrack = trackRepository.findById(savedTrack.getId()).orElse(null);
        assertNotNull(foundTrack);
        assertEquals("Test Track - Save Test", foundTrack.getName());
    }

    /**
     * Проверяет корректную работу {@link TrackRepository#findById(Object)} для существующего трека.
     *
     * <p><b>Цель теста:</b> Убедиться, что:
     * <ul>
     *   <li>{@code findById(id)} возвращает {@code Optional<Track>} с корректными данными;</li>
     *   <li>JPA/Hibernate выполняет SELECT по первичному ключу и маппит результат на объект {@link Track};</li>
     *   <li>сохранённый ранее трек доступен для чтения из тестовой H2 БД.</li>
     * </ul>
     * </p>
     *
     * <p><b>AAA паттерн (Arrange-Act-Assert):</b></p>
     *
     * <pre>{@code
     * ┌──────────────┬──────────────────────────────┐
     * │    GIVEN     │ Сначала сохраняем трек       │
     * ├──────────────┼──────────────────────────────┤
     * │    WHEN      │ Вызываем findById(id)       │
     * ├──────────────┼──────────────────────────────┤
     * │    THEN      │ Optional.present + данные   │
     * └──────────────┴──────────────────────────────┘
     * }</pre>
     *
     * <p><b>ШАГ 1: GIVEN — подготовка данных</b><br/>
     * Сначала сохраняем трек через {@code save()}, получаем сгенерированный {@code id}.
     * <pre>INSERT INTO tracks (name) VALUES ('Findable Track') → id = 1</pre>
     * Этот шаг гарантирует, что в тестовой БД есть запись с известным первичным ключом.</p>
     *
     * <p><b>ШАГ 2: WHEN — выполнение операции</b><br/>
     * {@code trackRepository.findById(savedId)} генерирует SQL:
     * <pre>
     * SELECT t1_0.id, t1_0.name
     * FROM tracks t1_0
     * WHERE t1_0.id = ?  ───┐
     *                       └── параметр: 1
     * </pre>
     * H2 возвращает одну строку → JPA маппит на новый объект {@code Track(id=1, name="...")}
     * → оборачивает в {@code Optional.of(track)}.</p>
     *
     * <p><b>ШАГ 3: THEN — проверки результата</b><br/>
     * <table>
     *   <tr><th>Проверка</th><th>Что проверяем</th><th>Почему важно</th></tr>
     *   <tr><td>{@code assertTrue(found.isPresent())}</td>
     *       <td>Optional содержит трек</td>
     *       <td>БД вернула запись по id</td></tr>
     *   <tr><td>{@code assertEquals(id, found.get().getId())}</td>
     *       <td>Первичный ключ совпадает</td>
     *       <td>@Id маппинг работает</td></tr>
     *   <tr><td>{@code assertEquals(name, found.get().getName())}</td>
     *       <td>Поле name не потерялось</td>
     *       <td>@Column маппинг работает</td></tr>
     * </table></p>
     *
     * <p><b>Ожидаемый SQL трафик (два запроса):</b>
     * <pre>
     * 1 INSERT tracks (name) → id=1
     * 2 SELECT tracks WHERE id=1 → 1 row
     * </pre></p>
     *
     * <p><b>В production:</b> Тот же код, но SELECT против PostgreSQL/MySQL.
     * {@code findById()} — самая частая операция в REST API ({@code GET /tracks/{id}}).</p>
     */
    @Test
    void shouldFindById() {
        // GIVEN — сначала сохраняем трек, получаем сгенерированный id
        Track savedTrack = trackRepository.save(new Track().setName("Findable Track"));
        Long trackId = savedTrack.getId();

        // WHEN — ищем трек по первичному ключу
        Optional<Track> foundTrack = trackRepository.findById(trackId);

        // THEN — проверяем результат findById()
        assertTrue(foundTrack.isPresent(), "Трек должен быть найден по существующему id");
        assertEquals(trackId, foundTrack.get().getId(), "Первичный ключ должен совпадать");
        assertEquals("Findable Track", foundTrack.get().getName(), "Название трека не изменилось");
    }

    /**
     * Проверяет корректное обновление существующего трека через {@link TrackRepository#save(Object)}.
     *
     * <p><b>Цель теста:</b> Убедиться, что:
     * <ul>
     *   <li>{@code save()} для уже сохранённого объекта с изменёнными данными обновляет запись в БД;</li>
     *   <li>JPA/Hibernate отслеживает изменения в persistence context и генерирует UPDATE SQL;</li>
     *   <li>после второго {@code save()} в БД остаётся только последнее состояние объекта.</li>
     * </ul>
     * </p>
     *
     * <p><b>AAA паттерн (Arrange-Act-Assert):</b></p>
     *
     * <pre>{@code
     * ┌──────────────┬──────────────────────────────┐
     * │    GIVEN     │ Сохраняем трек V1 → id=1    │
     * ├──────────────┼──────────────────────────────┤
     * │    WHEN      │ Меняем name → save() снова  │
     * ├──────────────┼──────────────────────────────┤
     * │    THEN      │ БД содержит V2, не V1       │
     * └──────────────┴──────────────────────────────┘
     * }</pre>
     *
     * <p><b>ШАГ 1: GIVEN — подготовка данных</b><br/>
     * Сохраняем трек V1. Получаем {@code id}. На этом этапе в БД:
     * <pre>tracks: id=1, name="Version 1"</pre></p>
     *
     * <p><b>ШАГ 2: WHEN — выполнение обновления</b><br/>
     * Меняем {@code name} на V2 и вызываем {@code save()} снова. JPA/Hibernate:
     * <pre>
     * 1. Детектирует "dirty" объект (name изменилось)
     * 2. Генерирует SQL: UPDATE tracks SET name='Version 2' WHERE id=1
     * 3. H2 выполняет UPDATE → 1 row affected
     * </pre>
     * Важно: Spring Data JPA {@code save()} универсален — INSERT для новых, UPDATE для managed объектов.</p>
     *
     * <p><b>ШАГ 3: THEN — проверки результата</b><br/>
     * <table>
     *   <tr><th>Проверка</th><th>Что проверяем</th><th>Почему важно</th></tr>
     *   <tr><td>{@code assertEquals("Version 2", updated.getName())}</td>
     *       <td>Объект содержит новое имя</td>
     *       <td>Изменения применены к Java-объекту</td></tr>
     *   <tr><td>{@code fresh.getName() == "Version 2"}</td>
     *       <td>БД содержит обновлённые данные</td>
     *       <td>UPDATE SQL выполнился успешно</td></tr>
     *   <tr><td>{@code count() == 1}</td><td>Нет дубликатов</td>
     *       <td>save() обновил, а не создал новую запись</td></tr>
     * </table></p>
     *
     * <p><b>Ожидаемый SQL трафик (два запроса):</b>
     * <pre>
     * 1 INSERT tracks (name='Version 1') → id=1
     * 2 UPDATE tracks SET name='Version 2' WHERE id=1 → 1 row updated
     * 3 SELECT tracks WHERE id=1 → name="Version 2" ✓
     * </pre></p>
     *
     * <p><b>Критическая проверка через reload:</b> После изменения вызываем {@code findById()}
     * для получения "свежего" объекта из БД. Это доказывает, что UPDATE пошёл в БД,
     * а не остался только в памяти объекта.</p>
     *
     * <p><b>В production:</b> REST {@code PUT /tracks/{id}} → findById → setName → save → 200 OK.</p>
     */
    @Test
    void shouldUpdateExistingTrack() {
        // GIVEN — сохраняем трек версии 1, получаем id
        Track trackV1 = trackRepository.save(new Track().setName("Version 1"));
        Long trackId = trackV1.getId();

        // WHEN — обновляем имя на версию 2 и сохраняем снова
        trackV1.setName("Version 2");  // Меняем состояние managed объекта
        Track trackV2 = trackRepository.save(trackV1);

        // THEN — проверяем, что объект обновился
        assertEquals("Version 2", trackV2.getName(), "Имя должно обновиться в объекте");
        assertEquals(trackId, trackV2.getId(), "ID не должен измениться при обновлении");

        // ДОПОЛНИТЕЛЬНАЯ ПРОВЕРКА: загружаем свежий объект из БД
        Track freshFromDb = trackRepository.findById(trackId).orElse(null);
        assertNotNull(freshFromDb, "Трек должен существовать в БД");
        assertEquals("Version 2", freshFromDb.getName(), "БД должна содержать обновлённое имя");
        assertEquals(1L, trackRepository.count(), "Должна быть только 1 запись, а не дубликат");
    }

    /**
     * Проверяет корректное удаление трека по первичному ключу через {@link TrackRepository#deleteById(Object)}}.
     *
     * <p><b>Цель теста:</b> Убедиться, что:
     * <ul>
     *   <li>{@code deleteById(id)} удаляет запись из тестовой H2 БД по первичному ключу;</li>
     *   <li>JPA/Hibernate генерирует DELETE SQL с WHERE id=? и выполняет его;</li>
     *   <li>после удаления {@code existsById(id)}, {@code findById(id)} и {@code count()} отражают пустое состояние.</li>
     * </ul>
     * </p>
     *
     * <p><b>AAA паттерн (Arrange-Act-Assert):</b></p>
     *
     * <pre>{@code
     * ┌──────────────┬──────────────────────────────┐
     * │    GIVEN     │ Трек сохранён → id=1        │
     * ├──────────────┼──────────────────────────────┤
     * │    WHEN      │ Вызываем deleteById(id)     │
     * ├──────────────┼──────────────────────────────┤
     * │    THEN      │ Трек удалён из БД          │
     * └──────────────┴──────────────────────────────┘
     * }</pre>
     *
     * <p><b>ШАГ 1: GIVEN — подготовка данных</b><br/>
     * Сохраняем трек, получаем {@code id}. В БД:
     * <pre>tracks: id=1, name="ToDelete"  ← 1 запись, count()=1</pre></p>
     *
     * <p><b>ШАГ 2: WHEN — выполнение удаления</b><br/>
     * {@code trackRepository.deleteById(trackId)} генерирует SQL:
     * <pre>
     * DELETE FROM tracks WHERE id = ?  ───┐
     *                                    └── параметр: 1
     * → H2: 1 row deleted
     * </pre>
     * Spring Data JPA автоматически вызывает {@code flush()} после DELETE для синхронизации.</p>
     *
     * <p><b>ШАГ 3: THEN — множественная проверка удаления</b><br/>
     * <table>
     *   <tr><th>Проверка</th><th>SQL</th><th>Что проверяем</th><th>Почему важно</th></tr>
     *   <tr><td>{@code !existsById(id)}</td><td>SELECT COUNT(*) WHERE id=?</td>
     *       <td>Быстрое существование</td><td>Оптимизированный запрос</td></tr>
     *   <tr><td>{@code findById(id).isEmpty()}</td><td>SELECT * WHERE id=?</td>
     *       <td>Optional.empty()</td><td>Полный SELECT маппинг</td></tr>
     *   <tr><td>{@code count() == 0}</td><td>SELECT COUNT(*)</td>
     *       <td>Общее количество</td><td>Нет "призрачных" записей</td></tr>
     * </table></p>
     *
     * <p><b>Ожидаемый SQL трафик (3 запроса):</b>
     * <pre>
     * 1 INSERT tracks → id=1
     * 2 DELETE tracks WHERE id=1 → 1 row deleted
     * 3 SELECT COUNT(*) WHERE id=1 → 0
     * 4 SELECT * WHERE id=1 → no rows
     * 5 SELECT COUNT(*) → 0
     * </pre></p>
     *
     * <p><b>Тройная проверка гарантирует:</b>
     * <ul>
     *   <li>DELETE SQL выполнился (existsById=false)</li>
     *   <li>БД чиста (count=0)</li>
     *   <li>Нет кэш-артефактов (findById=empty)</li>
     * </ul>
     * </p>
     *
     * <p><b>В production:</b> REST {@code DELETE /tracks/{id}} → 204 No Content.</p>
     */
    @Test
    void shouldDeleteById() {
        // GIVEN — сохраняем трек для удаления, получаем id
        Track trackToDelete = trackRepository.save(new Track().setName("ToDelete"));
        Long trackId = trackToDelete.getId();
        assertEquals(1L, trackRepository.count(), "Сначала должна быть 1 запись");

        // WHEN — удаляем трек по первичному ключу
        trackRepository.deleteById(trackId);

        // THEN — тройная проверка полного удаления
        assertFalse(trackRepository.existsById(trackId),
                "Трек не должен существовать после deleteById()");

        assertTrue(trackRepository.findById(trackId).isEmpty(),
                "findById() должен вернуть Optional.empty() для удалённого трека");

        assertEquals(0L, trackRepository.count(),
                "База должна быть пустой после удаления единственной записи");
    }

    /**
     * Проверяет корректную работу {@link TrackRepository#findAll()} для получения всех треков.
     *
     * <p><b>Цель теста:</b> Убедиться, что:
     * <ul>
     *   <li>{@code findAll()} возвращает {@code List<Track>} со всеми записями из БД;</li>
     *   <li>JPA/Hibernate выполняет {@code SELECT * FROM tracks} без WHERE;</li>
     *   <li>все сохранённые треки присутствуют в результате в правильном порядке.</li>
     * </ul>
     * </p>
     *
     * <p><b>AAA паттерн (Arrange-Act-Assert):</b></p>
     *
     * <pre>{@code
     * ┌──────────────┬──────────────────────────────┐
     * │    GIVEN     │ Сохраняем N треков → count=N │
     * ├──────────────┼──────────────────────────────┤
     * │    WHEN      │ Вызываем findAll()          │
     * ├──────────────┼──────────────────────────────┤
     * │    THEN      │ Список содержит N треков    │
     * └──────────────┴──────────────────────────────┘
     * }</pre>
     *
     * <p><b>ШАГ 1: GIVEN — подготовка данных</b><br/>
     * Сохраняем несколько треков с разными именами. H2 генерирует последовательные ID:
     * <pre>
     * tracks:
     *  id=1, name="Track Alpha"
     *  id=2, name="Track Beta"
     *  id=3, name="Track Gamma"
     * count() = 3
     * </pre></p>
     *
     * <p><b>ШАГ 2: WHEN — выполнение операции</b><br/>
     * {@code trackRepository.findAll()} генерирует простой SQL:
     * <pre>
     * SELECT t1_0.id, t1_0.name
     * FROM tracks t1_0
     * → H2 возвращает 3 строки → JPA маппит на List<Track>
     * </pre>
     * Spring Data JPA возвращает неизменяемый список с managed объектами.</p>
     *
     * <p><b>ШАГ 3: THEN — множественная проверка результата</b><br/>
     * <table>
     *   <tr><th>Проверка</th><th>Что проверяем</th><th>Почему важно</th></tr>
     *   <tr><td>{@code list.size() == 3}</td><td>Все треки возвращены</td>
     *       <td>Ничего не потерялось</td></tr>
     *   <tr><td>{@code list.contains(track1)}</td><td>Конкретные треки</td>
     *       <td>Маппинг ID→объект работает</td></tr>
     *   <tr><td>{@code list.get(0).getId() == 1}</td><td>Порядок сохранения</td>
     *       <td>H2 IDENTITY сохраняет последовательность</td></tr>
     * </table></p>
     *
     * <p><b>Ожидаемый SQL трафик:</b>
     * <pre>
     * INSERT tracks → id=1,2,3 (3 запроса)
     * SELECT * FROM tracks → 3 rows
     * </pre></p>
     *
     * <p><b>В production:</b> REST {@code GET /tracks} → pagination + sorting для больших списков.</p>
     */
    @Test
    void shouldReturnAllTracks() {
        // GIVEN — сохраняем несколько треков с известными именами
        Track track1 = trackRepository.save(new Track().setName("Track Alpha"));
        Track track2 = trackRepository.save(new Track().setName("Track Beta"));
        Track track3 = trackRepository.save(new Track().setName("Track Gamma"));

        assertEquals(3L, trackRepository.count(), "Должно быть 3 трека перед findAll()");

        // WHEN — получаем все треки
        List<Track> allTracks = trackRepository.findAll();

        // THEN — проверяем полный список
        assertEquals(3, allTracks.size(), "findAll() должен вернуть все 3 трека");
        assertTrue(allTracks.contains(track1), "Первый трек должен быть в списке");
        assertTrue(allTracks.contains(track2), "Второй трек должен быть в списке");
        assertTrue(allTracks.contains(track3), "Третий трек должен быть в списке");

        // Проверяем порядок (H2 возвращает в порядке IDENTITY)
        assertEquals(track1.getId(), allTracks.get(0).getId(), "Первый ID должен быть 1");
        assertEquals("Track Alpha", allTracks.get(0).getName(), "Имена должны совпадать");
    }

    /**
     * Проверяет работу {@link TrackRepository#findAll()} на полностью пустой базе данных.
     *
     * <p><b>Цель теста:</b> Убедиться, что:
     * <ul>
     *   <li>{@code findAll()} корректно работает на пустой БД (0 записей);</li>
     *   <li>JPA/Hibernate возвращает {@code emptyList()} вместо {@code null};</li>
     *   <li>{@code count() == 0} и {@code findAll().isEmpty() == true} консистентны.</li>
     * </ul>
     * </p>
     *
     * <p><b>AAA паттерн (Arrange-Act-Assert):</b></p>
     *
     * <pre>{@code
     * ┌──────────────┬──────────────────────────────┐
     * │    GIVEN     │ Пустая БД (count() = 0)     │
     * ├──────────────┼──────────────────────────────┤
     * │    WHEN      │ Вызываем findAll()          │
     * ├──────────────┼──────────────────────────────┤
     * │    THEN      │ Пустой список, не null      │
     * └──────────────┴──────────────────────────────┘
     * }</pre>
     *
     * <p><b>ШАГ 1: GIVEN — пустое состояние БД</b><br/>
     * <b>Ничего не сохраняем!</b> @DataJpaTest автоматически очищает H2 между тестами,
     * поэтому {@code tracks} пустая: <pre>count() = 0</pre>
     * Это самый чистый edge case — состояние БД "из коробки".</p>
     *
     * <p><b>ШАГ 2: WHEN — выполнение операции</b><br/>
     * {@code trackRepository.findAll()} генерирует:
     * <pre>
     * SELECT t1_0.id, t1_0.name
     * FROM tracks t1_0
     * → H2: 0 rows → Spring Data JPA → Collections.emptyList()
     * </pre></p>
     *
     * <p><b>ШАГ 3: THEN — проверки пустого состояния</b><br/>
     * <table>
     *   <tr><th>Проверка</th><th>Что проверяем</th><th>Почему важно</th></tr>
     *   <tr><td>{@code list.isEmpty()}</td><td>Пустой список</td>
     *       <td>findAll() не вернул null или мусор</td></tr>
     *   <tr><td>{@code list.size() == 0}</td><td>Размер 0</td>
     *       <td>Консистентность с count()</td></tr>
     *   <tr><td>{@code count() == 0}</td><td>БД пуста</td>
     *       <td>Синхронизация БД и Java API</td></tr>
     * </table></p>
     *
     * <p><b>Ожидаемый SQL трафик (1 запрос):</b>
     * <pre>
     * SELECT * FROM tracks → 0 rows
     * </pre></p>
     *
     * <p><b>Критическая проверка {@code != null}:</b> Spring Data JPA <b>гарантирует</b>
     * непустой список даже при 0 элементов (Collections.emptyList()), никогда не возвращает
     * {@code null}. Тест доказывает контракт API.</p>
     *
     * <p><b>В production:</b> Первый запуск {@code GET /tracks} на пустой БД → {@code []} в JSON.</p>
     */
    @Test
    void shouldReturnEmptyListWhenNoTracks() {
        // GIVEN — пустая база данных (ничего не сохраняем)
        assertEquals(0L, trackRepository.count(), "БД должна быть пустой в начале теста");

        // WHEN — получаем все треки из пустой БД
        List<Track> allTracks = trackRepository.findAll();

        // THEN — проверяем пустой результат
        assertNotNull(allTracks, "findAll() никогда не возвращает null");
        assertTrue(allTracks.isEmpty(), "Список должен быть пустым при отсутствии треков");
        assertEquals(0, allTracks.size(), "Размер списка должен быть 0");
        assertEquals(0L, trackRepository.count(), "count() должен подтверждать пустоту БД");
    }

    /**
     * Проверяет корректную работу {@link TrackRepository#count()} для подсчёта треков в БД.
     *
     * <p><b>Цель теста:</b> Убедиться, что:
     * <ul>
     *   <li>{@code count()} возвращает точное количество записей в таблице {@code tracks};</li>
     *   <li>JPA/Hibernate выполняет оптимизированный {@code SELECT COUNT(*) FROM tracks};</li>
     *   <li>метод синхронизирован с {@code findAll().size()} и реальным состоянием БД.</li>
     * </ul>
     * </p>
     *
     * <p><b>AAA паттерн (Arrange-Act-Assert):</b></p>
     *
     * <pre>{@code
     * ┌──────────────┬──────────────────────────────┐
     * │    GIVEN     │ Сохраняем N треков          │
     * ├──────────────┼──────────────────────────────┤
     * │    WHEN      │ Вызываем count()            │
     * ├──────────────┼──────────────────────────────┤
     * │    THEN      │ count() == N                │
     * └──────────────┴──────────────────────────────┘
     * }</pre>
     *
     * <p><b>ШАГ 1: GIVEN — создание известного количества записей</b><br/>
     * Сохраняем 3 трека. H2 IDENTITY генерирует id=1,2,3:
     * <pre>
     * tracks: 3 записи
     * INSERT x3 → реальное состояние БД
     * </pre></p>
     *
     * <p><b>ШАГ 2: WHEN — выполнение операции</b><br/>
     * {@code trackRepository.count()} генерирует супер-оптимизированный SQL:
     * <pre>
     * SELECT COUNT(*) FROM tracks t1_0
     * → H2: 1 row → column "count" = 3 → Long.valueOf(3L)
     * </pre>
     * <b>НЕ загружает объекты в память!</b> — только число.</p>
     *
     * <p><b>ШАГ 3: THEN — тройная проверка консистентности</b><br/>
     * <table>
     *   <tr><th>Проверка</th><th>SQL/Логика</th><th>Что проверяем</th></tr>
     *   <tr><td>{@code count() == 3L}</td><td>SELECT COUNT(*)</td>
     *       <td>Точное число записей</td></tr>
     *   <tr><td>{@code findAll().size() == 3}</td><td>SELECT * (лениво)</td>
     *       <td>Синхронизация с полным списком</td></tr>
     *   <tr><td>{@code !allTracks.isEmpty()}</td><td>-</td>
     *       <td>Логическая консистентность</td></tr>
     * </table></p>
     *
     * <p><b>Ожидаемый SQL трафик:</b>
     * <pre>
     * INSERT x3 → 3 записи
     * SELECT COUNT(*) → 3
     * </pre></p>
     *
     * <p><b>Почему {@code count()} важен:</b>
     * <ul>
     *   <li>Оптимизация: 1 быстрый запрос вместо загрузки всех объектов</li>
     *   <li>Пагинация: {@code totalElements} в REST API</li>
     *   <li>Бизнес-логика: "у пользователя 5 треков" → dashboard</li>
     * </ul>
     * </p>
     *
     * <p><b>В production:</b> {@code GET /tracks/count} или Pageable {@code totalElements}.</p>
     */
    @Test
    void shouldReturnCorrectCount() {
        // GIVEN — сохраняем известное количество треков
        trackRepository.save(new Track().setName("Counted Track 1"));
        trackRepository.save(new Track().setName("Counted Track 2"));
        trackRepository.save(new Track().setName("Counted Track 3"));

        // WHEN — получаем количество записей
        long trackCount = trackRepository.count();

        // THEN — проверяем точность подсчёта
        assertEquals(3L, trackCount, "count() должен вернуть точное количество треков");

        // ДОПОЛНИТЕЛЬНАЯ ПРОВЕРКА: синхронизация с findAll()
        List<Track> allTracks = trackRepository.findAll();
        assertEquals(3, allTracks.size(), "findAll().size() должен совпадать с count()");
        assertFalse(allTracks.isEmpty(), "Список не должен быть пустым при count() > 0");
    }

    /**
     * Проверяет сбой сохранения трека при нарушении ограничения {@link jakarta.persistence.Column#nullable} = false.
     *
     * <p><b>Цель теста:</b> Убедиться, что:
     * <ul>
     *   <li>{@code @Column(nullable = false)} на поле {@code name} блокирует INSERT с NULL;</li>
     *   <li>H2 выбрасывает {@code DataIntegrityViolationException} при попытке сохранения;</li>
     *   <li>БД остаётся консистентной — запись не создаётся, {@code count()} не меняется.</li>
     * </ul>
     * </p>
     *
     * <p><b>AAA паттерн (Arrange-Act-Assert):</b></p>
     *
     * <pre>{@code
     * ┌──────────────┬──────────────────────────────┐
     * │    GIVEN     │ Пустая БД + invalid Track   │
     * ├──────────────┼──────────────────────────────┤
     * │    WHEN      │ save(invalidTrack)          │
     * ├──────────────┼──────────────────────────────┤
     * │    THEN      │ DataIntegrityViolationExc.  │
     * └──────────────┴──────────────────────────────┘
     * }</pre>
     *
     * <p><b>ШАГ 1: GIVEN — подготовка некорректных данных</b><br/>
     * Создаём {@code Track} без вызова {@code setName()}. Поле {@code name == null}:
     * <pre>
     * new Track() → id=null, name=null NOT NULL violation
     * count() = 0 (пустая БД)
     * </pre></p>
     *
     * <p><b>ШАГ 2: WHEN — попытка сохранения</b><br/>
     * {@code trackRepository.save(invalidTrack)}:
     * <pre>
     * 1. JPA валидация → @Column(nullable=false) DETECTED
     * 2. Hibernate генерирует: INSERT INTO tracks (name) VALUES (NULL)
     * 3. H2 DB: ConstraintViolation → DataIntegrityViolationException
     * 4. Spring Data JPA пробрасывает исключение ВЫШЕ
     * </pre></p>
     *
     * <p><b>ШАГ 3: THEN — проверка исключения и целостности БД</b><br/>
     * <table>
     *   <tr><th>Проверка</th><th>Что ожидаем</th><th>Почему важно</th></tr>
     *   <tr><td>{@code assertThrows(DataIntegrityViolationException.class, ...)}</td>
     *       <td>Исключение выброшено</td><td>NOT NULL сработал</td></tr>
     *   <tr><td>{@code count() == 0}</td><td>БД нетронута</td>
     *       <td>Транзакция откатывается автоматически</td></tr>
     * </table></p>
     *
     * <p><b>Ожидаемый SQL трафик + откат:</b>
     * <pre>
     * INSERT INTO tracks (name) VALUES (NULL)
     * H2: Column 'name' cannot be null
     * ROLLBACK → count() остаётся 0
     * </pre></p>
     *
     * <p><b>Критически важно:</b> @DataJpaTest откатывает транзакцию автоматически после исключения,
     * поэтому {@code count() == 0} даже после неудачного {@code save()}.</p>
     *
     * <p><b>В production:</b> REST {@code POST /tracks} с пустым name → 400 Bad Request.</p>
     */
    @Test
    void shouldFailSaveWithNullName() {
        // GIVEN — трек с нарушением NOT NULL constraint
        Track invalidTrack = new Track(); // name = null
        assertEquals(0L, trackRepository.count(), "БД пуста перед тестом");
        assertNull(invalidTrack.getName(), "name должен быть null для провокации ошибки");

        // WHEN + THEN — ожидаем исключение при save()
        DataIntegrityViolationException exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> trackRepository.save(invalidTrack),
                "save() должен выбросить исключение из-за name=null"
        );

        entityManager.clear();

        // Проверяем, что БД осталась нетронутой (авто-откат транзакции)
        assertEquals(0L, trackRepository.count(),
                "count() должен остаться 0 после отката неудачного INSERT");

        // Дополнительно: исключение содержит следы SQL ошибки
        assertTrue(exception.getMessage().toLowerCase().contains("name"),
                "Исключение должно упоминать поле 'name'");
    }

    /**
     * Проверяет работу {@link TrackRepository#findById(Object)}} для несуществующего ID.
     *
     * <p><b>Цель теста:</b> Убедиться, что:
     * <ul>
     *   <li>{@code findById(nonExistingId)} возвращает {@code Optional.empty()} для отсутствующей записи;</li>
     *   <li>JPA/Hibernate корректно обрабатывает {@code SELECT * FROM tracks WHERE id=?} → 0 rows;</li>
     *   <li>API контракт Spring Data JPA соблюден — никогда не {@code null}, всегда {@code Optional}.</li>
     * </ul>
     * </p>
     *
     * <p><b>AAA паттерн (Arrange-Act-Assert):</b></p>
     *
     * <pre>{@code
     * ┌──────────────┬──────────────────────────────┐
     * │    GIVEN     │ Пустая БД (count() = 0)     │
     * ├──────────────┼──────────────────────────────┤
     * │    WHEN      │ findById(999L)              │
     * ├──────────────┼──────────────────────────────┤
     * │    THEN      │ Optional.empty()            │
     * └──────────────┴──────────────────────────────┘
     * }</pre>
     *
     * <p><b>ШАГ 1: GIVEN — пустое состояние БД + несуществующий ID</b><br/>
     * Ничего не сохраняем. БД пуста: <pre>count() = 0</pre>.
     * Используем {@code 999L} — гарантированно не сгенерируется H2 IDENTITY (1,2,3...).</p>
     *
     * <p><b>ШАГ 2: WHEN — выполнение операции</b><br/>
     * {@code trackRepository.findById(999L)} генерирует:
     * <pre>
     * SELECT t1_0.id, t1_0.name
     * FROM tracks t1_0
     * WHERE t1_0.id = ?  ───┐
     *                       └── параметр: 999
     * → H2: 0 rows → Optional.empty()
     * </pre></p>
     *
     * <p><b>ШАГ 3: THEN — проверки пустого Optional</b><br/>
     * <table>
     *   <tr><th>Проверка</th><th>Что проверяем</th><th>Почему важно</th></tr>
     *   <tr><td>{@code result.isEmpty()}</td><td>Optional пустой</td>
     *       <td>Корректная обработка 0 rows</td></tr>
     *   <tr><td>{@code !result.isPresent()}</td><td>Не present</td>
     *       <td>API контракт Optional</td></tr>
     *   <tr><td>{@code count() == 0}</td><td>БД пуста</td>
     *       <td>Консистентность состояния</td></tr>
     * </table></p>
     *
     * <p><b>Ожидаемый SQL трафик (1 запрос):</b>
     * <pre>
     * SELECT * FROM tracks WHERE id=999 → 0 rows → Optional.empty()
     * </pre></p>
     *
     * <p><b>В production критично:</b> REST {@code GET /tracks/999} → 404 Not Found.
     * Клиентский код обычно: {@code track.orElseThrow(() -> new TrackNotFoundException())}</p>
     */
    @Test
    void shouldReturnEmptyOptionalForNonExistingId() {
        // GIVEN — пустая база данных + несуществующий ID
        assertEquals(0L, trackRepository.count(), "БД пуста перед тестом");
        Long nonExistingId = 999L; // H2 IDENTITY начинает с 1, 999L точно не существует

        // WHEN — ищем несуществующий трек
        Optional<Track> result = trackRepository.findById(nonExistingId);

        // THEN — Optional должен быть пустым
        assertTrue(result.isEmpty(), "findById(nonExistingId) должен вернуть Optional.empty()");
        assertFalse(result.isPresent(), "isPresent() должен быть false для несуществующего ID");

        // Консистентность с состоянием БД
        assertEquals(0L, trackRepository.count(), "БД остаётся пустой");
    }
}