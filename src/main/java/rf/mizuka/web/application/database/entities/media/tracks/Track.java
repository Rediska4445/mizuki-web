package rf.mizuka.web.application.database.entities.media.tracks;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import rf.mizuka.web.application.database.entities.media.authors.Author;

import java.awt.*;
import java.time.Duration;
import java.util.Set;

@Data
@Entity
@AllArgsConstructor
@Table(name = "tracks")
public class Track
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* Metadata */
    @Column(length = 256)
    private String picturePath;

    @Column(length = 7)
    private String color;

    @Column(length = 128)
    private String title;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<Author> authors;

    @Column
    private Duration duration;

    /* Physical data */
    @Column(nullable = false)
    private String name;

    @Column(unique = true, length = 256)
    private String filePath;

    public Track() {}

    @Override
    public final int compareTo(@NotNull Track o)
    {
        return id.compareTo(o.getId());
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == null || getClass() != o.getClass())
            return false;

        Track track = (Track) o;

        return Objects.equal(id, track.id);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(id);
    }
}