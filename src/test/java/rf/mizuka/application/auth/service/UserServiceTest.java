package rf.mizuka.application.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import rf.mizuka.web.application.database.entities.user.User;
import rf.mizuka.web.application.database.repository.user.UserRepository;
import rf.mizuka.web.application.services.user.UserService;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(locations = "classpath:settings-test.properties")
@Transactional
@Rollback
@ExtendWith(MockitoExtension.class)
public class UserServiceTest
{
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    /*
    * 1. User should save, with all transferred to him data
    * 2. Register user should return saved user
    * 3. Register user should check on already exist user
    * 3.1. Exist check in the event problems should throw "UserExistException"
    * */
    @Test
    @DisplayName("T1. Register user should be success")
    void shouldRegisterNewUserAndAllowImmediateLogin()
            throws Exception
    {
        // For avoid "(userExist == null || userExist)" condition
        Mockito.when(userRepository.existsByUsername(Mockito.any()))
                .thenReturn(false);

        // return s-mocking user
        Mockito.when(userRepository.save(Mockito.any()))
                .thenAnswer(e -> new User("mock", "123"));

        // Mock user
        User user = userService.registerUser("mock", "123");

        // Checkin all data
        assertThat(user.getUsername())
                .isEqualTo("mock");
        assertThat(user.getPassword())
                .isEqualTo("123");
    }
}
