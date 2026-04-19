package rf.mizuka.web.application.models.rest;

import jakarta.persistence.*;
import rf.mizuka.web.application.models.user.User;

import java.util.Set;

/**
 * JPA сущность для хранения информации о зарегистрированных приложениях в БД.
 *
 * <p>Представляет внешнее REST API приложение разработчика с учетными данными доступа
 * и правами. Хранит связь с владельцем ({@link User}) и список разрешений ({@code scopes}).</p>
 *
 * <p><b>JPA конфигурация:</b>
 * <ul>
 *   <li>{@link jakarta.persistence.Entity @Entity} — таблица в БД;</li>
 *   <li>{@link jakarta.persistence.Table @Table(name = "applications")} — имя таблицы.</li>
 * </ul>
 * </p>
 *
 * <h3>Поля таблицы:</h3>
 * <table border="1">
 *   <tr><th>Поле</th><th>Тип</th><th>JPA аннотация</th><th>Назначение</th></tr>
 *   <tr><td>{@code id}</td><td>{@code Long}</td><td>{@code @Id @GeneratedValue(IDENTITY)}</td><td>Автоинкрементный первичный ключ</td></tr>
 *   <tr><td>{@code clientId}</td><td>{@code String}</td><td>{@code @Column(unique=true, nullable=false)}</td><td>Уникальный идентификатор приложения</td></tr>
 *   <tr><td>{@code clientSecret}</td><td>{@code String}</td><td>{@code @Column(nullable=false)}</td><td>Секретный ключ доступа (хэш)</td></tr>
 *   <tr><td>{@code developerName}</td><td>{@code String}</td><td>{@code @Column(name="application_name", length=64)}</td><td>Имя приложения для UI</td></tr>
 *   <tr><td>{@code scopes}</td><td>{@code Set<String>}</td><td>{@code @ElementCollection(fetch=EAGER)}</td><td>Список разрешений (read, write, admin)</td></tr>
 *   <tr><td>{@code owner}</td><td>{@link User}</td><td>{@code @ManyToOne @JoinColumn}</td><td>Владелец приложения из таблицы users</td></tr>
 * </table>
 *
 * <h3>Hibernate генерируемая схема БД:</h3>
 * <pre>
 * CREATE TABLE applications (
 *   id BIGINT PRIMARY KEY AUTO_INCREMENT,
 *   client_id VARCHAR(255) UNIQUE NOT NULL,
 *   client_secret VARCHAR(255) NOT NULL,
 *   application_name VARCHAR(64),
 *   owner_id BIGINT,
 *   FOREIGN KEY (owner_id) REFERENCES users(id)
 * );
 *
 * CREATE TABLE applications_scopes (
 *   application_id BIGINT,
 *   scopes VARCHAR(255),
 *   PRIMARY KEY (application_id, scopes),
 *   FOREIGN KEY (application_id) REFERENCES applications(id)
 * );
 * </pre>
 *
 * <h3>Особенности JPA маппинга:</h3>
 * <ul>
 *   <li>{@code @GeneratedValue(strategy = GenerationType.IDENTITY)} — MySQL/PostgreSQL автоинкремент;</li>
 *   <li>{@code @Column(unique = true)} на clientId — БД constraint предотвращает дубликаты;</li>
 *   <li>{@code @ElementCollection(fetch = FetchType.EAGER)} — scopes загружаются сразу с Application;</li>
 *   <li>{@code @ManyToOne} owner — ленивая загрузка (загружается при первом обращении getOwner()).</li>
 * </ul>
 *
 * <h3>Fluent API стиль (getter/setter цепочки):</h3>
 * <p>Каждый setter возвращает {@code this} для удобной цепочки:</p>
 * <pre>
 * Application app = new Application()
 *   .setClientId("app-123")
 *   .setClientSecret(bcryptHash)
 *   .setDeveloperName("Mobile App v1.0")
 *   .setScopes(Set.of("read", "write"))
 *   .setOwner(user);
 * </pre>
 *
 * <h3>Использование:</h3>
 * <ul>
 *   <li>Регистрация: {@code applicationRepository.save(app)} → INSERT;</li>
 *   <li>Поиск: {@code repository.findByClientId("app-123")} → SELECT;</li>
 *   <li>scopes: {@code app.getScopes()} → Set&lt;String&gt; {"read", "write"}.</li>
 * </ul>
 *
 * <p><b>Ограничения БД (автоматически):</b>
 * <ul>
 *   <li>UNIQUE(client_id) — нельзя два приложения с одинаковым clientId;</li>
 *   <li>NOT NULL(client_id, client_secret) — обязательные поля для аутентификации.</li>
 * </ul>
 * </p>
 */
@Entity
@Table(name = "applications")
public class Application {
    /**
     * Первичный ключ таблицы applications (автоинкремент).
     *
     * <p><b>JPA аннотации:</b>
     * <ul>
     *   <li>{@link jakarta.persistence.Id @Id} — помечает поле как первичный ключ таблицы;</li>
     *   <li>{@link jakarta.persistence.GeneratedValue @GeneratedValue(strategy = GenerationType.IDENTITY)} —
     *      автоинкрементный генератор первичного ключа:
     *      <ul>
     *        <li>MySQL: {@code AUTO_INCREMENT};</li>
     *        <li>PostgreSQL: {@code SERIAL};</li>
     *        <li>H2: {@code IDENTITY}.</li>
     *      </ul>
     *   </li>
     * </ul>
     * </p>
     *
     * <h3>Hibernate SQL:</h3>
     * <pre>
     * CREATE TABLE applications (
     *   id BIGINT PRIMARY KEY AUTO_INCREMENT  ← генерируется Hibernate
     * );
     *
     * INSERT INTO applications (...) → id = LAST_INSERT_ID()  ← авто
     * </pre>
     *
     * <p><b>Жизненный цикл:</b>
     * <ul>
     *   <li>При {@code save(new Application())} → Hibernate: INSERT → id = 1,2,3...;</li>
     *   <li>При {@code findById(1L)} → возвращает Application с заполненным id;</li>
     *   <li>Никогда не {@code null} после сохранения в БД.</li>
     * </ul>
     * </p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * Уникальный идентификатор REST API приложения (client_id).
     *
     * <p><b>JPA аннотация:</b>
     * <ul>
     *   <li>{@link jakarta.persistence.Column @Column(unique = true, nullable = false)} —
     *      создает БД ограничения:
     *      <ul>
     *        <li>{@code UNIQUE} — нельзя два приложения с одинаковым clientId;</li>
     *        <li>{@code NOT NULL} — обязательное поле.</li>
     *      </ul>
     *   </li>
     * </ul>
     * </p>
     *
     * <h3>Hibernate SQL:</h3>
     * <pre>
     * client_id VARCHAR(255) UNIQUE NOT NULL  ← constraint в БД
     *
     * INSERT INTO applications (client_id='app123') → OK
     * INSERT INTO applications (client_id='app123') → ERROR: Duplicate entry
     * </pre>
     *
     * <h3>Использование:</h3>
     * <ul>
     *   <li>Поиск: {@code repository.findByClientId("app123")} → SELECT WHERE client_id = ?;</li>
     *   <li>Регистрация: генерируется уникальный {@code UUID.randomUUID().toString()};</li>
     *   <li>Аутентификация: клиент передает {@code client_id} в /oauth2/token.</li>
     * </ul>
     *
     * <p><b>Constraint violation:</b><br/>
     * При {@code save()} с дубликатом → {@link org.springframework.dao.DataIntegrityViolationException} → HTTP 400/500.</p>
     */
    @Column(unique = true, nullable = false)
    private String clientId;
    /**
     * Секретный ключ доступа приложения (хранится в хэшированном виде).
     *
     * <p><b>JPA аннотация:</b>
     * <ul>
     *   <li>{@link jakarta.persistence.Column @Column(nullable = false)} — обязательное поле,
     *      {@code NOT NULL} constraint в БД.</li>
     * </ul>
     * </p>
     *
     * <h3>Hibernate SQL:</h3>
     * <pre>
     * client_secret VARCHAR(255) NOT NULL  ← BCrypt хэш (~60 символов)
     *
     * INSERT INTO applications (client_secret=NULL) → ERROR: Column cannot be null
     * </pre>
     *
     * <h3>Использование:</h3>
     * <ul>
     *   <li>При создании: {@code passwordEncoder.encode("secret123")} → {@code $2a$10$NCl...};</li>
     *   <li>При аутентификации: {@code passwordEncoder.matches(inputSecret, storedHash)};</li>
     *   <li><strong>Никогда plain text</strong> — только хэш в БД.</li>
     * </ul>
     *
     * <p><b>Constraint violation:</b><br/>
     * {@code save(app.setClientSecret(null))} → {@link org.springframework.dao.DataIntegrityViolationException}.</p>
     */
    @Column(nullable = false)
    private String clientSecret;
    /**
     * Человекочитаемое имя приложения для административного интерфейса.
     *
     * <p><b>JPA аннотация:</b>
     * <ul>
     *   <li>{@link jakarta.persistence.Column @Column(name = "application_name", length = 64)} —
     *      кастомное имя колонки в БД и ограничение длины:
     *      <ul>
     *        <li>{@code application_name VARCHAR(64)} — не {@code developerName};</li>
     *        <li>{@code length = 64} — максимум 64 символа (срез при превышении).</li>
     *      </ul>
     *   </li>
     * </ul>
     * </p>
     *
     * <h3>Hibernate SQL:</h3>
     * <pre>
     * application_name VARCHAR(64)  ← кастомное имя колонки
     *
     * INSERT INTO applications (application_name='Mobile App v1.0') → OK
     * INSERT INTO applications (application_name='x'.repeat(65)) → 'x'.repeat(64)
     * </pre>
     *
     * <h3>Использование:</h3>
     * <ul>
     *   <li>Админка: "Приложение Mobile App v1.0 (clientId: app123) принадлежит alice";</li>
     *   <li>UI: список приложений с именами вместо clientId;</li>
     *   <li>Опциональное поле — может быть {@code null}.</li>
     * </ul>
     */
    @Column(name = "application_name", length = 64)
    private String developerName;
    /**
     * Список разрешений/прав доступа приложения (множество строк).
     *
     * <p><b>JPA аннотация:</b>
     * <ul>
     *   <li>{@link jakarta.persistence.ElementCollection @ElementCollection} — коллекция примитивных значений
     *      (String) в отдельной таблице;</li>
     *   <li>{@link jakarta.persistence.FetchType @FetchType.EAGER} — загружается сразу вместе с Application,
     *      избегает N+1 проблемы.</li>
     * </ul>
     * </p>
     *
     * <h3>Hibernate SQL схема:</h3>
     * <pre>
     * CREATE TABLE applications_scopes (
     *   application_id BIGINT,    ← внешний ключ
     *   scopes VARCHAR(255),      ← "read", "write", "admin"
     *   PRIMARY KEY (application_id, scopes)
     * );
     * </pre>
     *
     * <h3>Использование:</h3>
     * <ul>
     *   <li>{@code app.getScopes()} → {@code Set.of("read", "write")};</li>
     *   <li>Автоматическая денормализация: {@code Set → несколько строк в applications_scopes};</li>
     *   <li>При {@code save(app)} → INSERT в applications + INSERT в applications_scopes.</li>
     * </ul>
     *
     * <p><b>EAGER загрузка:</b> Hibernate делает JOIN при {@code findByClientId()}:
     * <pre>SELECT a.*, s.scopes FROM applications a
     * LEFT JOIN applications_scopes s ON a.id = s.application_id</pre></p>
     */
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> scopes;
    /**
     * Владелец приложения (связь "многие-приложения к одному-пользователю").
     *
     * <p><b>JPA аннотации:</b>
     * <ul>
     *   <li>{@link jakarta.persistence.ManyToOne @ManyToOne} — отношение многие-к-одному;</li>
     *   <li>{@link jakarta.persistence.JoinColumn @JoinColumn(name = "owner_id")} — внешний ключ
     *      в таблице applications на таблицу users.</li>
     * </ul>
     * </p>
     *
     * <h3>Hibernate SQL схема:</h3>
     * <pre>
     * applications:
     *   owner_id BIGINT,           ← внешний ключ
     *   FOREIGN KEY (owner_id) REFERENCES users(id)
     * </pre>
     *
     * <h3>Ленивая загрузка (по умолчанию):</h3>
     * <ul>
     *   <li>При {@code findByClientId()} → owner = proxy (не загружается сразу);</li>
     *   <li>При {@code app.getOwner().getUsername()} → Hibernate: SELECT * FROM users WHERE id = ?;</li>
     * </ul>
     *
     * <h3>Использование:</h3>
     * <ul>
     *   <li>Админка: "Приложение app123 принадлежит пользователю alice";</li>
     *   <li>{@code app.setOwner(user)} → устанавливает owner_id = user.getId();</li>
     *   <li>Опционально: может быть {@code null} (приложение без владельца).</li>
     * </ul>
     */
    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    /**
     * Стандартный JPA/Hibernate getter для первичного ключа.
     *
     * <p>Возвращает автоинкрементный ID, сгенерированный БД при {@code save()}.
     * После сохранения в БД всегда не {@code null}.</p>
     *
     * <p><b>Использование:</b>
     * <ul>
     *   <li>{@code repository.findByClientId("app123").getId()} → {@code 1L};</li>
     *   <li>Админка/UI: отображение номера приложения.</li>
     * </ul>
     */
    public Long getId() {
        return id;
    }

    /**
     * Fluent setter для первичного ключа (цепочка методов).
     *
     * <p><b>Особенность:</b> Только для тестирования/mapping. В реальной БД
     * {@code @GeneratedValue(IDENTITY)} управляет id автоматически.</p>
     *
     * <h3>Fluent API пример:</h3>
     * <pre>
     * Application app = new Application()
     *     .setId(999L)    ← Только для тестов!
     *     .setClientId("test-app")
     *     .setClientSecret("hash");
     * </pre>
     *
     * <p><b>В production:</b> {@code setId()} игнорируется — БД сама генерирует ID.</p>
     */
    public Application setId(Long id) {
        this.id = id;
        return this;
    }

    /**
     * Getter для уникального идентификатора приложения.
     *
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Fluent setter для уникального идентификатора приложения.
     *
     * <p>Устанавливает clientId с автоматической проверкой {@code UNIQUE} constraint
     * БД при {@code repository.save()}.</p>
     *
     * <p><b>Пример цепочки:</b>
     * <pre>
     * new Application()
     *   .setClientId("app-123-" + UUID.randomUUID())
     *   .setClientSecret(hash)
     *   .setScopes(scopes);
     * </pre>
     */
    public Application setClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    /**
     * Getter для секретного ключа приложения (BCrypt хэш).
     *
     * <p>Возвращает хэшированное значение, хранящееся в БД.
     * Используется {@link org.springframework.security.crypto.password.PasswordEncoder#matches(CharSequence, String)}
     * для проверки введенного секрета при аутентификации.</p>
     *
     * <p><b>Никогда не возвращает plain text</b> — только результат {@code encode()}.</p>
     */
    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * Fluent setter для секретного ключа (только хэшированное значение).
     *
     * <p><b>Важно:</b> При установке передается уже хэшированное значение:
     * {@code passwordEncoder.encode(plainSecret)} → {@code $2a$10$NCl...}.</p>
     *
     * <p><b>Fluent пример:</b>
     * <pre>
     * app.setClientSecret(passwordEncoder.encode("mySecret123"));
     * </pre>
     */
    public Application setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
        return this;
    }

    /**
     * Getter для человекочитаемого имени приложения.
     *
     * <p>Используется в административном интерфейсе и списках приложений.
     * Может быть {@code null} (опциональное поле).</p>
     */
    public String getDeveloperName() {
        return developerName;
    }

    /**
     * Fluent setter для человекочитаемого имени приложения.
     *
     * <p>Устанавливает имя с автоматическим ограничением длины 64 символа
     * (JPA {@code @Column(length = 64)}).</p>
     *
     * <p><b>Пример:</b> "Mobile App v2.1", "Admin Dashboard", "IoT Gateway".</p>
     */
    public Application setDeveloperName(String developerName) {
        this.developerName = developerName;
        return this;
    }

    /**
     * Getter для списка разрешений приложения.
     *
     * <p>Возвращает {@link Set}&lt;String&gt; с правами доступа, загруженными EAGER из таблицы
     * {@code applications_scopes}. Никогда не возвращает {@code null} после загрузки из БД.</p>
     *
     * <p><b>Примеры значений:</b> {@code Set.of("read", "write")}, {@code Set.of("admin")},
     * {@code Set.of("read:users", "write:orders")}.</p>
     */
    public Set<String> getScopes() {
        return scopes;
    }

    /**
     * Fluent setter для списка разрешений приложения.
     *
     * <p>При {@code repository.save()} Hibernate автоматически создает/обновляет записи
     * в таблице {@code applications_scopes} для каждого элемента Set.</p>
     *
     * <p><b>Пример цепочки:</b>
     * <pre>
     * app.setScopes(Set.of("read", "write", "admin"));
     * </pre>
     */
    public Application setScopes(Set<String> scopes) {
        this.scopes = scopes;
        return this;
    }

    /**
     * Getter для владельца приложения.
     *
     * <p>Возвращает связанную {@link User} сущность (ленивая загрузка).
     * Первый вызов {@code getOwner()} инициирует SELECT из таблицы {@code users}.</p>
     *
     * <p>Может быть {@code null} — приложение без владельца.</p>
     */
    public User getOwner() {
        return owner;
    }

    /**
     * Fluent setter для владельца приложения.
     *
     * <p>Устанавливает связь ManyToOne: {@code owner_id = owner.getId()} во внешнем ключе.
     * При {@code save()} Hibernate автоматически заполняет {@code owner_id} в таблице applications.</p>
     */
    public Application setOwner(User owner) {
        this.owner = owner;
        return this;
    }
}
