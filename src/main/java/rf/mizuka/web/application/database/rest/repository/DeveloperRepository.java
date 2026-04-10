package rf.mizuka.web.application.database.rest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rf.mizuka.web.application.models.rest.Developer;
import rf.mizuka.web.application.models.user.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeveloperRepository extends JpaRepository<Developer, Long> {
    Optional<Developer> findByClientId(String clientId);
    @Query("SELECT d FROM Developer d LEFT JOIN FETCH d.scopes WHERE d.owner = :owner")
    List<Developer> findAllByOwner(@Param("owner") User owner);
}
