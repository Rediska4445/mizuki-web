package rf.mizuka.web.application.tracks.database.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rf.mizuka.web.application.tracks.models.Track;

@Repository
public interface TrackRepository extends JpaRepository<Track, Long> {

}