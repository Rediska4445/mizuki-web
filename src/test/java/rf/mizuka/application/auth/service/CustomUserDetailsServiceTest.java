package rf.mizuka.application.auth.service;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Rollback;
import rf.mizuka.web.application.database.user.repository.UserRepository;
import rf.mizuka.web.application.models.user.User;
import rf.mizuka.web.application.services.user.CustomUserDetailsService;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Rollback
class CustomUserDetailsServiceTest {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("loadUserByUsername возвращает UserDetails для существующего пользователя")
    void shouldLoadUserDetailsForExistingUser() {
        // GIVEN: пользователь в БД
        String username = "existinguser";
        String rawPassword = "secret123";

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        userRepository.save(user);

        // WHEN: вызов loadUserByUsername
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // THEN: проверки через org.junit.jupiter.api.Assertions
        assertNotNull(userDetails);
        assertEquals(username, userDetails.getUsername());
        String encodedPassword = userDetails.getPassword();
        assertNotNull(encodedPassword);
        assertTrue(encodedPassword.startsWith("$2a$") || encodedPassword.startsWith("$2b$"));
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
    }

    @Test
    @DisplayName("loadUserByUsername кидает UsernameNotFoundException для несуществующего пользователя")
    void shouldThrowUsernameNotFoundExceptionWhenUserNotExists() {
        // GIVEN: пользователь точно не существует
        String username = "nonexistentuser";
        assertFalse(userRepository.existsByUsername(username));

        // WHEN + THEN: проверяем исключение
        UsernameNotFoundException thrown =
                assertThrows(UsernameNotFoundException.class, () -> {
                    userDetailsService.loadUserByUsername(username);
                });

        assertTrue(thrown.getMessage().contains("User not found: " + username));
    }

    @Test
    @DisplayName("UserDetails возвращает isEnabled = true для обычного пользователя")
    void shouldUserDetailsBeEnabled() {
        // GIVEN: пользователь в БД
        String username = "enableduser";
        String rawPassword = "pass456";

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        userRepository.save(user);

        // WHEN: загружаем UserDetails
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // THEN: проверки
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
    }
}