package rf.mizuka.web.application.brokers.audio;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import rf.mizuka.web.application.brokers.audio.events.TrackListenEvent;
import rf.mizuka.web.application.services.tracks.TrackService;

import java.util.List;

import static rf.mizuka.web.application.brokers.audio.AudioStreamProducer.TRACK_LISTENS_TOPIC;

@Slf4j
@Component
@Lazy(false)
public class AudioStreamConsumer
{
    private final TrackService trackService;

    public AudioStreamConsumer(TrackService trackService)
    {
        this.trackService = trackService;
    }

    @KafkaListener(
            topics = TRACK_LISTENS_TOPIC,
            groupId = "mizuki-listeners-group",
            containerFactory = "mizukiKafkaListenerContainerFactory",
            properties =
            {
                "max.poll.records=${mizuki.kafka.plays.max-records:50}",
                "fetch.max.wait.ms=${mizuki.kafka.plays.max-wait-ms:10000}",
                "fetch.min.bytes=${mizuki.kafka.plays.fetch.min.bytes:5}"
            }
    )
    public void processListensBatch(List<TrackListenEvent> records)
    {
        log.info("consume event: {}", records);

        trackService.saveAggregatedListens(records);
    }
}