package rf.mizuka;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import rf.mizuka.web.application.brokers.audio.AudioStreamConsumer;
import rf.mizuka.web.application.brokers.audio.AudioStreamProducer;
import rf.mizuka.web.application.brokers.tracks.TrackConsumer;
import rf.mizuka.web.application.brokers.tracks.TrackProducer;
import rf.mizuka.web.application.configurations.brokers.KafkaConfig;
import rf.mizuka.web.application.database.repository.user.UserRepository;
import rf.mizuka.web.application.services.storage.StorageService;
import rf.mizuka.web.application.services.tracks.TrackService;
import rf.mizuka.web.application.services.user.UserService;

public class Base
{
    @MockitoBean
    protected StorageService storageService;

    @MockitoBean
    protected AudioStreamConsumer audioStreamConsumer;

    @MockitoBean
    protected TrackConsumer trackConsumer;

    @MockitoBean
    protected TrackProducer trackProducer;

    @MockitoBean
    protected AudioStreamProducer audioStreamProducer;

    @MockitoBean
    protected KafkaConfig kafkaConfig;

    @MockitoBean
    protected TrackService trackService;

    @MockitoBean
    protected UserService userService;

    @MockitoBean
    protected UserRepository userRepository;

    @MockitoBean
    protected UserDetailsService userDetailsService;

    @MockitoBean
    protected PasswordEncoder passwordEncoder;
}
