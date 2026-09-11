package rf.mizuka.application.tracks.service;

import io.minio.MinioClient;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.multipart.MultipartFile;
import rf.mizuka.web.application.brokers.audio.AudioStreamConsumer;
import rf.mizuka.web.application.brokers.audio.AudioStreamProducer;
import rf.mizuka.web.application.brokers.tracks.TrackConsumer;
import rf.mizuka.web.application.brokers.tracks.TrackProducer;
import rf.mizuka.web.application.configurations.brokers.KafkaConfig;
import rf.mizuka.web.application.configurations.storage.StorageConfig;
import rf.mizuka.web.application.database.entities.media.authors.Author;
import rf.mizuka.web.application.database.entities.media.tracks.Track;
import rf.mizuka.web.application.database.repository.media.authors.AuthorRepository;
import rf.mizuka.web.application.database.repository.media.tracks.TrackRepository;
import rf.mizuka.web.application.services.color.ColorService;
import rf.mizuka.web.application.services.image.ImageService;
import rf.mizuka.web.application.services.storage.StorageService;
import rf.mizuka.web.application.services.tracks.TrackService;

import java.awt.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = "classpath:settings-test.properties")
@Transactional
@Rollback
@ExtendWith(MockitoExtension.class)
public class TrackServiceTest
{
    @MockitoBean
    private TrackService trackService;

    @MockitoBean
    private TrackRepository trackRepository;

    @MockitoBean
    private MinioClient minioClient;

    @MockitoBean
    private StorageConfig storageConfig;

    @MockitoBean
    private AudioStreamConsumer audioStreamConsumer;

    @MockitoBean
    private AudioStreamProducer audioStreamProducer;

    @MockitoBean
    private KafkaConfig kafkaConfig;

    @MockitoBean
    private TrackConsumer trackConsumer;

    @MockitoBean
    private TrackProducer trackProducer;

    @MockitoBean
    private StorageService storageService;

    @MockitoBean
    private ImageService imageService;

    @MockitoBean
    private AuthorRepository authorRepository;

    @MockitoBean
    private ColorService colorService;

    private static String convertColorToHex(java.awt.Color color)
    {
        int rgbWithoutAlpha = color.getRGB() & 0x00FFFFFF;
        String hexString = Integer.toHexString(rgbWithoutAlpha).toUpperCase();
        String paddedHex = String.format("%6s", hexString).replace(' ', '0');

        return "#" + paddedHex;
    }

    @BeforeEach
    void setUp()
            throws Exception
    {
        trackRepository = Mockito.mock(TrackRepository.class);
        storageService = Mockito.mock(StorageService.class);
        imageService = Mockito.mock(ImageService.class);
        authorRepository = Mockito.mock(AuthorRepository.class);
        colorService = Mockito.mock(ColorService.class);

        trackService = new TrackService(
                imageService,
                null,
                trackRepository,
                authorRepository,
                colorService,
                storageService,
                null,
                true
        );
    }

    @Test
    void saveTrack_ShouldSaveToDatabase()
            throws Exception
    {
        Track track = new Track();
        track.setId(1L);
        track.setName("Test Track");
        track.setTitle("Track");
        track.setAuthors(new HashSet<>(Set.of(new Author("test"))));
        track.setDuration(Duration.ofSeconds(100));
        track.setPicturePath("Hello");
        track.setColor(convertColorToHex(Color.WHITE));

        MultipartFile mockFile = new MockMultipartFile(
                "file",
                "test-picture.mp3",
                "audio/mpeg",
                "FakeImageContent".getBytes()
        );

        when(trackRepository.existsById(any()))
                .thenAnswer(e -> true);
        when(imageService.getDefaultMusicImage())
                .thenAnswer(e -> new byte[] {});
        when(authorRepository.buildOrGet(any()))
                .thenAnswer(e -> new Author("test"));
        when(storageService.uploadTrack(any()))
                .thenAnswer(e -> "url");

        when(trackService.saveTrack(track, mockFile)).thenAnswer(e -> track);

        Track savedTrack = trackService.saveTrack(track, mockFile);

        assertThat(savedTrack.getId()).isNotNull();

        boolean exists = trackRepository.existsById(savedTrack.getId());

        assertThat(exists).isTrue();
    }


    /**
     * existsByTitleAndExactAuthors <u>should</u> be return false,
     * if to him transferred null or empty data
     * */
    @Test
    void existsByTitleAndExactAuthors_ShouldReturnFalse()
    {
        assertThat(trackService.existsByTitleAndExactAuthors(
                null, Set.of()
        )).isFalse();
    }

    @Test
    public void existsByTitleAndExactAuthors_ShouldReturnTrue_WhenTitleAndAllAuthorsMatchExactly()
    {
        Track tr = new Track();
        tr.setName("TestTrack");
        tr.setTitle("TestTrackTitle");

        final String fAtr = "TestAuthor";
        tr.setAuthors(Set.of(new Author(fAtr)));

        List<Track> test = new ArrayList<>(List.of(tr));

        // Prepare mock in repository
        when(trackRepository.findTracksByTitleAndFirstAuthor(
                tr.getTitle(), fAtr
        )).thenReturn(test);

        // Assert
        assertThat(trackService.existsByTitleAndExactAuthors(
                tr.getTitle(), tr.getAuthors()
        )).isTrue();
    }

    @Test
    public void existsByTitleAndExactAuthors_ShouldReturnFalse_WhenTitleAndAuthorsNotExist()
    {
        Track tr = new Track();
        tr.setName("TestTrack");
        tr.setTitle("TestTrackTitle");

        final String fAtr = "TestAuthor";
        tr.setAuthors(Set.of(new Author(fAtr)));

        List<Track> test = new ArrayList<>(List.of(tr));

        // Prepare mock in repository
        // Should return empty list, cuz we checkin not exist entities
        when(trackRepository.findTracksByTitleAndFirstAuthor(
                tr.getTitle(), fAtr
        )).thenReturn(List.of());

        // Assert
        assertThat(trackService.existsByTitleAndExactAuthors(
                tr.getTitle(), tr.getAuthors()
        )).isFalse();
    }
}
