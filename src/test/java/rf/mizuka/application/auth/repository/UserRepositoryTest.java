package rf.mizuka.application.auth.repository;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.annotation.Rollback;
import rf.mizuka.web.application.auth.database.repository.UserRepository;
import rf.mizuka.web.application.auth.models.User;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Transactional
@Rollback
public class UserRepositoryTest {
    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("existsByUsername должно возвращать false для несуществующего пользователя")
    void shouldReturnFalseForNonexistentUser() {
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