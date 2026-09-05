package rf.mizuka.web.application.brokers;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import rf.mizuka.web.application.brokers.audio.events.TrackListenEvent;
import rf.mizuka.web.application.brokers.tracks.events.TrackLikeEvent;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TrackLikeEvent.class, name = "track_like"),
        @JsonSubTypes.Type(value = TrackListenEvent.class, name = "track_listen")
})
public abstract class BaseEvent<K>
{
    public abstract K key();
}