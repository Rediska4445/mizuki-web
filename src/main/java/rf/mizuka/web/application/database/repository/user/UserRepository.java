package rf.mizuka.web.application.database.repository.user;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rf.mizuka.web.application.database.entities.user.User;

import java.util.Optional;

/**
 * Repository for user management in database.
 * **/
@Repository
public interface UserRepository
        extends JpaRepository<User, Long>
{
    /* read user section */
    @Cacheable(value = "users", key = "#a0" /* first argument */)
    Optional<User> findByUsername(String username);

    @Cacheable(value = "user_exists_cache", key = "#a0" /* first argument */, unless = "#result == null")
    Boolean existsByUsername(String username);

    /* user post section */
    @Override
    @CacheEvict(value = "users", allEntries = true)
    void deleteById(Long aLong);

    /* user likes post section */
    @Modifying
    @Query(value = """
            INSERT INTO user_liked_tracks (user_id, track_id)\s
            VALUES (:userId, :trackId)\s
            ON CONFLICT (user_id, track_id) DO NOTHING
           \s""",
            nativeQuery = true
    )
    void insertLike(
            @Param("userId") Long userId,
            @Param("trackId") Long trackId
    );

    @Modifying
    @Query(value = """
            DELETE FROM user_liked_tracks\s
            WHERE user_id = :userId AND track_id = :trackId
           \s""",
            nativeQuery = true
    )
    void deleteLike(
            @Param("userId") Long userId,
            @Param("trackId") Long trackId
    );
}
