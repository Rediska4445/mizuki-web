package rf.mizuka.web.application.services.authors;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rf.mizuka.web.application.database.entities.media.authors.Author;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthorService
{
    @Getter
    private final String authorsSeparator;

    public AuthorService(@Value("${mizuki.authors.separator}:,") String authorsSeparator)
    {
        this.authorsSeparator = authorsSeparator;
    }

    public Set<Author> splitAuthors(String authors)
    {
        return Arrays.stream(authors.split(authorsSeparator)).map(Author::new).collect(Collectors.toSet());
    }

    public String joinAuthors(Collection<Author> authors)
    {
        return String.join(authorsSeparator,  authors.stream()
                .map(Author::getName)
                .collect(Collectors.joining(authorsSeparator))
        );
    }
}
