package rf.mizuka.web.application.models.rest;

import jakarta.persistence.*;
import rf.mizuka.web.application.models.user.User;

import java.util.Set;

@Entity
@Table(name = "developers")
public class Developer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String clientId;

    @Column(nullable = false)
    private String clientSecret;

    @Column(name = "developer_name", length = 64)
    private String developerName;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> scopes;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    public Long getId() {
        return id;
    }

    public Developer setId(Long id) {
        this.id = id;
        return this;
    }

    public String getClientId() {
        return clientId;
    }

    public Developer setClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public Developer setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
        return this;
    }

    public String getDeveloperName() {
        return developerName;
    }

    public Developer setDeveloperName(String developerName) {
        this.developerName = developerName;
        return this;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public Developer setScopes(Set<String> scopes) {
        this.scopes = scopes;
        return this;
    }

    public User getOwner() {
        return owner;
    }

    public Developer setOwner(User owner) {
        this.owner = owner;
        return this;
    }
}
