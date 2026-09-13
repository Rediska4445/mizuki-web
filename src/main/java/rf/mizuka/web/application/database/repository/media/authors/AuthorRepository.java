package rf.mizuka.web.application.database.repository.media.authors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import rf.mizuka.web.application.database.entities.media.authors.Author;

import java.util.Optional;

public interface AuthorRepository
        extends JpaRepository<Author, Long>
{
    @Cacheable(value = "authors", key = "#a0" /* first argument */)
    Optional<Author> findByName(String name);

    @Override
    @CacheEvict(value = "authors", allEntries = true)
    void deleteById(Long aLong);

    // PUT logic for authors
    default Author buildOrGet(String name)
    {
        return findByName(name)
                .orElseGet(() -> save(new Author(name.trim())));
    }
}
