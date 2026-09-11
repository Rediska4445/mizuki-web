package rf.mizuka.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import rf.mizuka.web.application.configurations.security.SecurityConfig;
import rf.mizuka.web.application.controllers.audio.AudioController;
import rf.mizuka.web.application.controllers.tracks.TracksController;
import rf.mizuka.web.application.database.repository.user.UserRepository;
import rf.mizuka.web.application.services.audio.AudioService;
import rf.mizuka.web.application.services.storage.StorageService;
import rf.mizuka.web.application.services.tracks.TrackService;
import rf.mizuka.web.application.services.user.UserService;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestPropertySource(locations = "classpath:settings-test.properties")
@WebMvcTest
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
public class HomeControllerTest
{
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StorageService storageService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private TrackService trackService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private AudioService audioService;

    @MockitoBean(name = "bCryptPasswordEncoder")
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private TracksController tracksController;

    @MockitoBean
    private AudioController audioController;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private CacheManager cacheManager;

    @BeforeEach
    void setup()
    {
        CacheManager realCacheManager = new ConcurrentMapCacheManager();
        Mockito.when(cacheManager.getCache(Mockito.anyString()))
                .thenAnswer(invocation -> realCacheManager.getCache(invocation.getArgument(0)));

        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        org.springframework.data.domain.Page emptyPage =
                org.springframework.data.domain.Page.empty();

        org.mockito.Mockito.when(trackService.searchTracks(Mockito.any(), org.mockito.Mockito.anyString(), org.mockito.Mockito.anyInt()))
                .thenReturn(emptyPage);
    }

    @Test
    @WithMockUser
    void authenticatedUserShouldSeeHomePage()
            throws Exception
    {
        mockMvc.perform(get("/").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("home"));
    }
}