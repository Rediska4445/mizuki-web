package rf.mizuka.web.application.brokers.audio.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import rf.mizuka.web.application.brokers.BaseEvent;

import java.util.Objects;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TrackListenEvent
        extends BaseEvent<String>
{
    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("trackId")
    private Long trackId;

    @Override
    public String key()
    {
        return String.valueOf(Objects.hash(userId, trackId));
    }
}
