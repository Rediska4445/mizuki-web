package rf.mizuka.web.application.brokers.tracks.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.checkerframework.checker.units.qual.K;
import rf.mizuka.web.application.brokers.BaseEvent;

import java.util.Objects;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TrackLikeEvent
        extends BaseEvent<String>
{
    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("trackId")
    private Long trackId;

    @JsonProperty("isLike")
    private boolean isLike;

    @Override
    public String key()
    {
        return String.valueOf(Objects.hash(userId, trackId));
    }
}
