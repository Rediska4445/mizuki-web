package rf.mizuka.web.application.database.entities.media.tracks;

import com.google.common.base.Objects;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.validator.constraints.time.DurationMin;
import org.jetbrains.annotations.NotNull;
import rf.mizuka.web.application.database.entities.media.authors.Author;
import rf.mizuka.web.application.database.entities.user.User;

import java.io.Serial;
import java.io.Serializable;
import java.sql.Types;
import java.time.Duration;
import java.util.Set;

@Data
@Entity
@Builder
@AllArgsConstructor
@Table(name = "tracks")
public class Track
        implements Serializable, Comparable<Track>
{
    @Serial
    private static final long serialVersionUID
            = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* Metadata */
    @Column(length = 256)
    @Basic(fetch = FetchType.LAZY)
    private String picturePath;

    @Column(length = 7)
    @Basic(fetch = FetchType.LAZY)
    private String color;

    @Column(length = 128)
    @Basic(fetch = FetchType.LAZY)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User fileOwner;

    @Column
    @Basic(fetch = FetchType.LAZY)
    private Boolean isExplicit
            = Boolean.FALSE;

    @Column(nullable = false)
    @Min(value = 0)
    private Long likesCount
            = 0L;

    @Column(nullable = false)
    @Min(value = 0)
    private Long playsCount
            = 0L;

    @Column(columnDefinition = "TEXT")
    @Basic(fetch = FetchType.LAZY)
    private String description;

    @JoinTable(
        name = "tracks_authors",
        joinColumns = @JoinColumn(name = "tracks_id"),
        inverseJoinColumns = @JoinColumn(name = "authors_id")
    )
    @ManyToMany(cascade =
    {
        CascadeType.PERSIST,
        CascadeType.MERGE
    })
    private Set<Author> authors;

    @Column
    @DurationMin(seconds = 0)
    @Basic(fetch = FetchType.LAZY)
    @JdbcTypeCode(Types.BIGINT)
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

    @Override
    public String toString()
    {
        return "Track{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", filePath='" + filePath + '\'' +
                '}';
    }
}