package rf.mizuka.application.tracks.service;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.multipart.MultipartFile;
import rf.mizuka.utilities.color.Colorizier;
import rf.mizuka.web.application.database.entities.media.authors.Author;
import rf.mizuka.web.application.database.entities.media.tracks.Track;
import rf.mizuka.web.application.database.repository.TrackRepository;
import rf.mizuka.web.application.services.tracks.TrackService;

import java.awt.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
@Rollback
public class TrackServiceTest
{
    @Autowired
    private TrackService trackService;

    @Autowired
    private TrackRepository trackRepository;

    @Test
    public void encode64ShouldBeReturnString()
    {
        byte[] inputBytes = "Spring".getBytes();
        String expectedBase64 = "U3ByaW5n";

        String result = trackService.encodeBase64Picture(inputBytes);

        assertThat(result)
                .isNotNull()
                .isEqualTo(expectedBase64);
    }

    @Test
    void saveTrack_ShouldSaveToDatabaseAndThenRollback()
            throws Exception
    {
        Track track = new Track();
        track.setName("Test Track");
        track.setTitle("Track");
        track.setAuthors(new HashSet<>(Set.of(new Author("test"))));
        track.setDuration(Duration.ofSeconds(100));
        track.setPicture("Hello".getBytes());
        track.setColor(Colorizier.convertColorToHex(Color.WHITE));

        MultipartFile mockFile = new MockMultipartFile(
                "file",
                "test-picture.png",
                "image/png",
                "FakeImageContent".getBytes()
        );

        Track savedTrack = trackService.saveTrack(track, mockFile);

        assertThat(savedTrack.getId()).isNotNull();

        boolean exists = trackRepository.existsById(savedTrack.getId());
        assertThat(exists).isTrue();
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class MockTrackServiceTest
    {
        @Mock
        private TrackRepository trackRepository;

        @InjectMocks
        private TrackService trackService;

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
}
