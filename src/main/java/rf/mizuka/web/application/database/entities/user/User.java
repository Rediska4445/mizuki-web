package rf.mizuka.web.application.database.entities.user;

import com.google.common.base.Objects;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import rf.mizuka.web.application.database.entities.media.tracks.Track;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

@Entity
@ToString
@AllArgsConstructor
@Table(name = "users")
public class User
    implements UserDetails
{
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Getter
    @ManyToMany(
            cascade =
            {
                CascadeType.PERSIST,
                CascadeType.MERGE
            },
            fetch = FetchType.LAZY
    )
    @JoinTable(
            name = "user_liked_tracks",
            joinColumns = @JoinColumn(
                    name = "user_id",
                    foreignKey = @ForeignKey(foreignKeyDefinition = "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE")
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "track_id",
                    foreignKey = @ForeignKey(foreignKeyDefinition = "FOREIGN KEY (track_id) REFERENCES tracks(id) ON DELETE CASCADE")
            )
    )
    private Set<Track> likedTracks;

    public User() {}

    public User(String username, String password)
    {
        this.username = username;
        this.password = password;
    }

    @Override
    public String getUsername()
    {
        return username;
    }

    @Override
    public String getPassword()
    {
        return password;
    }

    @Override
    public boolean isAccountNonExpired()
    {
        return true;
    }

    @Override
    public boolean isAccountNonLocked()
    {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired()
    {
        return true;
    }

    @Override
    public boolean isEnabled()
    {
        return true;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities()
    {
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));
    }

    public User setUsername(String username)
    {
        this.username = username;
        return this;
    }

    public User setPassword(String password)
    {
        this.password = password;
        return this;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == null || getClass() != o.getClass())
            return false;

        User user = (User) o;

        return Objects.equal(id, user.id) && Objects.equal(username, user.username);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(id, username);
    }

    @Override
    public String toString()
    {
        return "User{" +
                "id=" + id +
                '}';
    }
}