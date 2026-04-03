package rf.mizuka.web.application.tracks.models;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "tracks")
public class Track {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String name;

    public Track() {
    }

    public String getName() {
        return name;
    }

    public Track setName(String name) {
        this.name = name;
        return this;
    }
}
