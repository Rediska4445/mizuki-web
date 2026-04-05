package rf.mizuka.web.application.tracks.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import rf.mizuka.web.application.tracks.database.repository.TrackRepository;
import rf.mizuka.web.application.tracks.models.Track;

import java.security.Principal;

/**
 * MVC‑контроллер, отвечающий за обработку запросов к странице списка треков
 * и загрузке новых аудиотреков.
 *
 * <p>Класс помечен аннотацией {@link org.springframework.stereotype.Controller @Controller},
 * что делает его компонентом Spring MVC, обрабатывающим HTTP‑запросы и возвращающим
 * представления (view) для рендеринга HTML‑страницы через шаблонизатор (например, Thymeleaf).
 * В сочетании с {@link org.springframework.web.bind.annotation.RequestMapping @RequestMapping("/app")}
 * все обработчики запросов этого контроллера маппятся на пути, относящиеся к базовому префиксу
 * {@code /app} (например, GET /app/tracks, POST /app/tracks).</p>
 *
 * <p>Ограничения доступа:
 * <ul>
 *   <li>Страница предназначена только для авторизованных пользователей, поэтому в
 *   типичной конфигурации Spring Security доступ к путям вида {@code /app/**} должен
 *   быть защищён (например, через {@code httpSecurity} или {@code @PreAuthorize}).
 *   Поскольку контроллер возвращает HTML‑контент, безопасность обычно настраивается на уровне
 *   {@code HttpSecurity} (например, требование состояния authenticated для всех запросов
 *   к {@code /app/**}), а не только в методе. Если в коде присутствуют аннотации уровня
 *   метода (например {@link org.springframework.security.access.prepost.PreAuthorize @PreAuthorize}),
 *   они обеспечивают дополнительные проверки прав доступа перед выполнением обработчика.</li>
 *   <li>Параметр типа {@link java.security.Principal Principal} в методе {@link #list(Model, Principal)}
 *   позволяет получить информацию о текущем аутентифицированном пользователе через стандартный
 *   контракт Spring Security, используя имя пользователя (например, в поле {@code principal.getName()}
 *   для отображения в шаблоне).</li>
 * </ul>
 * </p>
 *
 * <p>Механизмы работы на уровне Spring MVC и Web:
 * <ul>
 *   <li>Аннотация {@link org.springframework.web.bind.annotation.GetMapping @GetMapping("/tracks")}
 *   указывает, что метод {@link #list(Model, Principal)} обрабатывает HTTP GET‑запрос к пути
 *   {@code /app/tracks} и отображает HTML‑страницу списка треков. Внутри метода используется
 *   {@code Model} для передачи данных в шаблон:
 *   <ul>
 *     <li>через {@link org.springframework.ui.Model#addAttribute(String, Object) addAttribute}
 *     в модель добавляется список всех треков, загружаемых через {@link rf.mizuka.web.application.tracks.database.repository.TrackRepository TrackRepository#findAll()};
 *     этот вызов делегируется Spring Data JPA‑репозиторию и в итоге превращается в JPA‑запрос к
 *     таблице {@code tracks}, результат которого маппится на набор сущностей {@code Track}.</li>
 *     <li>имя пользователя также добавляется в модель, чтобы шаблон мог отображать
 *     его в интерфейсе (например, в заголовке или профиле пользователя).</li>
 *   </ul>
 *   </li>
 *   <li>Аннотация {@link org.springframework.web.bind.annotation.PostMapping @PostMapping("/tracks")}
 *   указывает, что метод {@link #upload(MultipartFile, RedirectAttributes)} обрабатывает HTTP POST‑запрос,
 *   направленный на загрузку аудиофайла (трека) через форму с {@code enctype="multipart/form-data"}.
 *   В параметре используется тип {@link org.springframework.web.multipart.MultipartFile MultipartFile},
 *   который является стандартным механизмом Spring MVC для обработки загрузки файлов через HTTP.
 *   {@link org.springframework.web.servlet.mvc.support.RedirectAttributes RedirectAttributes}
 *   используется для передачи кратковременных атрибутов (flash attributes) между запросами,
 *   например, сообщения об успешной загрузке, которое будет отображено после редиректа.</li>
 *   <li>После обработки загрузки метод возвращает строку вида {@code "redirect:/app/tracks"},
 *   что указывает Spring MVC выполнить перенаправление (HTTP 302/303) на GET‑путь
 *   списка треков. Это стандартный паттерн Post–Redirect–Get (PRG), предотвращающий
 *   повторную отправку формы при обновлении страницы и гарантирующий, что последующие запросы
 *   являются чистыми GET‑запросами, которые безопасно кешируются и не модифицируют данные.</li>
 * </ul>
 * </p>
 *
 * <p>Интеграция с бизнес‑логикой:
 * <ul>
 *   <li>Компонент {@code TrackRepository} вводится через {@link org.springframework.beans.factory.annotation.Autowired @Autowired},
 *   что позволяет контроллеру делегировать работу с базой данных репозиторию, а не выполнять
 *   операции с {@code EntityManager} или JPA напрямую. Это обеспечивает слабую связность
 *   и соблюдение принципа инверсии управления.</li>
 *   <li>Сервисы авторизации, работающие с объектами типа {@link rf.mizuka.web.application.auth.models.User User},
 *   обычно находятся в соседних слоях; контроллер в данном случае использует только
 *   представление имени пользователя для отображения и не включает сложную логику аутентификации.</li>
 * </ul>
 * </p>
 *
 * <p>Таким образом, класс {@code TracksController}:
 * <ul>
 *   <li>реализует стандартный MVC‑контроллер для отображения списка треков;</li>
 *   <li>поддерживает загрузку новых треков через HTTP POST‑запрос с файлом;</li>
 *   <li>предназначен для использования в контексте авторизованных пользователей,
 *   с соответствующей защитой доступа на уровне Spring Security;</li>
 *   <li>интегрируется с Spring Data JPA‑репозиторием и Spring MVC‑механизмами шаблонов,
 *   редиректов и обработки загрузок файлов.</li>
 * </ul>
 * </p>
 */
@Controller
@RequestMapping("/app")
public class TracksController {
    /**
     * Репозиторий сущности {@link rf.mizuka.web.application.tracks.models.Track},
     * введённый через встроенную поддержку DI в Spring.
     *
     * <p>Поле помечено аннотацией {@link org.springframework.beans.factory.annotation.Autowired @Autowired},
     * что указывает Spring‑контейнеру выполнить инъекцию экземпляра {@code TrackRepository}
     * при создании данного контроллера. В контексте Spring Boot это обычно реализуется
     * как интерфейс, реализованный прокси‑объектом Spring Data JPA, который автоматически
     * предоставляет стандартные методы {@code save}, {@code findAll}, {@code deleteById} и т.п.
     * для работы с таблицей {@code tracks}.</p>
     *
     * <p>Принципы:
     * <ul>
     *   <li>{@code @Autowired} обеспечивает стандартный паттерн Dependency Injection:
     *   контроллер не создаёт репозиторий явно, а получает его уже сконфигурированный
     *   из контекста, что упрощает тестирование и конфигурацию;</li>
     *   <li>в реальности Spring создаёт прокси над интерфейсом {@code TrackRepository},
     *   который делегирует вызовы методов в {@link jakarta.persistence.EntityManager}
     *   и JPA‑провайдер (например Hibernate), преобразуя операции с объектами {@code Track}
     *   в SQL‑запросы и обратно;</li>
     *   <li>использование {@code final} здесь необязательно, но в современных приложениях
     *   рекомендуется делать поля, в которые вводится зависимость, final и использовать
     *   конструктор‑инъекцию, чтобы гарантировать неизменяемость и корректную инициализацию.</li>
     * </ul>
     * </p>
     */
    @Autowired
    private TrackRepository trackRepo;
    /**
     * Обрабатывает HTTP GET‑запрос к пути {@code /app/tracks}.
     *
     * <p>Метод вызывается, когда приходит HTTP GET‑запрос к адресу {@code /app/tracks},
     * который отображает страницу списка треков. Аннотация
     * {@link org.springframework.web.bind.annotation.GetMapping @GetMapping("/tracks")}
     * указывает, что этот обработчик отвечает только на GET‑запросы по пути, базовому
     * для контроллера ({@code /app}), и возвращает имя представления, которое будет
     * отрендерено Spring MVC (например, через Thymeleaf).</p>
     *
     * <p>Параметр {@link org.springframework.ui.Model Model} используется для передачи данных
     * в шаблон (view): в модели добавляется список всех треков, полученных через
     * {@link rf.mizuka.web.application.tracks.database.repository.TrackRepository TrackRepository#findAll()},
     * и имя пользователя, взятое из {@link java.security.Principal Principal#getName()}.
     * Это стандартный подход MVC‑слоя Spring: контроллер подготавливает модель,
     * а шаблон отображает её в HTML‑страницу.</p>
     *
     * <p>Параметр {@link java.security.Principal Principal} представляет информацию
     * о текущем аутентифицированном пользователе, как это определено Spring Security.
     * Он автоматически подставляется в метод, если запрос приходит из аутентифицированного
     * контекста, что позволяет контроллеру использовать имя пользователя в UI‑представлении.</p>
     *
     * @param model модель данных, передаваемая в шаблон; содержит список треков и имя пользователя.
     * @param principal объект, представляющий аутентифицированного пользователя в системе
     *                  с точки зрения Spring Security.
     * @return имя представления (view), используемое {@link org.springframework.web.servlet.ViewResolver ViewResolver}
     *         для отображения страницы списка треков (например, {@code "app/tracks.html"}).
     */
    @GetMapping("/tracks")
    public String list(Model model, Principal principal) {
        model.addAttribute("tracks", trackRepo.findAll());
        model.addAttribute("username", principal.getName());

        return "app/tracks";
    }
    /**
     * Обрабатывает HTTP POST‑запрос для загрузки нового аудиотрека на сервер.
     *
     * <p>Метод вызывается при отправке HTTP POST‑запроса на тот же путь {@code /app/tracks},
     * что и метод GET, но соответствует типу запроса POST и используется для загрузки файла
     * через форму с {@code enctype="multipart/form-data"}. Аннотация
     * {@link org.springframework.web.bind.annotation.PostMapping @PostMapping("/tracks")}
     * гарантирует, что только HTTP POST‑запросы к этому пути будут обработаны этим методом.</p>
     *
     * <p>Параметр {@link org.springframework.web.multipart.MultipartFile MultipartFile}
     * использует стандартный механизм Spring MVC для обработки загрузок файлов через HTTP:
     * файл передаётся как часть тела запроса, а Spring автоматически декодирует его в объект
     * {@code MultipartFile} с метаданными (например, {@code getOriginalFilename()}).
     * Если файл не пустой, метод создаёт новую сущность {@link rf.mizuka.web.application.tracks.models.Track Track},
     * устанавливает в неё имя, равное исходному имени загруженного файла, и сохраняет её
     * через {@link rf.mizuka.web.application.tracks.database.repository.TrackRepository TrackRepository#save(Track)}.</p>
     *
     * <p>После загрузки сообщение об успешном действии (например, {@code "Загружен: ..."})
     * добавляется в объект {@link org.springframework.web.servlet.mvc.support.RedirectAttributes RedirectAttributes}
     * в виде flash attribute, который будет передан в следующий запрос после перенаправления.
     * Это стандартный паттерн Post–Redirect–Get (PRG), предотвращающий повторную
     * отправку формы при обновлении страницы.</p>
     *
     * @param file загружаемый аудиофайл, переданный клиентом через форму;
     *             если файл пустой (например, пользователь ничего не выбрал),
     *             он не обрабатывается, а контроллер сразу выполняет редирект.
     * @param redirect объект атрибутов, передающийся между запросами через перенаправление;
     *                 используется для хранения flash‑атрибутов, таких как сообщения
     *                 об успешной загрузке, которые отображаются на странице списка треков.
     * @return строку перенаправления {@code "redirect:/app/tracks"},
     *         указывающую Spring MVC выполнить HTTP 302/303 на GET‑путь списка треков.
     */
    @PostMapping("/tracks")
    public String upload(@RequestParam("file") MultipartFile file, RedirectAttributes redirect) {
        if (!file.isEmpty()) {
            Track track = new Track();
            track.setName(file.getOriginalFilename());

            trackRepo.save(track);

            redirect.addFlashAttribute("message", "Загружен: " + file.getOriginalFilename());
        }

        return "redirect:/app/tracks";
    }
}