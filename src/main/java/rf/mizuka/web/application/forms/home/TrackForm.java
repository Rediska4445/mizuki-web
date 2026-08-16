package rf.mizuka.web.application.forms.home;

import rf.mizuka.web.application.database.entities.media.tracks.Track;

public record TrackForm(
        Track track,
        String base64Picture,
        String stringDuration
) { }