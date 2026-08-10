package rf.mizuka.application.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import rf.mizuka.web.application.controllers.auth.UserExistException;
import rf.mizuka.web.application.database.user.repository.UserRepository;
import rf.mizuka.web.application.models.user.User;
import rf.mizuka.web.application.services.user.UserService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@Rollback
public class UserServiceTest {
    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Успешная регистрация нового пользователя позволяет ему сразу войти в систему")
    void shouldRegisterNewUserAndAllowImmediateLogin() throws Exception {
        long initialCount = userRepository.count();
        assertThat(initialCount).isGreaterThanOrEqualTo(0);

        String username = "newuser";
        String rawPassword = "secret123";

        assertThat(userRepository.existsByUsername(username)).isFalse();

        userService.registerUser(username, rawPassword);

        Optional<User> userOpt = userRepository.findByUsername(username);
        assertThat(userOpt).isPresent();
        User user = userOpt.get();
        assertThat(user.getUsername()).isEqualTo(username);
        assertThat(user.getPassword()).isNotEmpty();

        String encodedPassword = user.getPassword();
        assertThat(encodedPassword)
                .matches("\\$2[ab]\\$\\d{2}\\$.{53}");

        assertThat(passwordEncoder.matches(rawPassword, encodedPassword)).isTrue();

        UserDetails loadedUser = userDetailsService.loadUserByUsername(username);
        assertThat(loadedUser.getUsername()).isEqualTo(username);
        assertThat(loadedUser.getPassword()).isEqualTo(encodedPassword);

        assertThatThrownBy(() -> userService.registerUser(username, "anotherPass"))
                .isInstanceOf(UserExistException.class)
                .hasMessage(username);

        assertThat(userRepository.count()).isEqualTo(initialCount + 1);
    }
}
