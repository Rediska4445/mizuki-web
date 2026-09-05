package rf.mizuka.web.application.brokers.audio;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import rf.mizuka.web.application.brokers.BaseEvent;
import rf.mizuka.web.application.brokers.audio.events.TrackListenEvent;

@Slf4j
@Component
public class AudioStreamProducer
{
    protected static final String TRACK_LISTENS_TOPIC
            = "track-listens";

    private final KafkaTemplate<String, BaseEvent> kafkaTemplate;

    public AudioStreamProducer(KafkaTemplate<String, BaseEvent> kafkaTemplate)
    {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void logTrackListen(TrackListenEvent event)
    {
        log.info("produce event: {}", event);

        kafkaTemplate.send(TRACK_LISTENS_TOPIC, event.key(), event);
    }
}
