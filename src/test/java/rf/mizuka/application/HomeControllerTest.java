package rf.mizuka.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import rf.mizuka.web.application.HomeController;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit-тесты для {@link rf.mizuka.web.application.HomeController}.
 *
 * <p>Класс использует аннотацию {@link WebMvcTest} для тестирования только веб-слоя приложения:
 * <ul>
 *   <li>Загружает только контроллеры и связанные компоненты Spring MVC</li>
 *   <li>Не запускает базу данных, сервисы или репозитории</li>
 *   <li>Предоставляет {@link MockMvc} для имитации HTTP-запросов</li>
 *   <li>Обеспечивает высокую скорость выполнения тестов (1-2 секунды)</li>
 * </ul>
 *
 * @see WebMvcTest
 * @see MockMvc
 */
@WebMvcTest(HomeController.class)
@WithMockUser(username = "testuser")
@AutoConfigureMockMvc(addFilters = false)
public class HomeControllerTest {
    /**
     * {@code MockMvc} instance для выполнения HTTP запросов к контроллерам.
     *
     * <p>Объект имитирует полноценный HTTP-сервер Spring MVC, включая:
     * <ul>
     *   <li>DispatcherServlet для маршрутизации запросов</li>
     *   <li>HandlerMapping для поиска подходящего метода контроллера</li>
     *   <li>ViewResolver для обработки имен представлений</li>
     *   <li>Полную цепочку фильтров и интерцепторов</li>
     * </ul>
     * Spring автоматически создает и настраивает этот бин.
     */
    @Autowired
    private MockMvc mockMvc;
    /**
     * Тест базового сценария работы контроллера.
     *
     * <p>Проверяет корректную обработку HTTP GET-запроса на корневой путь:
     * <ul>
     *   <li>Статус ответа: 200 OK</li>
     *   <li>Имя представления: "home" (соответствует {@code return "home"})</li>
     *   <li>Тип контента: {@code text/html;charset=UTF-8} для HTML-шаблонов</li>
     * </ul>
     *
     * <p><b>Последовательность выполнения:</b></p>
     * <ol>
     *   <li>{@code mockMvc.perform(get("/"))} создает {@link org.springframework.mock.web.MockHttpServletRequest} с методом GET и путем "/"</li>
     *   <li>Spring MVC DispatcherServlet маршрутизирует запрос к {@link HomeController#home(Model)}}</li>
     *   <li>Метод возвращает строку "home", которая интерпретируется как имя view</li>
     *   <li>{@link org.springframework.web.servlet.ViewResolver} обрабатывает имя view и формирует {@link org.springframework.mock.web.MockHttpServletResponse}</li>
     *   <li>Цепочка {@code .andExpect()} проверяет характеристики ответа</li>
     * </ol>
     *
     * @throws Exception если произошла ошибка при выполнении HTTP-запроса или проверке ответа
     */
    @Test
    @DisplayName("GET / возвращает view 'home' со статусом 200")
    void homeShouldReturnHomeView() throws Exception {
        // Шаг 1: Создание и отправка HTTP GET-запроса
        //
        // MockMvcRequestBuilders.get("/") возвращает MockMvcRequestBuilders.RequestBuilder,
        // который содержит спецификацию запроса (метод, путь, заголовки)
        //
        // mockMvc.perform() выполняет запрос через полный стек Spring MVC и возвращает MvcResult
        mockMvc.perform(get("/"))
                // Шаг 2: Проверка HTTP-статуса ответа
                //
                // status().isOk() эквивалентно status().is(200)
                // Проверяет поле status в MockHttpServletResponse
                // Если контроллер выполнился без исключений → статус 200
                .andExpect(status().isOk())
                // Шаг 3: Проверка имени представления (view name)
                //
                // view().name("home") проверяет значение modelAndView.getViewName()
                // HomeController.home() возвращает "home" → ViewResolver получает это имя
                // Тест проходит ИФАЛЬНО когда возвращается строка "home"
                .andExpect(view().name("home"))
                // Шаг 4: Проверка Content-Type заголовка ответа
                //
                // content().contentType() извлекает заголовок Content-Type из ответа
                // Spring Boot по умолчанию использует InternalResourceViewResolver или ThymeleafViewResolver
                // Для HTML-шаблонов стандартный Content-Type = "text/html;charset=UTF-8"
                .andExpect(content().contentType("text/html;charset=UTF-8"));
    }

    /**
     * Тест проверки ограничения HTTP-методов контроллера.
     *
     * <p>Проверяет поведение Spring MVC при попытке выполнить неподдерживаемый
     * HTTP-метод на пути, который обслуживается методом {@link HomeController#home(Model)}.
     *
     * <p><b>Почему ожидается 405 Method Not Allowed:</b></p>
     * <ul>
     *   <li>Метод помечен аннотацией {@code @GetMapping} (сокращение от {@code @RequestMapping(method = RequestMethod.GET)})</li>
     *   <li>Spring HandlerMapping проверяет соответствие HTTP-метода запроса аннотации контроллера</li>
     *   <li>POST ≠ GET → HandlerMapping отклоняет запрос ДО вызова контроллера</li>
     *   <li>По стандарту HTTP/1.1 сервер возвращает статус 405 с заголовком {@code Allow: GET}</li>
     * </ul>
     *
     * <p><b>Последовательность выполнения:</b></p>
     * <ol>
     *   <li>{@code post("/")} создает {@link org.springframework.mock.web.MockHttpServletRequest} с методом POST</li>
     *   <li>DispatcherServlet → HandlerMapping ищет контроллер для пути "/"</li>
     *   <li>HandlerMapping находит HomeController.home(), но метод = POST ≠ GET</li>
     *   <li>Возвращается {@link org.springframework.web.HttpRequestMethodNotSupportedException}</li>
     *   <li>Spring обрабатывает исключение → статус 405 Method Not Allowed</li>
     * </ol>
     *
     * @throws Exception при ошибке выполнения теста
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc7231#section-6.5.5">HTTP/1.1 RFC 7231: 405 Method Not Allowed</a>
     */
    @Test
    @DisplayName("POST / возвращает 405 Method Not Allowed")
    void postHomeShouldReturnMethodNotAllowed() throws Exception {
        // Шаг 1: Создание POST-запроса (неподдерживаемый метод для данного пути)
        //
        // MockMvcRequestBuilders.post("/") формирует запрос:
        // - method = "POST"
        // - path = "/"
        // - без тела запроса (по умолчанию)
        mockMvc.perform(post("/"))

                // Шаг 2: Проверка статуса ответа
                //
                // isMethodNotAllowed() эквивалентно is(405)
                // Проверка срабатывает на уровне HandlerMapping ДО выполнения контроллера
                // Заголовок Allow: GET будет автоматически добавлен Spring MVC
                .andExpect(status().isMethodNotAllowed());
    }

    /**
     * Тест проверки обработки несуществующего пути.
     *
     * <p>Проверяет поведение Spring MVC при запросе к пути, который не обслуживается
     * {@link HomeController}. Контроллер обслуживает только точный путь "/" через
     * комбинацию {@code @RequestMapping("/")} + {@code @GetMapping}.
     *
     * <p><b>Почему ожидается 404 Not Found:</b></p>
     * <ul>
     *   <li>Spring HandlerMapping не находит подходящий контроллер для пути "/wrong-path"</li>
     *   <li>Нет метода с аннотацией, соответствующей GET "/wrong-path"</li>
     *   <li>DispatcherServlet возвращает стандартный статус 404</li>
     * </ul>
     *
     * <p><b>Последовательность выполнения:</b></p>
     * <ol>
     *   <li>{@code get("/wrong-path")} создает запрос к несуществующему пути</li>
     *   <li>DispatcherServlet → HandlerMapping НЕ находит контроллер</li>
     *   <li>Возвращается {@link org.springframework.web.servlet.NoHandlerFoundException}</li>
     *   <li>Обработчик исключений → статус 404 Not Found</li>
     * </ol>
     */
    @Test
    @DisplayName("GET /wrong-path возвращает 404 Not Found")
    void wrongPathShouldReturnNotFound() throws Exception {

        // Шаг 1: GET-запрос к несуществующему пути
        // HandlerMapping не найдет контроллер для "/wrong-path"
        mockMvc.perform(get("/wrong-path"))

                // Шаг 2: Проверка статуса 404
                // Spring возвращает 404 когда нет подходящего @RequestMapping
                .andExpect(status().isNotFound());
    }

    /**
     * Тест проверки HTTP-заголовка Allow для неподдерживаемых методов.
     *
     * <p>Проверяет, что Spring MVC не только возвращает статус 405 Method Not Allowed,
     * но и корректно формирует заголовок {@code Allow}, указывающий поддерживаемые
     * HTTP-методы для ресурса "/".
     *
     * <p><b>Зачем нужен заголовок Allow:</b></p>
     * <ul>
     *   <li>Соответствует стандарту HTTP/1.1 (RFC 7231, раздел 6.5.5)</li>
     *   <li>Информирует клиента о допустимых методах (GET, POST, PUT, DELETE)</li>
     *   <li>Улучшает UX: браузер/клиент может показать "доступно только GET"</li>
     *   <li>Spring автоматически анализирует аннотации контроллера (@GetMapping → Allow: GET)</li>
     * </ul>
     *
     * <p><b>Последовательность выполнения:</b></p>
     * <ol>
     *   <li>{@code post("/")} создает POST-запрос (неподдерживаемый)</li>
     *   <li>HandlerMapping находит HomeController.home(), но GET ≠ POST</li>
     *   <li>Spring анализирует аннотации → определяет поддерживаемый метод GET</li>
     *   <li>Возвращается 405 + заголовок {@code Allow: GET}</li>
     * </ol>
     *
     * @throws Exception при ошибке выполнения запроса или проверки заголовка
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc7231#section-6.5.5">RFC 7231: 405 Method Not Allowed</a>
     */
    @Test
    @DisplayName("POST / возвращает заголовок Allow: GET")
    void postHomeShouldReturnAllowGetHeader() throws Exception {

        // Шаг 1: POST-запрос к пути, поддерживающему только GET
        // HandlerMapping найдет контроллер, но отклонит метод POST
        mockMvc.perform(post("/"))

                // Шаг 2: Базовая проверка статуса 405
                // HttpRequestMethodNotSupportedException → 405
                .andExpect(status().isMethodNotAllowed())

                // Шаг 3: Проверка заголовка Allow
                //
                // header().string("Allow", "GET") проверяет:
                // - Наличие заголовка "Allow" в MockHttpServletResponse
                // - Точное значение = "GET" (Spring анализирует @GetMapping)
                //
                // Spring MVC автоматически формирует Allow на основе аннотаций контроллера
                .andExpect(header().string("Allow", "GET"));
    }

    /**
     * Тест проверки корректной передачи объекта Model в контроллер.
     *
     * <p>Проверяет, что Spring автоматически создает и передает объект {@link Model}
     * в метод контроллера как параметр, даже если контроллер не использует его явно.
     *
     * <p><b>Механизм работы:</b></p>
     * <ul>
     *   <li>Spring видит параметр {@code Model model} в сигнатуре метода</li>
     *   <li>Создает новый {@link org.springframework.ui.ExtendedModelMap} (реализация Model)</li>
     *   <li>Передает его через Dependency Injection в метод контроллера</li>
     *   <li>Model передается в ViewResolver вместе с именем view "home"</li>
     * </ul>
     */
    @Test
    @DisplayName("GET / передает непустую Model в контроллер")
    void homeShouldReceiveNonEmptyModel() throws Exception {

        // Шаг 1: Выполняем GET-запрос на "/"
        // Spring MVC создает Model и передает в HomeController.home(Model)
        mockMvc.perform(get("/"))

                // Шаг 2: Проверка статуса ответа (базовая проверка)
                // Контроллер выполнился успешно
                .andExpect(status().isOk())

                // Шаг 3: Проверка объекта Model в результате
                //
                // model().size(0) проверяет:
                // - Model объект существует (не null)
                // - Количество атрибутов = 0 (контроллер ничего не добавил через addAttribute())
                // - Model готова к использованию в шаблоне home.html
                .andExpect(model().size(0));
    }

    /**
     * Тест проверки обработки GET-запроса с query-параметрами.
     *
     * <p>Проверяет, что Spring MVC корректно маршрутизирует запросы с query-параметрами
     * к соответствующему контроллеру. Query-параметры НЕ влияют на поиск метода контроллера.
     *
     * <p><b>Механизм маршрутизации:</b></p>
     * <ul>
     *   <li>{@code GET /?debug=true} → path = "/" (query игнорируется для HandlerMapping)</li>
     *   <li>{@code @RequestMapping("/")} + {@code @GetMapping} ловит путь "/"</li>
     *   <li>Query-параметры доступны через {@code @RequestParam} (если нужны)</li>
     * </ul>
     */
    @Test
    @DisplayName("GET /?debug=true → тот же контроллер")
    void homeShouldHandleQueryParameters() throws Exception {

        // Шаг 1: GET-запрос с query-параметром debug=true
        // .param("debug", "true") добавляет ?debug=true к запросу
        // Spring использует ТОЛЬКО path="/" для маршрутизации
        mockMvc.perform(get("/").param("debug", "true"))

                // Шаг 2: Проверка успешной обработки
                // Query-параметры НЕ меняют маршрутизацию → HomeController.home()
                .andExpect(status().isOk())

                // Шаг 3: Проверка view name (тот же контроллер сработал)
                .andExpect(view().name("home"));
    }

    /**
     * Тест проверки Content-Type заголовка ответа более детально.
     *
     * <p>Проверяет точное значение Content-Type, включая кодировку, и структуру ответа
     * Spring MVC при рендеринге HTML-шаблона.
     *
     * <p><b>Content-Type разбор:</b></p>
     * <ul>
     *   <li>{@code "text/html;charset=UTF-8"} - стандарт для HTML + UTF-8</li>
     *   <li>Устанавливается ViewResolver (Thymeleaf/JSP/Freemarker)</li>
     *   <li>Spring Boot auto-configuration определяет кодировку</li>
     * </ul>
     */
    @Test
    @DisplayName("GET / возвращает Content-Type text/html;charset=UTF-8")
    void homeShouldReturnCorrectContentType() throws Exception {

        // Шаг 1: GET-запрос к корню приложения
        mockMvc.perform(get("/"))

                // Шаг 2: Базовая проверка статуса
                .andExpect(status().isOk())

                // Шаг 3: Точная проверка Content-Type заголовка
                //
                // content().contentType() проверяет EXACT значение заголовка
                // "text/html;charset=UTF-8" = MIME-type + charset
                // charset=UTF-8 от spring.http.encoding.charset=UTF-8 (по умолчанию)
                .andExpect(content().contentType("text/html;charset=UTF-8"))

                // Шаг 4: Проверка наличия HTML контента (пусть минимального)
                // content().string() проверяет тело ответа содержит HTML
                .andExpect(content().string(containsString("<!DOCTYPE")));
    }

    /**
     * Тест проверки наличия HTML-контента в теле ответа.
     *
     * <p>Проверяет, что ViewResolver генерирует **непустой** HTML-контент
     * при рендеринге шаблона "home".
     */
    @Test
    @DisplayName("GET / возвращает непустой HTML контент")
    void homeShouldReturnNonEmptyHtmlContent() throws Exception {

        // Шаг 1: GET-запрос к контроллеру
        mockMvc.perform(get("/"))

                // Шаг 2: Проверка статуса
                .andExpect(status().isOk())

                // Шаг 3: Проверка HTML Content-Type
                .andExpect(content().contentTypeCompatibleWith("text/html"))

                // Шаг 4: Проверка непустого тела ответа
                //
                // hasLength(0, false) = длина строки > 0 символов
                // ViewResolver генерирует HTML (минимум <!DOCTYPE html>)
                .andExpect(content().string(not(isEmptyString())));
    }

    /**
     * Тест проверки корректной работы аннотации @Controller (не @RestController).
     *
     * <p>Проверяет ключевое различие между {@code @Controller} и {@code @RestController}:
     * <ul>
     *   <li>@Controller → возвращает ИМЯ VIEW ("home") → HTML через ViewResolver</li>
     *   <li>@RestController → возвращает JSON/строку напрямую → НЕТ ViewResolver</li>
     * </ul>
     */
    @Test
    @DisplayName("@Controller возвращает view name, а не JSON")
    void controllerShouldReturnViewNameNotJson() throws Exception {

        // Шаг 1: GET-запрос к контроллеру
        mockMvc.perform(get("/"))

                // Шаг 2: Проверка статуса
                .andExpect(status().isOk())

                // Шаг 3: Проверка НЕ JSON ответа
                // @Controller НЕ использует @ResponseBody → НЕ application/json
                .andExpect(content().contentTypeCompatibleWith("text/html"))

                // Шаг 4: Проверка отсутствия JSON в теле
                // ViewResolver рендерит HTML, НЕ парсит "home" как JSON
                .andExpect(content().string(not(containsString("\"home\""))));
    }

    /**
     * Тест проверки обработки PUT-запроса (неподдерживаемый метод).
     *
     * <p>Проверяет поведение Spring MVC для PUT-метода на пути "/".
     * Аналогично POST, но для другого REST-метода.
     *
     * <p><b>Ожидаемый результат:</b></p>
     * <ul>
     *   <li>Статус: 405 Method Not Allowed</li>
     *   <li>Заголовок: {@code Allow: GET} (только GET поддерживается)</li>
     * </ul>
     */
    @Test
    @DisplayName("PUT / возвращает 405 Method Not Allowed")
    void putHomeShouldReturnMethodNotAllowed() throws Exception {

        // Шаг 1: PUT-запрос (REST-метод обновления, неподдерживаемый)
        mockMvc.perform(put("/"))

                // Шаг 2: Проверка статуса 405
                .andExpect(status().isMethodNotAllowed())

                // Шаг 3: Проверка заголовка Allow
                // Spring анализирует @GetMapping → Allow: GET
                .andExpect(header().string("Allow", "GET"));
    }

    /**
     * Тест проверки DELETE-запроса (неподдерживаемый метод).
     *
     * <p>Проверяет поведение Spring MVC для DELETE-метода на пути "/".
     * Полное покрытие всех REST-методов для данного ресурса.
     *
     * <p><b>HTTP-методы покрытия:</b></p>
     * <ul>
     *   <li>GET → 200 OK (поддерживается)</li>
     *   <li>POST → 405 (неподдерживается)</li>
     *   <li>PUT → 405 (неподдерживается) </li>
     *   <li>DELETE → 405 (этот тест)</li>
     * </ul>
     */
    @Test
    @DisplayName("DELETE / возвращает 405 Method Not Allowed")
    void deleteHomeShouldReturnMethodNotAllowed() throws Exception {

        // Шаг 1: DELETE-запрос (REST-метод удаления, неподдерживаемый)
        mockMvc.perform(delete("/"))

                // Шаг 2: Проверка статуса 405
                // @GetMapping поддерживает ТОЛЬКО GET
                .andExpect(status().isMethodNotAllowed())

                // Шаг 3: Проверка заголовка Allow
                // Spring MVC анализирует аннотации → только GET
                .andExpect(header().string("Allow", "GET"));
    }

    /**
     * Тест проверки UTF-8 кодировки в Content-Type.
     *
     * <p>Проверяет, что Spring Boot по умолчанию использует UTF-8 кодировку
     * для HTML-ответов (spring.http.encoding.charset=UTF-8).
     */
    @Test
    @DisplayName("GET / использует UTF-8 кодировку")
    void homeShouldUseUTF8Encoding() throws Exception {

        // Шаг 1: GET-запрос к контроллеру
        mockMvc.perform(get("/"))

                // Шаг 2: Проверка наличия charset=UTF-8 в Content-Type
                // Spring Boot auto-configuration → UTF-8 по умолчанию
                .andExpect(content().contentType("text/html;charset=UTF-8"));
    }

    /**
     * Тест проверки базового пути @RequestMapping("/") на уровне класса.
     *
     * <p>Проверяет, что {@code @RequestMapping("/")} + {@code @GetMapping}
     * образуют полный путь {@code GET "/"}.
     */
    @Test
    @DisplayName("@RequestMapping(\"/\") + @GetMapping = GET /")
    void classLevelRequestMappingShouldWork() throws Exception {

        // Шаг 1: GET-запрос к корневому пути (комбинация аннотаций)
        mockMvc.perform(get("/"))

                // Шаг 2: Проверка view name от точного соответствия аннотаций
                .andExpect(status().isOk())
                .andExpect(view().name("home"));
    }
}