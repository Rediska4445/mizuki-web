package rf.mizuka.web.application.database.repository;

import org.springframework.data.jpa.repository.JpaRepository;
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
    Optional<User> findByUsername(
            String username
    );
    /**
     *
     * **/
    boolean existsByUsername(
            String username
    );
}
