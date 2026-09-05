package rf.mizuka.web.application.database.repository.user;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rf.mizuka.web.application.database.entities.user.User;

import java.util.List;
import java.util.Optional;

/**
 * Repository for user management in database.
 * **/
@Repository
public interface UserRepository
        extends JpaRepository<User, Long>
{
    /**
     * Retrieves a user by their unique identifier.
     * <p>
     * This is a default CRUD method inherited from {@link org.springframework.data.jpa.repository.JpaRepository}
     * and is not overridden with custom query logic. It is annotated with an entity graph
     * to eagerly fetch the associated liked tracks in a single query.
     * </p>
     *
     * @param id the unique identifier of the user to find
     * @return an {@link java.util.Optional} containing the found user, or {@link java.util.Optional#empty()} if none found
     */
    @EntityGraph(attributePaths = {
            "likedTracks"
    })
    Optional<User> findById(Long id);

    /* read user section */
    /**
     * Retrieves a user by their username.
     * <p>
     * This method eagerly loads the associated {@code likedTracks} collection using an entity graph
     * to avoid {@code LazyInitializationException} and reduce the number of database queries.
     * </p>
     * <p>
     * The result of this operation is cached in the "users" cache using the {@code username}
     * (the first argument) as the cache key to improve performance on subsequent lookups.
     * </p>
     *
     * @param username the username of the user to find
     * @return an {@link java.util.Optional} containing the found user, or {@link java.util.Optional#empty()} if none found
     */
    @EntityGraph(attributePaths = {
            "likedTracks"
    })
    @Cacheable(
            value = "users", key = "#a0" /* first argument */
    )
    Optional<User> findByUsername(String username);

    @Cacheable(
            value = "user_exists_cache", key = "#a0" /* first argument */, unless = "#result == null"
    )
    Boolean existsByUsername(String username);

    @Query(
            value = "SELECT track_id FROM user_liked_tracks WHERE user_id = :userId", nativeQuery = true
    )
    List<Long> findLikedTrackIdsByUserId(@Param("userId") Long userId);

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
