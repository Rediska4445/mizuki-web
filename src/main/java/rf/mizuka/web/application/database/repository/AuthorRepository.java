package rf.mizuka.web.application.database.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rf.mizuka.web.application.database.entities.media.authors.Author;

import java.util.Optional;

public interface AuthorRepository
        extends JpaRepository<Author, Long>
{
    Optional<Author> findByName(String name);

    default Author buildOrGet(String name) {
        return findByName(name)
                .orElseGet(() -> save(new Author(name)));
    }
}
