package rf.mizuka.application.tracks.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import rf.mizuka.web.application.tracks.controllers.TracksController;
import rf.mizuka.web.application.tracks.database.repository.TrackRepository;
import rf.mizuka.web.application.tracks.models.Track;

import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тестовый класс для {@link TracksController}.
 *
 * <p>Использует <b>@WebMvcTest</b> для изолированного тестирования MVC-слоя:
 * <ul>
 *   <li>Загружает только контроллер + связанные компоненты (ViewResolver, etc)</li>
 *   <li>НЕ загружает полное Spring Boot приложение (быстрее!)</li>
 *   <li>Автоматически настраивает MockMvc</li>
 * </ul></p>
 *
 * <p><b>Зависимости:</b></p>
 * <ul>
 *   <li>{@code @Autowired MockMvc mockMvc} — для симуляции HTTP-запросов</li>
 *   <li>{@code @MockBean TrackRepository trackRepo} — мок репозитория
 *       (заменяет реальный bean в контексте теста)</li>
 * </ul>
 *
 * <p>Тестирует: GET/POST {@code /app/tracks}, Model attributes, редиректы, flash-сообщения.</p>
 */
@WebMvcTest(TracksController.class)
class TracksControllerTest {
    /**
     * MockMvc для симуляции HTTP-запросов к контроллеру.
     *
     * <p>Автоматически настроен @WebMvcTest. Поддерживает:
     * <ul>
     *   <li>GET, POST, multipart/form-data</li>
     *   <li>Моки Security (Principal, @WithMockUser)</li>
     *   <li>Проверки Model, View, Flash attributes, статусов</li>
     * </ul></p>
     */
    @Autowired
    private MockMvc mockMvc;
    /**
     * Мок TrackRepository, заменяющий реальный bean в тестовом контексте.
     *
     * <p><b>@MockBean</b> (не @Mock!):
     * <ul>
     *   <li>Создаёт мок И заменяет им реальный @Repository в Spring контексте</li>
     *   <li>@Mock — только для обычных unit-тестов без Spring</li>
     * </ul>
     * Используется для: {@code when(trackRepo.findAll()).thenReturn(...)}
     */
    @MockitoBean
    private TrackRepository trackRepo;

    /**
     * Обрабатывает HTTP GET‑запрос к пути {@code /app/tracks} для отображения страницы списка треков.
     *
     * <p>Метод вызывается при GET‑запросе к адресу {@code /app/tracks} и возвращает имя представления
     * {@code "app/tracks"} для рендеринга HTML‑страницы через Thymeleaf или другой шаблонизатор.
     * Аннотация {@link org.springframework.web.bind.annotation.GetMapping @GetMapping("/tracks")}
     * маппит запросы на базовый путь контроллера ({@code /app}).</p>
     *
     * <p><b>Обработка данных:</b></p>
     * <ul>
     *   <li>Загружает все треки из базы данных через {@link TrackRepository#findAll()},
     *       результат добавляется в модель под ключом {@code "tracks"} для отображения в шаблоне.</li>
     *   <li>Извлекает имя текущего аутентифицированного пользователя из {@link Principal#getName()}.
     *       Если имя равно {@code "default"} (случай тестов или дефолтного пользователя), используется
     *       как есть; в остальных случаях также используется напрямую. Результат добавляется в модель
     *       под ключом {@code "username"} для персонализации интерфейса.</li>
     * </ul>
     *
     * <p><b>Безопасность:</b> Метод ожидает аутентифицированного пользователя (Principal не null).
     * В реальном приложении доступ к {@code /app/**} защищён Spring Security. При отсутствии Principal
     * метод упадёт с NPE — безопасность должна обеспечиваться на уровне фильтров.</p>
     *
     * <p><b>MVC‑поток:</b> Контроллер → Model (tracks, username) → ViewResolver → Thymeleaf →
     * {@code app/tracks.html} с данными для таблицы/списка треков и приветствием пользователя.</p>
     *
     * @return логическое имя представления {@code "app/tracks"}
     * @throws NullPointerException если {@code principal == null} (неавторизованный доступ)
     * @see TrackRepository#findAll()
     */
    @Test
    void list_WithDefaultUser_ShouldReturnTracksPage() throws Exception {
        // Given
        List<Track> mockTracks = List.of(
                new Track(),
                new Track()
        );
        when(trackRepo.findAll()).thenReturn(mockTracks);

        // When & Then
        mockMvc.perform(get("/app/tracks")
                        .principal(() -> "default"))
                .andExpect(status().isOk())
                .andExpect(view().name("app/tracks"))
                .andExpect(model().attribute("tracks", mockTracks))
                .andExpect(model().attribute("username", "default"));

        verify(trackRepo, times(1)).findAll();
    }
    /**
     * Проверяет успешную загрузку валидного аудиофайла через POST {@code /app/tracks}.
     *
     * <p><b>Сценарий:</b> Пользователь отправляет multipart/form-data с непустым файлом.
     * Ожидается: создание Track с именем файла, сохранение в БД, редирект с flash-сообщением.</p>
     *
     * <p><b>Тестовые данные:</b></p>
     * <ul>
     *   <li>{@code MockMultipartFile}: имя "test-track.mp3", MIME "audio/mpeg", размер > 0</li>
     *   <li>Мок {@code trackRepo.save()} возвращает сохранённый Track</li>
     * </ul>
     *
     * <p><b>Проверки:</b></p>
     * <ul>
     *   <li>HTTP 302 + редирект на {@code /app/tracks} (PRG паттерн)</li>
     *   <li>Flash attribute {@code "message"} = "Загружен: test-track.mp3"</li>
     *   <li>Один вызов {@code trackRepo.save(Track)} с правильным именем</li>
     * </ul>
     *
     * <p><b>Предусловия:</b> {@code @WithMockUser} обеспечивает аутентификацию (хотя метод upload не использует Principal).
     * CSRF отключён в тестовой конфигурации или не требуется для multipart.</p>
     *
     * @throws Exception если MockMvc или мокинг не сработает
     */
    @Test
    @WithMockUser
    void upload_ValidFile_ShouldSaveTrackAndRedirectWithMessage() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-track.mp3",
                "audio/mpeg",
                "audio content".getBytes()
        );
        Track mockTrack = new Track();
        mockTrack.setName("test-track.mp3");
        when(trackRepo.save(any(Track.class))).thenReturn(mockTrack);

        // When & Then
        mockMvc.perform(multipart("/app/tracks")
                        .file(file)
                        .contentType("multipart/form-data"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/app/tracks"))
                .andExpect(flash().attributeCount(1))
                .andExpect(flash().attribute("message", "Загружен: test-track.mp3"));

        verify(trackRepo, times(1)).save(argThat(track ->
                "test-track.mp3".equals(track.getName())));
    }
}
