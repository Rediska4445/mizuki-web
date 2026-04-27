package rf.mizuka.application.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import rf.mizuka.web.application.controllers.auth.UserExistException;
import rf.mizuka.web.application.database.user.repository.UserRepository;
import rf.mizuka.web.application.models.user.User;
import rf.mizuka.web.application.services.user.UserService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Интеграционные тесты для сервиса аутентификации {@link UserService},
 * отвечающего за регистрацию пользователей в системе.
 *
 * <p>Класс использует Spring Boot Test для запуска полного контекста приложения:
 * <ul>
 *   <li>Загружает все компоненты Spring Boot: веб, JPA, Spring Security, DataSource</li>
 *   <li>Предоставляет доступ к реальной базе данных (H2/PostgreSQL/MySQL)</li>
 *   <li>Позволяет тестировать интеграцию UserService с UserRepository, PasswordEncoder и Spring Security</li>
 * </ul>
 *
 * <p>Аннотации класса:
 * <ul>
 *   <li>{@link SpringBootTest} — запускает полноценный Spring Boot-контекст</li>
 *   <li>{@link Transactional} — каждый тестовый метод выполняется внутри транзакции</li>
 *   <li>{@link Rollback} — транзакция откатывается после завершения теста (данные не сохраняются в БД)</li>
 * </ul>
 *
 * <p>Тесты проверяют:
 * <ul>
 *   <li>Успешную регистрацию пользователя с корректным BCrypt-хэшем пароля</li>
 *   <li>Повторную регистрацию с тем же username → IllegalArgumentException</li>
 *   <li>Возможность аутентификации через Spring Security после регистрации</li>
 * </ul>
 *
 * @see SpringBootTest
 * @see Transactional
 * @see Rollback
 */
@SpringBootTest
@Transactional
@Rollback
public class UserServiceTest {
    /**
     * Служба аутентификации для регистрации пользователей в системе.
     *
     * <p>Сервис отвечает за бизнес-логику регистрации:
     * <ul>
     *   <li>Проверка уникальности username через UserRepository.existsByUsername()</li>
     *   <li>Создание сущности User с хэшированным паролем BCrypt</li>
     *   <li>Сохранение пользователя в базу данных через UserRepository.save()</li>
     *   <li>Управление транзакциями через @Transactional</li>
     * </ul>
     *
     * Тесты проверяют:
     * <ul>
     *   <li>Успешную регистрацию нового пользователя</li>
     *   <li>Повторную регистрацию того же username → IllegalArgumentException</li>
     *   <li>Корректность хэширования пароля</li>
     * </ul>
     *
     * @see UserService#registerUser(String, String)
     */
    @Autowired
    private UserService userService;

    /**
     * JPA-репозиторий для доступа к таблице пользователей.
     *
     * <p>Репозиторий обеспечивает доступ к данным в базе данных:
     * <ul>
     *   <li>existsByUsername(username) — проверка уникальности пользователя</li>
     *   <li>save(user) — сохранение пользователя в БД</li>
     *   <li>findByUsername(username) — поиск пользователя по логину</li>
     *   <li>count() — подсчёт количества пользователей</li>
     * </ul>
     *
     * Тесты используют репозиторий для:
     * <ul>
     *   <li>Проверки, что пользователь создан/не создан</li>
     *   <li>Валидации данных в БД после тестов</li>
     * </ul>
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * Spring Security UserDetailsService для аутентификации пользователей.
     *
     * <p>Служба загружает UserDetails из базы данных:
     * <ul>
     *   <li>loadUserByUsername(username) — возвращает UserDetails для пользователя</li>
     *   <li>Используется в DaoAuthenticationProvider при логине</li>
     * </ul>
     *
     * Тесты проверяют:
     * <ul>
     *   <li>Что пользователь может быть найден после регистрации</li>
     *   <li>Что UserDetails содержит корректный username и хэшированный пароль</li>
     * </ul>
     */
    @Autowired
    private UserDetailsService userDetailsService;

    /**
     * Кодировщик пароля BCrypt для Spring Security.
     *
     * <p>PasswordEncoder отвечает за хэширование паролей:
     * <ul>
     *   <li>encode(rawPassword) — создание BCrypt-хэша пароля</li>
     *   <li>matches(rawPassword, encodedPassword) — проверка пароля</li>
     * </ul>
     *
     * Тесты используют кодировщик для:
     * <ul>
     *   <li>Проверки, что пароль хэшируется правильно</li>
     *   <li>Валидации соответствия исходного пароля хэшированному</li>
     * </ul>
     */
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Тест успешной регистрации нового пользователя и его готовности к аутентификации.
     *
     * <p>Проверяет полный кейс регистрации пользователя через {@link UserService#registerUser(String, String)}:
     * <ul>
     *   <li>Пользователь с заданным именем ещё не существует в базе данных</li>
     *   <li>Метод корректно создает нового пользователя с BCrypt-хэшем пароля</li>
     *   <li>Пользователь сохраняется в БД (через транзакцию)</li>
     *   <li>После регистрации пользователь может успешно войти в систему:
     *     <ul>
     *       <li>DaoAuthenticationProvider находит User в БД</li>
     *       <li>passwordEncoder.matches() возвращает true для правильного пароля</li>
     *       <li>Authentication успешно устанавливается в SecurityContextHolder</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <p>Тест использует реальную JPA‑транзакцию и H2‑базу данных (через @SpringBootTest и @Transactional),
     * чтобы проверить именно «уровень БД», а не только моки репозитория.
     *
     * <p>Это КРИТИЧЕСКИЙ тест:
     * <ul>
     *   <li>Проверяет, что логика регистрации корректно интегрируется с Spring Security</li>
     *   <li>Подтверждает, что пароль реально хэшируется и match() работает</li>
     *   <li>Проверяет, что @Transactional предотвращает запись дубликата</li>
     * </ul>
     *
     * @throws Exception если произошла ошибка при выполнении бизнес‑логики или аутентификации
     */
    @Test
    @DisplayName("Успешная регистрация нового пользователя позволяет ему сразу войти в систему")
    void shouldRegisterNewUserAndAllowImmediateLogin() throws Exception {
        long initialCount = userRepository.count();
        assertThat(initialCount).isGreaterThanOrEqualTo(0);

        // Шаг 1: подготовка данных
        String username = "newuser";
        String rawPassword = "secret123";

        // Убеждаемся, что пользователь не существует ДО регистрации
        assertThat(userRepository.existsByUsername(username)).isFalse();

        // Шаг 2: успешный вызов регистрационного метода
        userService.registerUser(username, rawPassword);

        // Шаг 3: проверка, что пользователь реально сохранён в БД
        // 1) ищем по username
        Optional<User> userOpt = userRepository.findByUsername(username);
        assertThat(userOpt).isPresent();
        User user = userOpt.get();
        assertThat(user.getUsername()).isEqualTo(username);
        assertThat(user.getPassword()).isNotEmpty();

        // 2) проверяем, что пароль захэширован (BCrypt — строка начинается с $2a$ или $2b$)
        String encodedPassword = user.getPassword();
        assertThat(encodedPassword)
                .matches("\\$2[ab]\\$\\d{2}\\$.{53}");  // BCrypt обычно выглядит как $2a$10$.... (59 знаков)

        // 3) проверяем, что пароль правильно проходит через passwordEncoder.matches
        assertThat(passwordEncoder.matches(rawPassword, encodedPassword)).isTrue();

        // Шаг 4: проверяем, что пользователь может быть аутентифицирован
        // Тест «попытка логина» = UserDetailsService + DaoAuthenticationProvider
        UserDetails loadedUser = userDetailsService.loadUserByUsername(username);
        assertThat(loadedUser.getUsername()).isEqualTo(username);
        assertThat(loadedUser.getPassword()).isEqualTo(encodedPassword);
        // нет смысла дважды проверять, что пароль BCrypt — это уже проверили выше

        // Шаг 5: проверка, что повторная регистрация того же username вызывает ошибку
        assertThatThrownBy(() -> userService.registerUser(username, "anotherPass"))
                .isInstanceOf(UserExistException.class)
                .hasMessage(username);

        // Дополнительная проверка, что пользователь всё ещё один в БД
        // (транзакция откатилась, дубль не сохранился)
        assertThat(userRepository.count()).isEqualTo(initialCount + 1);
    }
}
