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
    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] picture;

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

    @Column(unique = true)
    private String filePath;

    public Track() {}
}