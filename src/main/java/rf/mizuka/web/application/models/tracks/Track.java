package rf.mizuka.web.application.models.tracks;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@AllArgsConstructor
@Table(name = "tracks")
public final class Track
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] picture;

    @Transient
    private String base64Picture;

    public String getBase64Picture() {
        if (this.picture != null && this.picture.length > 0) {
            return java.util.Base64.getEncoder().encodeToString(this.picture);
        }
        return null;
    }

    @Column(nullable = false)
    private String name;

    @Column(length = 128)
    private String title;

    @Column(length = 64, unique = true)
    private String author;

    @Column
    private Short duration;

    @Column(unique = true)
    private String filePath;

    public Track() {}
}