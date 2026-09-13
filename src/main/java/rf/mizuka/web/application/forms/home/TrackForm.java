package rf.mizuka.web.application.forms.home;

import lombok.*;
import rf.mizuka.web.application.database.entities.media.tracks.Track;

@Data
@Builder
@NoArgsConstructor(staticName = "empty")
@AllArgsConstructor(staticName = "of")
public class TrackForm // record is inconvenient
{
    Track track;
    String trackUrl;
    String pictureUrl;
    String stringDuration;
    Boolean isLike;
}