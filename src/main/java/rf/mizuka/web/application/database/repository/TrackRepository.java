package rf.mizuka.web.application.database.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rf.mizuka.web.application.database.entities.media.tracks.Track;

import java.util.List;

@Repository
public interface TrackRepository
        extends JpaRepository<Track, Long>
{
    @Query("""
        SELECT DISTINCT t FROM Track t 
        JOIN FETCH t.authors all_a 
        JOIN t.authors first_a 
        WHERE t.title = :title 
          AND first_a.name = :firstAuthor
    """)
    List<Track> findTracksByTitleAndFirstAuthor(
            @Param("title") String title,
            @Param("firstAuthor") String firstAuthor
    );
    Page<Track> findByNameContaining(
            String name, Pageable pageable
    );
}