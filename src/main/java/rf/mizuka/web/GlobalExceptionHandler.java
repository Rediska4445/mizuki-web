package rf.mizuka.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Централизованный глобальный обработчик исключений для REST API Spring-приложения.
 *
 * <p>Класс помечен аннотацией:
 * <ul>
 *   <li>{@link org.springframework.web.bind.annotation.RestControllerAdvice @RestControllerAdvice} —
 *   указывает Spring, что этот класс является глобальным советником (advice) для контроллеров,
 *   автоматически регистрируя его как {@link org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver ExceptionHandlerExceptionResolver}.
 *   Комбинация {@link org.springframework.web.bind.annotation.ControllerAdvice @ControllerAdvice} +
 *   {@link org.springframework.web.bind.annotation.ResponseBody @ResponseBody} обеспечивает:
 *   <ul>
 *     <li>автоматическое обнаружение через {@link org.springframework.context.annotation.ComponentScan ComponentScan};</li>
 *     <li>глобальное применение ко всем {@link org.springframework.web.bind.annotation.RestController @RestController}
 *     и {@link org.springframework.stereotype.Controller @Controller} классам;</li>
 *     <li>автоматическую JSON-сериализацию возвращаемых объектов через {@link org.springframework.http.converter.json.MappingJackson2HttpMessageConverter Jackson}.</li>
 *   </ul>
 *   </li>
 * </ul>
 * </p>
 *
 * <p>Основная роль этого класса — централизованно перехватывать исключения из контроллеров и
 * преобразовывать их в стандартизированные HTTP-ответы JSON-формата вместо стандартных
 * HTML-страниц ошибок Tomcat или stack trace в теле ответа.</p>
 *
 * <p>Механизм работы в Spring MVC/DispatcherServlet:
 * <ol>
 *   <li>Контроллер выбрасывает исключение во время обработки {@code @RequestMapping} метода;</li>
 *   <li>{@link org.springframework.web.servlet.DispatcherServlet DispatcherServlet} передает исключение
 *   цепочке {@link org.springframework.web.servlet.HandlerExceptionResolver HandlerExceptionResolver};</li>
 *   <li>{@link org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver ExceptionHandlerExceptionResolver}
 *   сканирует все {@code @RestControllerAdvice} классы в ApplicationContext;</li>
 *   <li>Ищет методы с {@link org.springframework.web.bind.annotation.ExceptionHandler @ExceptionHandler},
 *   совместимые по типу исключения (точное совпадение или наследование);</li>
 *   <li><strong>Приоритет выбора:</strong> более специфичный тип исключения имеет приоритет
 *   ({@code BadCredentialsException} > {@code Exception}). Spring использует алгоритм "best match";</li>
 *   <li>Находит первый подходящий метод → вызывает его → возвращает {@link org.springframework.http.ResponseEntity ResponseEntity}.</li>
 * </ol>
 * </p>
 *
 * <p>Конкретные обработчики исключений внутри класса:
 * <ul>
 *   <li><strong>{@code @ExceptionHandler(BadCredentialsException.class)}</strong> — специфичный обработчик
 *   для {@link org.springframework.security.authentication.BadCredentialsException BadCredentialsException} из
 *   Spring Security. Возвращает HTTP 401 {@link org.springframework.http.HttpStatus#UNAUTHORIZED UNAUTHORIZED}
 *   с JSON {@code {"error": "Bad credentials"}}. Срабатывает при неверных учетных данных
 *   в {@link org.springframework.security.authentication.AuthenticationManager AuthenticationManager}.</li>
 *   <li><strong>{@code @ExceptionHandler(Exception.class)}</strong> — catch-all обработчик с наименьшим приоритетом.
 *   Ловит все остальные {@link java.lang.Exception Exception} (включая RuntimeException, NullPointerException,
 *   SQLException и др.). Возвращает HTTP 500 {@link org.springframework.http.HttpStatus#INTERNAL_SERVER_ERROR INTERNAL_SERVER_ERROR}
 *   с JSON {@code {"error": "An error occurred", "message": "[stack trace]"}} для логирования.</li>
 * </ul>
 * </p>
 *
 * <p>Преимущества архитектурного подхода:
 * <ul>
 *   <li><strong>DRY принцип:</strong> один класс вместо {@code try-catch} блоков в каждом контроллере;</li>
 *   <li><strong>Консистентность API:</strong> единообразные JSON-ответы на ошибки во всем приложении;</li>
 *   <li><strong>Безопасность:</strong> не раскрывает внутренние детали исключений клиентам
 *   (generic сообщения + stack trace только для логов);</li>
 *   <li><strong>Расширяемость:</strong> легко добавлять обработчики для новых типов исключений
 *   (ValidationException, EntityNotFoundException и т.д.);</li>
 *   <li><strong>Производительность:</strong> Spring кэширует резолверы и mappings методов во время запуска.</li>
 * </ul>
 * </p>
 *
 * <p>Важные особенности реализации:
 * <ul>
 *   <li>Методы возвращают {@link org.springframework.http.ResponseEntity ResponseEntity}&lt;{@link java.util.Map Map}&lt;String,String&gt;&gt;
 *   для полного контроля HTTP-статуса, headers и body;</li>
 *   <li>{@link java.util.Map#of Map.of()} создает immutable Map (Java 9+), автоматически сериализуется в JSON;</li>
 *   <li>Параметр метода {@code RuntimeException e} в первом обработчике технически корректен,
 *   так как {@code BadCredentialsException} наследуется от {@code RuntimeException};</li>
 *   <li>Обработчик с {@code Exception.class} является fallback — никогда не сработает для
 *   {@code BadCredentialsException} из-за приоритета специфичных обработчиков.</li>
 * </ul>
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler
{
    /**
     * Специфичный обработчик исключений аутентификации Spring Security.
     *
     * <p>Класс метода помечен аннотацией:
     * <ul>
     *   <li>{@link org.springframework.web.bind.annotation.ExceptionHandler @ExceptionHandler(BadCredentialsException.class)} —
     *   связывает метод с конкретным типом исключения {@link org.springframework.security.authentication.BadCredentialsException BadCredentialsException}.
     *   Spring использует механизм {@link org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver ExceptionHandlerMethodResolver}
     *   для сопоставления типа выброшенного исключения с обработчиками во всех {@code @RestControllerAdvice} классах.</li>
     * </ul>
     * </p>
     *
     * <p><strong>Условия срабатывания:</strong></p>
     * <ol>
     *   <li>В контроллере выбрасывается {@code BadCredentialsException} (наследник {@code AuthenticationException} → {@code RuntimeException});
     *      типично при вызове {@link org.springframework.security.authentication.AuthenticationManager#authenticate(Authentication)} AuthenticationManager.authenticate()}
     *      с неверными учетными данными (логин/пароль);</li>
     *   <li>{@link org.springframework.web.servlet.DispatcherServlet DispatcherServlet} передает исключение
     *      {@link org.springframework.web.servlet.HandlerExceptionResolver HandlerExceptionResolver} цепочке;</li>
     *   <li>{@link org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver ExceptionHandlerExceptionResolver}
     *      находит этот метод как <strong>лучшее совпадение</strong> (exact type match);</li>
     *   <li>Метод вызывается <strong>раньше</strong> общего {@code @ExceptionHandler(Exception.class)}
     *      из-за приоритета специфичных типов в Spring алгоритме "best match".</li>
     * </ol>
     * </p>
     *
     * <p><strong>Механизм Spring Security, вызывающий исключение:</strong></p>
     * <ul>
     *   <li>{@link org.springframework.security.authentication.dao.DaoAuthenticationProvider DaoAuthenticationProvider}
     *      проверяет учетные данные через {@link org.springframework.security.core.userdetails.UserDetailsService UserDetailsService};</li>
     *   <li>При несовпадении хеша пароля (BCrypt) или отсутствии пользователя в БД
     *      выбрасывается {@code BadCredentialsException} с сообщением "Bad credentials";</li>
     *   <li>Исключение пробрасывается вверх до контроллера через стек AuthenticationManager → FilterChain.</li>
     * </ul>
     * </p>
     *
     * <p><strong>Логика обработки и возвращаемый ответ:</strong></p>
     * <ul>
     *   <li>{@link org.springframework.http.ResponseEntity#status(HttpStatusCode)}  ResponseEntity.status(HttpStatus.UNAUTHORIZED)}
     *      устанавливает HTTP-статус <code>401 Unauthorized</code>;</li>
     *   <li>{@link java.util.Map#of Map.of("error", e.getMessage())} создает неизменяемую Map (Java 9+)
     *      с единственным полем {@code error}, содержащим стандартное сообщение Spring Security
     *      (<code>"Bad credentials"</code>);</li>
     * </ul>
     * <blockquote><pre>HTTP/1.1 401 Unauthorized
     * Content-Type: application/json
     * {"error": "Bad credentials"}</pre></blockquote>
     * </p>
     *
     * <p><strong>Параметры метода:</strong></p>
     * <ul>
     *   <li>{@code RuntimeException e} — Spring автоматически инжектит исключение,
     *      выбросившее обработчик. Тип {@code RuntimeException} корректен, так как
     *      {@code BadCredentialsException} наследуется от {@code AuthenticationException} → {@code RuntimeException}.</li>
     * </ul>
     * </p>
     *
     * <p><strong>Тип возвращаемого значения:</strong></p>
     * <ul>
     *   <li>{@link org.springframework.http.ResponseEntity ResponseEntity}&lt;{@link java.util.Map Map}&lt;String,String&gt;&gt;
     *      — стандартная обертка Spring MVC для полного контроля HTTP-ответа:
     *      <ul>
     *        <li>HTTP-статус (401);</li>
     *        <li>HTTP-заголовки (Content-Type: application/json);</li>
     *        <li>JSON-body (Map → Jackson сериализация).</li>
     *      </ul>
     *   </li>
     * </ul>
     * </p>
     *
     * <p><strong>Преимущества специфичного обработчика:</strong></p>
     * <ul>
     *   <li>Точный HTTP-статус 401 вместо generic 500;</li>
     *   <li>Краткое, безопасное сообщение для клиента (не раскрывает детали аутентификации);</li>
     *   <li>Высокий приоритет — никогда не доходит до catch-all обработчика;</li>
     *   <li>Легко расширяется для других Security исключений (DisabledException, LockedException).</li>
     * </ul>
     * </p>
     *
     * @param e {@link org.springframework.security.authentication.BadCredentialsException BadCredentialsException}
     *          из Spring Security AuthenticationManager (автоинжект Spring)
     * @return {@link org.springframework.http.ResponseEntity ResponseEntity} с HTTP 401 и JSON {@code {"error": "Bad credentials"}}
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentialsException(RuntimeException e) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Entity is unauthorized"));
    }
    /**
     * Catch-all обработчик всех остальных исключений (fallback-обработчик).
     *
     * <p>Класс метода помечен аннотацией:
     * <ul>
     *   <li>{@link org.springframework.web.bind.annotation.ExceptionHandler @ExceptionHandler(Exception.class)} —
     *   связывает метод с базовым типом {@link java.lang.Exception Exception}, от которого наследуются
     *   <em>все</em> checked и unchecked исключения Java (RuntimeException, IOException, SQLException и др.).
     *   Spring {@link org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver ExceptionHandlerMethodResolver}
     *   рассматривает этот обработчик как обработчик <strong>наименьшей специфичности</strong>.</li>
     * </ul>
     * </p>
     *
     * <p><strong>Условия срабатывания (НИЗШИЙ приоритет):</strong></p>
     * <ol>
     *   <li>Контроллер выбрасывает любое исключение, <strong>НЕ пойманное более специфичными обработчиками</strong>
     *      (например, не {@code BadCredentialsException}, {@code MethodArgumentNotValidException} и т.д.);</li>
     *   <li>{@link org.springframework.web.servlet.DispatcherServlet DispatcherServlet} передает исключение
     *      {@link org.springframework.web.servlet.HandlerExceptionResolver HandlerExceptionResolver} цепочке;</li>
     *   <li>{@link org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver ExceptionHandlerExceptionResolver}
     *      проходит по всем {@code @RestControllerAdvice} классам и их {@code @ExceptionHandler} методам;</li>
     *   <li><strong>Алгоритм Spring "best match":</strong> если есть обработчики для более специфичных типов
     *      (например, {@code @ExceptionHandler(BadCredentialsException.class)}), они вызываются первыми;</li>
     *   <li>Этот метод вызывается <strong>только</strong> если нет более специфичных совпадений — safety net.</li>
     * </ol>
     * </p>
     *
     * <p><strong>Примеры исключений, которые ловит этот обработчик:</strong></p>
     * <ul>
     *   <li>{@link java.lang.NullPointerException NullPointerException} — разыменование null;</li>
     *   <li>{@link java.lang.IllegalArgumentException IllegalArgumentException} — неверные параметры;</li>
     *   <li>{@link org.springframework.dao.DataAccessException DataAccessException} — ошибки БД (JDBC, JPA);</li>
     *   <li>{@link java.io.IOException IOException} — ошибки ввода-вывода;</li>
     *   <li>Любые кастомные исключения, не покрытые специфичными обработчиками.</li>
     * </ul>
     * </p>
     *
     * <p><strong>Логика обработки и возвращаемый ответ:</strong></p>
     * <ul>
     *   <li>{@link org.springframework.http.ResponseEntity#status(HttpStatusCode)} ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)}
     *      устанавливает стандартный HTTP-статус <code>500 Internal Server Error</code> для серверных ошибок;</li>
     *   <li>{@link java.util.Map#of Map.of()} создает неизменяемую Map с двумя полями:
     *      <ul>
     *        <li>{@code "error": "An error occurred"} — <strong>generic сообщение для клиента</strong>,
     *            безопасное, не раскрывает детали реализации;</li>
     *        <li>{@code "message": e.getMessage()} — <strong>детальное сообщение для логов/дебаггинга</strong>,
     *            содержит stack trace или описание проблемы;</li>
     *      </ul>
     *   </li>
     *   <li>Благодаря {@code @RestControllerAdvice}, Map автоматически сериализуется в JSON:</li>
     * </ul>
     * <blockquote><pre>HTTP/1.1 500 Internal Server Error
     * Content-Type: application/json
     * {
     *   "error": "An error occurred",
     *   "message": "Could not obtain connection to query metadata: ... [полный stack trace]"
     * }</pre></blockquote>
     * </p>
     *
     * <p><strong>Двухуровневая стратегия сообщений (безопасность + отладка):</strong></p>
     * <table border="1">
     *   <tr><th>Поле</th><th>Назначение</th><th>Содержимое</th><th>Безопасность</th></tr>
     *   <tr><td>{@code error}</td><td>Клиент (публичный API)</td><td>Generic текст</td><td>Безопасно</td></tr>
     *   <tr><td>{@code message}</td><td>Логи/дебаггинг</td><td>Stack trace</td><td>Только для разработки</td></tr>
     * </table>
     * В production рекомендуется логировать {@code e} полностью, а клиенту отдавать только {@code error}.
     * </p>
     *
     * <p><strong>Параметры метода:</strong></p>
     * <ul>
     *   <li>{@code Exception e} — Spring автоматически инжектит исключение через
     *      {@link org.springframework.web.method.support.HandlerMethodArgumentResolver HandlerMethodArgumentResolver}.
     *      Точный тип определяется runtime-типом исключения.</li>
     * </ul>
     * </p>
     *
     * <p><strong>Тип возвращаемого значения:</strong></p>
     * <ul>
     *   <li>{@link org.springframework.http.ResponseEntity ResponseEntity}&lt;{@link java.util.Map Map}&lt;String,String&gt;&gt;
     *      — полный контроль HTTP-ответа:
     *      <ul>
     *        <li>HTTP-статус 500;</li>
     *        <li>{@code Content-Type: application/json} (автоопределение);</li>
     *        <li>JSON-body с двумя полями для разных целей.</li>
     *      </ul>
     *   </li>
     * </ul>
     * </p>
     *
     * <p><strong>Архитектурная роль в GlobalExceptionHandler:</strong></p>
     * <ul>
     *   <li><strong>Safety net:</strong> гарантирует, что НИ ОДНО исключение не "протечет" в Tomcat error page;</li>
     *   <li><strong>Консистентность:</strong> все непредвиденные ошибки имеют одинаковый JSON-формат;</li>
     *   <li><strong>Мониторинг:</strong> {@code e.getMessage()} содержит достаточно данных для Sentry/New Relic;</li>
     *   <li><strong>Расширяемость:</strong> при добавлении специфичных обработчиков этот остается последним рубежом.</li>
     * </ul>
     * </p>
     *
     * <p><strong>Рекомендации по улучшению в production:</strong></p>
     * <ul>
     *   <li>Логировать полное исключение: {@code log.error("Unhandled exception", e)};</li>
     *   <li>В продакшене скрывать {@code "message"} поле или заменять на generic текст;</li>
     *   <li>Добавить метрики: {@code meter.increment("exception.unhandled.total") };</li>
     * </ul>
     * </p>
     *
     * @param e Любое {@link java.lang.Exception Exception}, не пойманное специфичными обработчиками
     * @return {@link org.springframework.http.ResponseEntity ResponseEntity} с HTTP 500 и JSON
     *         {@code {"error": "An error occurred", "message": "[детали]"}}
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAllExceptions(Exception e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "An error occurred"
                ));
    }
}
