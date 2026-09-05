package rf.mizuka.web.application.brokers.tracks;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import rf.mizuka.web.application.brokers.BaseEvent;
import rf.mizuka.web.application.brokers.tracks.events.TrackLikeEvent;

@Slf4j
@Component
public class TrackProducer
{
    protected static final String TRACK_LIKES_TOPIC
            = "track-likes";
    private final KafkaTemplate<String, BaseEvent> kafkaTemplate;

    public TrackProducer(KafkaTemplate<String, BaseEvent> kafkaTemplate)
    {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void logTrackLikeAction(TrackLikeEvent event)
    {
        log.info("produce event: {}", event);

        kafkaTemplate.send(TRACK_LIKES_TOPIC, event.key(), event);
    }
}
