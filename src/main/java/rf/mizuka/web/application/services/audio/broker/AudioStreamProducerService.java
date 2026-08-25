package rf.mizuka.web.application.services.audio.broker;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class AudioStreamProducerService
{
    protected static final String TRACK_LISTENS_TOPIC
            = "track-listens";

    private final KafkaTemplate<Long, Long> kafkaTemplate;

    public AudioStreamProducerService(KafkaTemplate<Long, Long> kafkaTemplate)
    {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void logTrackListen(Long userId, Long trackId)
    {
        kafkaTemplate.send(TRACK_LISTENS_TOPIC, userId, trackId);
    }
}
