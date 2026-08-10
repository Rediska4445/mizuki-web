package rf.mizuka.application.tracks.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import rf.mizuka.web.application.database.tracks.repository.TrackRepository;
import rf.mizuka.web.application.models.tracks.Track;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class TrackRepositoryTest {
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TrackRepository trackRepository;

    @Test
    void shouldSaveTrackWithGeneratedId() {
        Track newTrack = new Track().setName("Test Track - Save Test");

        Track savedTrack = trackRepository.save(newTrack);

        assertNotNull(savedTrack);
        assertTrue(savedTrack.getId() > 0L);
        assertEquals("Test Track - Save Test", savedTrack.getName());

        Track foundTrack = trackRepository.findById(savedTrack.getId()).orElse(null);
        assertNotNull(foundTrack);
        assertEquals("Test Track - Save Test", foundTrack.getName());
    }

    @Test
    void shouldFindById() {
        Track savedTrack = trackRepository.save(new Track().setName("Findable Track"));
        Long trackId = savedTrack.getId();

        Optional<Track> foundTrack = trackRepository.findById(trackId);

        assertTrue(foundTrack.isPresent(), "Трек должен быть найден по существующему id");
        assertEquals(trackId, foundTrack.get().getId(), "Первичный ключ должен совпадать");
        assertEquals("Findable Track", foundTrack.get().getName(), "Название трека не изменилось");
    }

    @Test
    void shouldUpdateExistingTrack() {
        Track trackV1 = trackRepository.save(new Track().setName("Version 1"));
        Long trackId = trackV1.getId();

        trackV1.setName("Version 2");
        Track trackV2 = trackRepository.save(trackV1);

        assertEquals("Version 2", trackV2.getName(), "Имя должно обновиться в объекте");
        assertEquals(trackId, trackV2.getId(), "ID не должен измениться при обновлении");

        Track freshFromDb = trackRepository.findById(trackId).orElse(null);
        assertNotNull(freshFromDb, "Трек должен существовать в БД");
        assertEquals("Version 2", freshFromDb.getName(), "БД должна содержать обновлённое имя");
        assertEquals(1L, trackRepository.count(), "Должна быть только 1 запись, а не дубликат");
    }

    @Test
    void shouldDeleteById() {
        Track trackToDelete = trackRepository.save(new Track().setName("ToDelete"));
        Long trackId = trackToDelete.getId();
        assertEquals(1L, trackRepository.count(), "Сначала должна быть 1 запись");

        trackRepository.deleteById(trackId);

        assertFalse(trackRepository.existsById(trackId),
                "Трек не должен существовать после deleteById()");

        assertTrue(trackRepository.findById(trackId).isEmpty(),
                "findById() должен вернуть Optional.empty() для удалённого трека");

        assertEquals(0L, trackRepository.count(),
                "База должна быть пустой после удаления единственной записи");
    }

    @Test
    void shouldReturnAllTracks() {
        Track track1 = trackRepository.save(new Track().setName("Track Alpha"));
        Track track2 = trackRepository.save(new Track().setName("Track Beta"));
        Track track3 = trackRepository.save(new Track().setName("Track Gamma"));

        assertEquals(3L, trackRepository.count(), "Должно быть 3 трека перед findAll()");

        List<Track> allTracks = trackRepository.findAll();

        assertEquals(3, allTracks.size(), "findAll() должен вернуть все 3 трека");
        assertTrue(allTracks.contains(track1), "Первый трек должен быть в списке");
        assertTrue(allTracks.contains(track2), "Второй трек должен быть в списке");
        assertTrue(allTracks.contains(track3), "Третий трек должен быть в списке");

        assertEquals(track1.getId(), allTracks.get(0).getId(), "Первый ID должен быть 1");
        assertEquals("Track Alpha", allTracks.get(0).getName(), "Имена должны совпадать");
    }

    @Test
    void shouldReturnEmptyListWhenNoTracks() {
        assertEquals(0L, trackRepository.count(), "БД должна быть пустой в начале теста");

        List<Track> allTracks = trackRepository.findAll();

        assertNotNull(allTracks, "findAll() никогда не возвращает null");
        assertTrue(allTracks.isEmpty(), "Список должен быть пустым при отсутствии треков");
        assertEquals(0, allTracks.size(), "Размер списка должен быть 0");
        assertEquals(0L, trackRepository.count(), "count() должен подтверждать пустоту БД");
    }

    @Test
    void shouldReturnCorrectCount() {
        trackRepository.save(new Track().setName("Counted Track 1"));
        trackRepository.save(new Track().setName("Counted Track 2"));
        trackRepository.save(new Track().setName("Counted Track 3"));

        long trackCount = trackRepository.count();

        assertEquals(3L, trackCount, "count() должен вернуть точное количество треков");

        List<Track> allTracks = trackRepository.findAll();
        assertEquals(3, allTracks.size(), "findAll().size() должен совпадать с count()");
        assertFalse(allTracks.isEmpty(), "Список не должен быть пустым при count() > 0");
    }

    @Test
    void shouldFailSaveWithNullName() {
        Track invalidTrack = new Track();
        assertEquals(0L, trackRepository.count(), "БД пуста перед тестом");
        assertNull(invalidTrack.getName(), "name должен быть null для провокации ошибки");

        DataIntegrityViolationException exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> trackRepository.save(invalidTrack),
                "save() должен выбросить исключение из-за name=null"
        );

        entityManager.clear();

        assertEquals(0L, trackRepository.count(),
                "count() должен остаться 0 после отката неудачного INSERT");

        assertTrue(exception.getMessage().toLowerCase().contains("name"),
                "Исключение должно упоминать поле 'name'");
    }

    @Test
    void shouldReturnEmptyOptionalForNonExistingId() {
        assertEquals(0L, trackRepository.count(), "БД пуста перед тестом");
        Long nonExistingId = 999L;

        Optional<Track> result = trackRepository.findById(nonExistingId);

        assertTrue(result.isEmpty(), "findById(nonExistingId) должен вернуть Optional.empty()");

        assertEquals(0L, trackRepository.count(), "БД остаётся пустой");
    }
}