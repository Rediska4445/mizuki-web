package rf.mizuka.web.application.configurations.brokers;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.converter.BatchMessagingMessageConverter;
import org.springframework.kafka.support.converter.ByteArrayJacksonJsonMessageConverter;
import org.springframework.kafka.support.converter.JacksonJsonMessageConverter;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import rf.mizuka.web.application.brokers.BaseEvent;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig
{
    private final String bootstrapServers;
    private final String groupId;
    private final Class<?> keyDeserializer;
    private final Class<?> valueDeserializer;
    private final Class<?> keySerializer;
    private final Class<?> valueSerializer;
    private final int maxPollRecords;

    public KafkaConfig(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.consumer.group-id}") String groupId,
            @Value("${spring.kafka.consumer.key-deserializer}") Class<?> keyDeserializer,
            @Value("${spring.kafka.consumer.value-deserializer}") Class<?> valueDeserializer,
            @Value("${spring.kafka.producer.key-serializer}") Class<?> keySerializer,
            @Value("${spring.kafka.producer.value-serializer}") Class<?> valueSerializer,
            @Value("${spring.kafka.consumer.max-poll-records}") int maxPollRecords
    ) {
        this.bootstrapServers = bootstrapServers;
        this.groupId = groupId;
        this.keyDeserializer = keyDeserializer;
        this.valueDeserializer = valueDeserializer;
        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;
        this.maxPollRecords = maxPollRecords;
    }

    @Bean
    public KafkaTemplate<String, BaseEvent> kafkaTemplate()
    {
        return new KafkaTemplate<>(producerFactory());
    }

    // Producer for send
    @Bean
    public ProducerFactory<String, BaseEvent> producerFactory()
    {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);
        configProps.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, true);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    // Consumer for make
    @Bean
    public ConsumerFactory<String, Object> consumerFactory()
    {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        props.put("spring.json.trusted.packages", "rf.mizuka.*");

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ByteArrayJacksonJsonMessageConverter jsonMessageConverter()
    {
        return new ByteArrayJacksonJsonMessageConverter();
    }

    @Bean("mizukiKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> mizukiKafkaListenerContainerFactory()
    {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        JacksonJsonMessageConverter jsonConverter = new JacksonJsonMessageConverter();
        jsonConverter.getTypeMapper().addTrustedPackages("rf.mizuka.*");

        factory.setConsumerFactory(consumerFactory());
        factory.setBatchListener(true);
        factory.setBatchMessageConverter(new BatchMessagingMessageConverter(jsonConverter));
        factory.setCommonErrorHandler(new org.springframework.kafka.listener.DefaultErrorHandler());

        return factory;
    }
}