package rf.mizuka.application.audio.metadata.service;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.NamedExecutable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import rf.mizuka.web.application.database.entities.media.tracks.Track;
import rf.mizuka.web.application.services.audio.AudioMetadataService;
import rf.mizuka.web.application.services.audio.UnknownAuthorException;
import rf.mizuka.web.application.services.audio.UnknownTitleException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.time.Duration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class AudioMetadataServiceTest
{
    private Track validTrack;
    private Track invalidTrack;

    @InjectMocks
    public AudioMetadataService audioMetadataService;

    @BeforeEach
    void setUp() throws Exception {
        URL validFileUrl = getClass().getClassLoader().getResource("static/audio/.mp3");
        URL corruptedFileUrl = getClass().getClassLoader().getResource("static/audio/imnotismusic.mp3");

        assertThat(validFileUrl).as("Тестовый файл valid-track.mp3 не найден в resources/audio/").isNotNull();
        assertThat(corruptedFileUrl).as("Тестовый файл corrupted.mp3 не найден in resources/audio/").isNotNull();

        File validFile = Paths.get(validFileUrl.toURI()).toFile();
        File invalidFile = Paths.get(corruptedFileUrl.toURI()).toFile();

        validTrack = new Track();
        validTrack.setFilePath(validFile.getAbsolutePath());

        invalidTrack = new Track();
        invalidTrack.setFilePath(invalidFile.getAbsolutePath());
    }

    @Test
    public void extractMetadata_ShouldThrowsExceptions()
            throws IOException,
            UnknownTitleException, CannotReadException, TagException,
            UnknownAuthorException, InvalidAudioFrameException,  ReadOnlyFileException
    {
        assertThrows(InvalidAudioFrameException.class, (NamedExecutable) () -> audioMetadataService.extractMetadata(invalidTrack));
    }

    @Test
    public void extractMetadata_ShouldWork()
            throws IOException,
            UnknownTitleException, CannotReadException, TagException,
            UnknownAuthorException, InvalidAudioFrameException,  ReadOnlyFileException
    {
        AudioMetadataService.Metadata metadata = audioMetadataService.extractMetadata(validTrack);

        assertThat(metadata).isNotNull();
        assertThat(metadata.title())
                .isNotNull()
                .isNotEmpty()
                .hasSizeGreaterThan(1);
    }

    @Test
    public void convertDurationToString_ShouldReturnFormattedDuration()
    {
        assertThat(audioMetadataService.convertDurationToString(Duration.ofSeconds(5)))
                .isEqualTo("0:05");

        assertThat(audioMetadataService.convertDurationToString(Duration.ofSeconds(61)))
                .isEqualTo("1:01");

        assertThat(audioMetadataService.convertDurationToString(Duration.ofSeconds(120)))
                .isEqualTo("2:00");

        assertThat(audioMetadataService.convertDurationToString(Duration.ofSeconds(3600)))
                .isEqualTo("1:00:00");

        assertThat(audioMetadataService.convertDurationToString(Duration.ofSeconds(3661)))
                .isEqualTo("1:01:01");

        assertThat(audioMetadataService.convertDurationToString(Duration.ofSeconds(-10)))
                .isEqualTo("--:--");
    }
}
