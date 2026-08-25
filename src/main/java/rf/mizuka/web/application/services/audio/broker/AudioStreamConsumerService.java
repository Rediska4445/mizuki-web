package rf.mizuka.web.application.services.audio.broker;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rf.mizuka.web.application.database.repository.TrackRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static rf.mizuka.web.application.services.audio.broker.AudioStreamProducerService.TRACK_LISTENS_TOPIC;

@Service
@Lazy(false)
public class AudioStreamConsumerService
{
    private final TrackRepository trackRepository;

    public AudioStreamConsumerService(TrackRepository trackRepository)
    {
        this.trackRepository = trackRepository;
    }

    @KafkaListener(
            topics = TRACK_LISTENS_TOPIC,
            groupId = "mizuki-listeners-group",
            containerFactory = "mizukiKafkaListenerContainerFactory"
    )
    @Transactional(rollbackFor = Exception.class)
    public void processListensBatch(List<ConsumerRecord<Long, Long>> records)
    {
        Map<Long, Long> userAggregatedStats = records.stream()
                .filter(record -> record.key() != null)
                .collect(Collectors.groupingBy(
                        ConsumerRecord::value,
                        Collectors.counting()
                ));

        userAggregatedStats.forEach(trackRepository::addUserListenCount);
    }
}