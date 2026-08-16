package rf.mizuka.web.application.database.entities.media.authors;

import jakarta.persistence.*;
import lombok.*;
import rf.mizuka.web.application.database.entities.user.User;

@Data
@Entity
@Getter
@Setter
@ToString
@Table(name = "authors")
@AllArgsConstructor
public class Author
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @Column(nullable = false, length = 64, unique = true)
    private String name;

    public Author(String name) {
        this.name = name;
    }

    public Author() {}
}


