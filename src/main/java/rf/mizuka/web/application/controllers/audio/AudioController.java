package rf.mizuka.web.application.controllers.audio;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import rf.mizuka.web.application.database.entities.media.tracks.Track;
import rf.mizuka.web.application.forms.home.TrackForm;
import rf.mizuka.web.application.services.storage.StorageService;
import rf.mizuka.web.application.services.storage.exceptions.PresignedUrlException;
import rf.mizuka.web.application.services.tracks.TrackService;

import java.awt.*;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/audio")
public class AudioController
{
    private final TrackService trackService;
    private final StorageService storageService;

    public AudioController(TrackService trackService, StorageService storageService)
    {
        this.trackService = trackService;
        this.storageService = storageService;
    }

    @ResponseBody
    @GetMapping(value = "/stream/{id}", produces = "audio/mpeg")
    public ResponseEntity<?> currentAudio(
            @PathVariable Long id,
            jakarta.servlet.http.HttpSession session
    )
            throws PresignedUrlException
    {
        final Optional<Track> idTrack = trackService.trackRepository().findById(id);

        if(idTrack.isEmpty())
            return ResponseEntity.notFound().build();

        session.setAttribute("currentTrackId", id);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaType.valueOf("audio/mpeg"))
                .body(storageService.getTrackPresignedUrl(idTrack.get().getFilePath()));
    }

    @GetMapping("/{trackId}")
    public ResponseEntity<?> getTrack(@PathVariable Long trackId)
            throws PresignedUrlException
    {
        if (trackId == null)
        {
            return ResponseEntity.ok(Map.of("active", false));
        }

        Optional<Track> trackOpt = trackService.trackRepository().findById(trackId);
        if (trackOpt.isEmpty())
        {
            return ResponseEntity.ok(Map.of("active", false));
        }

        Track track = trackOpt.get();

        return buildTrackApiAnswer(track);
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentTrack(HttpSession session)
            throws PresignedUrlException
    {
        Long trackId = (Long) session.getAttribute("currentTrackId");
        if (trackId == null)
        {
            return ResponseEntity.ok(Map.of("active", false));
        }

        Optional<Track> trackOpt = trackService.trackRepository().findById(trackId);
        if (trackOpt.isEmpty())
        {
            return ResponseEntity.ok(Map.of("active", false));
        }

        Track track = trackOpt.get();

        return buildTrackApiAnswer(track);
    }

    private ResponseEntity<?> buildTrackApiAnswer(Track track)
            throws PresignedUrlException
    {
        return ResponseEntity.ok(Map.of(
            "active", true,
            "trackId", track.getId().toString(),
            "trackPath", storageService.getTrackPresignedUrl(track.getFilePath()),
            "title", track.getTitle(),
            "picturePath", storageService.getTrackPictureUrl(track.getPicturePath()),
            "color", (track.getColor() == null ? Color.WHITE : track.getColor()), // May be null
            "author", trackService.joinAuthors(track.getAuthors())
        ));
    }
}
