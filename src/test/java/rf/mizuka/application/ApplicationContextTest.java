package rf.mizuka.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import rf.mizuka.web.application.brokers.audio.AudioStreamConsumer;
import rf.mizuka.web.application.brokers.tracks.TrackConsumer;
import rf.mizuka.web.application.clients.storage.StorageClient;
import rf.mizuka.web.application.configurations.brokers.KafkaConfig;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:settings-test.properties")
public class ApplicationContextTest
{
    @MockitoBean
    private StorageClient storageClient;

    @MockitoBean
    private AudioStreamConsumer audioStreamConsumer;

    @MockitoBean
    private KafkaConfig kafkaConfig;

    @MockitoBean
    private TrackConsumer trackConsumer;

    // 1. Spring context should create
    @Test
    @DisplayName("Create spring context")
    void contextLoads() {}
}

