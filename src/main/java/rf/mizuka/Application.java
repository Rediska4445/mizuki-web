package rf.mizuka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Главный класс приложения Mizuka — WEB‑сервер для Mizuka.
 *
 * <p>При старте этого класса Spring Boot запускает встроенный веб‑сервер (Tomcat по умолчанию),
 * инициализирует Spring-контейнер (ApplicationContext) и автоматически конфигурирует многие компоненты
 * на основе зависимостей (например, spring-boot-starter-web, spring-boot-starter-data-jpa и т.д.).</p>
 *
 * <p>Основные принципы, которые задействованы:
 * <ul>
 *   <li><b>IoC (Inversion of Control)</b> — Spring берёт на себя создание объектов и управление их жизненным циклом,
 *   а вы получаете их через контейнер (через @Autowired или ApplicationContext). Это избавляет от ручного new и
 *   связывает компоненты в единое приложение.</li>
 *   <li><b>DI (Dependency Injection)</b> — зависимости (сервисы, контроллеры, репозитории) не создают зависимости внутри себя,
 *   а “внедряются” фреймворком, что делает код более гибким и легко тестируемым.</li>
 *   <li><b>Auto‑configuration</b> — Spring Boot анализирует зависимости в classpath и файлы конфигурации (application.properties/yaml),
 *   и автоматически создаёт готовые к использованию бины: DispatcherServlet, DataSource, EntityManagerFactory, TransactionManager и т.п.</li>
 *   <li><b>Component scanning</b> — по умолчанию Spring сканирует пакет rf.mizuka и его подпакеты на наличие компонентов
 *   с аннотациями @Component, @Service, @Repository, @Controller и т.д. и регистрирует их как бины в контексте.</li>
 * </ul>
 * </p>
 *
 * <p>Поэтому класс Application должен находиться в корне пакета (rf.mizuka), чтобы сканирование касалось всего приложения.
 * Если бы он был глубже (например, rf.mizuka.config), то могли бы не попасть_scan_xxx_пакеты.</p>
 *
 * <p>Сама аннотация {@link org.springframework.boot.autoconfigure.SpringBootApplication}
 * — это комбинация:
 * <ul>
 *   <li>{@link org.springframework.context.annotation.Configuration} — помечает класс как класс конфигурации Spring;</li>
 *   <li>{@link org.springframework.boot.autoconfigure.EnableAutoConfiguration} — включает автонастройку на основе зависимостей;</li>
 *   <li>{@link org.springframework.context.annotation.ComponentScan} — включает поиск компонентов, начиная с пакета rf.mizuka.</li>
 * </ul>
 * </p>
 *
 * <p>Таким образом, через один класс Application и одну аннотацию вы поднимаете:
 * <ul>
 *   <li>Spring‑контекст с управлением всеми бинами;</li>
 *   <li>встроенный веб‑сервер (Tomcat/Jetty/Undertow) с готовым DispatcherServlet и MVC‑конфигурацией;</li>
 *   <li>автоматическую конфигурацию БД, транзакций, безопасности и т.д., если соответствующие стартеры есть в зависимостях.</li>
 * </ul>
 * </p>
 *
 * <p>Запуск приложения:
 * <ul>
 *   <li>через IDE: запуск метода main;</li>
 *   <li>через консоль: {@code java -jar mizuka-webserver.jar} (если в проекте используется spring-boot-maven-plugin или gradle‑plugin).</li>
 * </ul>
 * </p>
 */
@SpringBootApplication
public class Application {
	/**
	 * Точка входа JVM в приложение Mizuka WEB‑сервер.
	 *
	 * <p>Метод вызывает
	 * {@link org.springframework.boot.SpringApplication#run(Class, String[]) SpringApplication.run(Application.class, args)},
	 * который:
	 * <ul>
	 *   <li>создаёт новый Spring Boot-контекст (реализацию ApplicationContext);</li>
	 *   <li>читает classpath и файлы конфигурации (application.properties / application.yml);</li>
	 *   <li>находит все классы с аннотациями @Configuration, @Component, @Controller, @Service и т.д.;
	 *   распознаёт их как бины и строит граф зависимостей;</li>
	 *   <li>запускает auto‑configuration на основе зависимостей (например, если есть spring-boot-starter-web,
	 *   он автоматически регистрирует DispatcherServlet, resource handlers, message source и т.п.);</li>
	 *   <li>если приложение имеет тип web (в зависимости от стартера), поднимает встроенный веб‑сервер
	 *   и “прикрепляет” к нему Spring MVC‑ядро; при этом сервер слушает порт 8080 по умолчанию
	 *   (переопределяется через application.properties / environment‑переменные).</li>
	 * </ul>
	 * </p>
	 *
	 * <p>После успешного запуска контекста:
	 * <ul>
	 *   <li>все контроллеры становятся доступны по HTTP‑эндпоинтам (например, /api/...);</li>
	 *   <li>сервисы и репозитории можно использовать через @Autowired или через контекст;</li>
	 *   <li>вся логика старта/инициализации данных (Scheduled, CommandLineRunner, ApplicationRunner,
	 *   ApplicationListener и т.п.) может быть подключена через Spring‑механизмы.</li>
	 * </ul>
	 * </p>
	 *
	 * <p>Таким образом, именно этот класс и метод main фактически:
	 * <ul>
	 *   <li>являются “корнем” всего приложения;</li>
	 *   <li>определяют, где Spring начинает сканировать компоненты;</li>
	 *   <li>отвечают за запуск встроенного сервера и всей инфраструктуры Spring Boot.</li>
	 * </ul>
	 * </p>
	 *
	 * @param args аргументы командной строки (обычно не используются, но Spring Boot может их применять
	 *             для конфигурации через свойства командной строки, например --server.port=8081).
	 */
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
