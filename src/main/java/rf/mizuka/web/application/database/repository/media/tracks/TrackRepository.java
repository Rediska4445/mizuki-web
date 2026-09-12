package rf.mizuka.web.application.database.repository.media.tracks;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rf.mizuka.web.application.database.entities.media.tracks.Track;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrackRepository
        extends JpaRepository<Track, Long>
{
    @Override
    @Cacheable(value = "tracks", key = "#a0")
    Optional<Track> findById(Long aLong);

    @Query(
            "SELECT t.likesCount FROM Track t WHERE t.id = :trackId"
    )
    @Cacheable(value = "tracks_likes", key = "#a0")
    Long getLikesCountByTrackId(@Param("trackId") Long trackId);

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

    @Query("""
            SELECT DISTINCT t FROM Track t
            LEFT JOIN t.authors a
            WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(a.name) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%'))
            OR CAST(t.duration AS string) LIKE CONCAT('%', :query, '%')
    """)
    Page<Track> searchTracks(
            @Param("query") String query,
            Pageable pageable
    );

    @Override
    @CacheEvict(value = {"tracks_likes", "tracks"}, allEntries = true)
    void deleteById(Long aLong);

    @Modifying
    @Query(
            "UPDATE Track t SET t.playsCount = t.playsCount + :count WHERE t.id = :trackId"
    )
    void addListenCount(
            @Param("trackId") Long trackId,
            @Param("count") Long count
    );

    // For decrement, transfer -1 in count parameter
    @Modifying
    @Query("""
            UPDATE Track t\s
            SET t.likesCount = CASE\s
                WHEN (t.likesCount + :count) < 0 THEN 0\s
                ELSE (t.likesCount + :count)\s
            END\s
            WHERE t.id = :trackId
   \s""")
    @CacheEvict(value = {"tracks_likes", "tracks"}, allEntries = true)
    void addTrackLikes(
            @Param("trackId") Long trackId,
            @Param("count") Long count
    );
}