package rf.mizuka.application.tracks.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import rf.mizuka.web.application.database.entities.media.tracks.Track;
import rf.mizuka.web.application.database.repository.TrackRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class TrackRepositoryTest
{
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TrackRepository trackRepository;

    @Test
    void shouldSaveTrackWithGeneratedId()
    {
        Track newTrack = new Track();
        newTrack.setName("Test Track - Save Test");

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
        Track tr = new Track();
        tr.setName("Findable Track");

        Track savedTrack = trackRepository.save(tr);
        Long trackId = savedTrack.getId();

        Optional<Track> foundTrack = trackRepository.findById(trackId);

        assertTrue(foundTrack.isPresent(), "Трек должен быть найден по существующему id");
        assertEquals(trackId, foundTrack.get().getId(), "Первичный ключ должен совпадать");
        assertEquals("Findable Track", foundTrack.get().getName(), "Название трека не изменилось");
    }

    @Test
    void shouldUpdateExistingTrack() {
        Track tr = new Track();
        tr.setName("Findable Trac1k");
        Track trackV1 = trackRepository.save(tr);
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
        Track tr = new Track();
        tr.setName("Findable Track123");
        Track trackToDelete = trackRepository.save(tr);
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
        String str = "Track1";
        String str1 = "Track2";
        String str2 = "Track3";

        Track tr = new Track();
        tr.setName(str);

        Track tr1 = new Track();
        tr1.setName(str1);

        Track tr2 = new Track();
        tr2.setName(str2);

        Track track1 = trackRepository.save(tr);
        Track track2 = trackRepository.save(tr1);
        Track track3 = trackRepository.save(tr2);

        assertEquals(3L, trackRepository.count(), "Должно быть 3 трека перед findAll()");

        List<Track> allTracks = trackRepository.findAll();

        assertEquals(3, allTracks.size(), "findAll() должен вернуть все 3 трека");
        assertTrue(allTracks.contains(track1), "Первый трек должен быть в списке");
        assertTrue(allTracks.contains(track2), "Второй трек должен быть в списке");
        assertTrue(allTracks.contains(track3), "Третий трек должен быть в списке");

        assertEquals(track1.getId(), allTracks.get(0).getId(), "Первый ID должен быть 1");
        assertEquals(str, allTracks.get(0).getName(), "Имена должны совпадать");
        assertEquals(str1, allTracks.get(1).getName(), "Имена должны совпадать");
        assertEquals(str2, allTracks.get(2).getName(), "Имена должны совпадать");

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
        String str = "Track1";
        String str1 = "Track2";
        String str2 = "Track3";

        Track tr = new Track();
        tr.setName(str);

        Track tr1 = new Track();
        tr1.setName(str1);

        Track tr2 = new Track();
        tr2.setName(str2);

        trackRepository.save(tr);
        trackRepository.save(tr1);
        trackRepository.save(tr2);

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