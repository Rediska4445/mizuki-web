package rf.mizuka.application.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import rf.mizuka.web.application.brokers.audio.AudioStreamProducer;
import rf.mizuka.web.application.brokers.tracks.TrackProducer;
import rf.mizuka.web.application.configurations.security.PasswordEncoder;
import rf.mizuka.web.application.controllers.audio.AudioController;
import rf.mizuka.web.application.controllers.auth.AuthController;
import rf.mizuka.web.application.database.entities.user.User;
import rf.mizuka.web.application.database.repository.user.UserRepository;
import rf.mizuka.web.application.services.authors.AuthorService;
import rf.mizuka.web.application.services.user.CustomUserDetailsService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@WebMvcTest
@Rollback
@TestPropertySource(locations = "classpath:settings-test.properties")
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest
{
    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private io.minio.MinioClient minioClient;

    @MockitoBean
    private rf.mizuka.web.application.services.storage.StorageService storageService;

    @MockitoBean
    private rf.mizuka.web.application.services.tracks.TrackService trackService;

    @MockitoBean
    private rf.mizuka.web.application.services.user.UserService userService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private AudioStreamProducer audioStreamProducer;

    @MockitoBean
    private AudioController audioController;

    @MockitoBean
    private AuthController authController;

    @MockitoBean
    private TrackProducer trackProducer;

    @MockitoBean
    private CacheManager cacheManager;

    @MockitoBean
    private AuthorService authorService;

    @BeforeEach
    void setUp()
    {
        CacheManager realCacheManager = new ConcurrentMapCacheManager();
        Mockito.when(cacheManager.getCache(Mockito.anyString()))
                .thenAnswer(invocation -> realCacheManager.getCache(invocation.getArgument(0)));

        userRepository = Mockito.mock(UserRepository.class);

        userDetailsService = new CustomUserDetailsService(userRepository);
    }

    /*
    * 1. userDetailsService.loadUserByUsername should return UserDetails for success for exist user
    * 2. userDetailsService.loadUserByUsername should not return null result for success for exist user
    * */
    @Test
    void shouldLoadUserDetailsForExistingUser()
    {
        String username = "existinguser";
        String rawPassword = "secret123";

        User user = new User();
        user.setUsername(username);
        user.setPassword(rawPassword);

        // Study mock
        Mockito.when(userRepository.findByUsername(Mockito.any()))
                .thenAnswer(e -> Optional.of(user));

        UserDetails userDetails
                = userDetailsService.loadUserByUsername(username);

        // Check on null
        assertNotNull(userDetails);

        // Asserts
        assertEquals(username, userDetails.getUsername());
        assertEquals(rawPassword, userDetails.getPassword());
    }

    @Test
    @DisplayName("loadUserByUsername кидает UsernameNotFoundException для несуществующего пользователя")
    void shouldThrowUsernameNotFoundExceptionWhenUserNotExists()
    {
        String username = "nonexistentuser";
        assertFalse(userRepository.existsByUsername(username));

        UsernameNotFoundException thrown =
                assertThrows(UsernameNotFoundException.class, () -> {
                    userDetailsService.loadUserByUsername(username);
                });

        assertTrue(thrown.getMessage().contains("User not found: " + username));
    }

    @Test
    void shouldUserDetailsBeEnabled()
    {
        String username = "existinguser";
        String rawPassword = "secret123";

        User user = new User();
        user.setUsername(username);
        user.setPassword(rawPassword);

        // Study mock
        Mockito.when(userRepository.findByUsername(Mockito.any()))
                .thenAnswer(e -> Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
    }
}