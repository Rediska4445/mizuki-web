package rf.mizuka.web.application.database.tracks.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rf.mizuka.web.application.models.tracks.Track;

@Repository
public interface TrackRepository extends JpaRepository<Track, Long> {
    Page<Track> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<Track> findAll(Pageable pageable);
}