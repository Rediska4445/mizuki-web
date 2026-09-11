package rf.mizuka.application.auth.repository;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import rf.mizuka.web.application.database.entities.user.User;
import rf.mizuka.web.application.database.repository.user.UserRepository;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Transactional
@Rollback
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = "classpath:settings-test.properties")
public class UserRepositoryTest
{
    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private CacheManager cacheManager;

    @BeforeEach
    void setUp()
    {
        CacheManager realCacheManager = new ConcurrentMapCacheManager();
        Mockito.when(cacheManager.getCache(Mockito.anyString()))
                .thenAnswer(invocation -> realCacheManager.getCache(invocation.getArgument(0)));
    }

    @Test
    @DisplayName("existsByUsername должно возвращать false для несуществующего пользователя")
    void shouldReturnFalseForNonexistentUser()
    {
        String username = "newuser";

        boolean exists = userRepository.existsByUsername(username);

        assertFalse(exists);
    }

    @Test
    @DisplayName("existsByUsername должно возвращать true для существующего пользователя")
    void shouldReturnTrueForExistingUser() {
        String username = "existinguser";
        String password = "secret123";

        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        userRepository.save(user);

        boolean exists = userRepository.existsByUsername(username);

        assertTrue(exists);
    }

    @Test
    @DisplayName("UserRepository не должен допускать дубликаты по username")
    void shouldNotAllowDuplicateUsernameInRepository() {
        String username = "duplicateuser";
        String password = "secret123";

        User user1 = new User();
        user1.setUsername(username);
        user1.setPassword(password);
        userRepository.save(user1);

        User user2 = new User();
        user2.setUsername(username);
        user2.setPassword("anotherPass");
    }
}