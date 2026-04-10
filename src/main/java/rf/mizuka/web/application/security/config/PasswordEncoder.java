package rf.mizuka.web.application.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Конфигурационный класс, регистрирующий в контексте Spring
 * бин шифровальщика паролей, используемый для безопасного хранения
 * и проверки паролей в системе аутентификации.
 *
 * <p>Класс помечен аннотацией {@link org.springframework.context.annotation.Configuration @Configuration},
 * что указывает, что он содержит методы, аннотированные {@link org.springframework.context.annotation.Bean @Bean},
 * которые возвращают компоненты, управляемые Spring‑контейнером. В Spring Security
 * такой бин шифровальщика паролей ({@link org.springframework.security.crypto.password.PasswordEncoder})
 * используется по умолчанию при работе с формами логина, {@code UserDetailsService}
 * и хранением хэшей паролей в базе данных.</p>
 *
 * <p>Механизм работы бина {@code bCryptPasswordEncoder()}:
 * <ul>
 *   <li>Метод возвращает экземпляр {@link org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder},
 *   который реализует стандартный алгоритм хеширования BCrypt для паролей.
 *   BCrypt является потенциально устойчивым к атакам по времени и атакам с перебором,
 *   благодаря встроенному “salt” и возможности настройки уровня сложности (work factor).</li>
 *   <li>При регистрации нового пользователя пароль вводится в систему как строка
 *   и затем передаётся в метод {@code encode(CharSequence rawPassword)}, где
 *   он преобразуется в хэш BCrypt, который сохраняется в базе данных.</li>
 *   <li>При последующей аутентификации (логине) введённый пользователем пароль
 *   также хешируется тем же экземпляром {@code PasswordEncoder} и сравнивается
 *   с хэшем, хранящимся в базе; совпадение подтверждает корректность пароля
 *   без хранения его в открытом виде.</li>
 * </ul>
 * </p>
 *
 * <p>Интеграция с Spring Security:
 * <ul>
 *   <li>Созданный через {@code @Bean} бин типа {@code PasswordEncoder}
 *   автоматически подключается к механизму аутентификации Spring Security
 *   и используется по умолчанию в {@code DaoAuthenticationProvider},
 *   если в конфигурации не указан альтернативный экземпляр.</li>
 *   <li>Такой подход позволяет централизовать логику хеширования и обеспечить
 *   единый формат хранения паролей, независимо от используемого {@code UserDetailsService}
 *   (например, база данных, JPA, репозиторий и т.п.).</li>
 * </ul>
 * </p>
 *
 * <p>Таким образом, данный класс:
 * <ul>
 *   <li>является частью конфигурации безопасности приложения;</li>
 *   <li>регистрирует стандартный бин BCrypt‑шифровальщика для паролей;</li>
 *   <li>обеспечивает безопасное хранение и проверку паролей в системе аутентификации.</li>
 * </ul>
 * </p>
 */
@Configuration
public class PasswordEncoder {
    /**
     * Создаёт и возвращает шифровальщик паролей BCrypt.
     *
     * <p>Аннотация {@code @Bean} указывает, что Spring должен зарегистрировать
     * возвращённый экземпляр {@link org.springframework.security.crypto.password.PasswordEncoder}
     * как бин в контексте, который будет использоваться Spring Security
     * при работе с логинами и хранением хэшей паролей.</p>
     *
     * @return экземпляр {@link org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder},
     *         который хранит пароль в виде BCrypt‑хэша, используя встроенный salt
     *         и настраиваемый уровень сложности.
     */
    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
