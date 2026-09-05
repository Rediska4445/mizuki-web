package rf.mizuka.web.application.brokers.tracks;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import rf.mizuka.web.application.brokers.tracks.events.TrackLikeEvent;
import rf.mizuka.web.application.services.tracks.TrackService;

import java.util.List;

import static rf.mizuka.web.application.brokers.tracks.TrackProducer.TRACK_LIKES_TOPIC;

@Slf4j
@Component
@Lazy(false)
public class TrackConsumer
{
    private final TrackService trackService;

    public TrackConsumer(TrackService trackService)
    {
        this.trackService = trackService;
    }

    @KafkaListener(
            topics = TRACK_LIKES_TOPIC,
            groupId = "mizuki-likes-group",
            containerFactory = "mizukiKafkaListenerContainerFactory",
            properties =
            {
                "max.poll.records=${mizuki.kafka.likes.max-records:20}",
                "fetch.max.wait.ms=${mizuki.kafka.likes.max-wait-ms:5000}",
                "fetch.min.bytes=${mizuki.kafka.likes.fetch.min.bytes:1}"
            }
    )
    public void processLikesBatch(List<TrackLikeEvent> records)
    {
        log.info("consume event: {}", records);

        trackService.saveAggregatedLikes(records);
    }
}
