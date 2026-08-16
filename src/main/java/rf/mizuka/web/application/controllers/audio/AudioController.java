package rf.mizuka.web.application.controllers.audio;

import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import rf.mizuka.web.application.database.entities.media.authors.Author;
import rf.mizuka.web.application.database.entities.media.tracks.Track;
import rf.mizuka.web.application.services.tracks.TrackService;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/track")
public class AudioController
{
    private final TrackService trackService;

    public AudioController(TrackService trackService)
    {
        this.trackService = trackService;
    }

    @GetMapping(value = "/{id}", produces = "text/html")
    public String trackPage(@PathVariable Long id, Model model)
    {
        final Optional<Track> track = trackService.trackRepository().findById(id);
        if(track.isEmpty())
            model.addAttribute("error", String.format("Track with request id (%d) is not exist!", id));
        else
            model.addAttribute("track", track);

        return "app/tracks/track";
    }

    @ResponseBody
    @GetMapping(value = "/stream/{id}", produces = "audio/mpeg")
    public ResponseEntity<ResourceRegion> streamAudio(
            @PathVariable Long id,
            @RequestHeader HttpHeaders headers,
            jakarta.servlet.http.HttpSession session
    ) throws IOException {
        final Optional<Track> idTrack = trackService.trackRepository().findById(id);

        if(idTrack.isEmpty())
            return ResponseEntity.notFound().build();

        Resource audioFile = new FileSystemResource(Paths.get(idTrack.get().getFilePath()));
        if (!audioFile.exists())
            return ResponseEntity.notFound().build();

        session.setAttribute("currentTrackId", id);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaType.valueOf("audio/mpeg"))
                .body(trackService.audioService().resourceRegion(audioFile, headers));
    }

    @GetMapping("/{trackId}")
    public ResponseEntity<?> getTrack(@PathVariable Long trackId)
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

        String base64Picture = trackService.encodeBase64Picture(track);
        String coverSrc = (base64Picture != null && !base64Picture.isEmpty())
                ? "data:image/jpeg;base64," + base64Picture
                : "/img/logo-hd.png";

        return ResponseEntity.ok(Map.of(
                "active", true,
                "trackId", track.getId().toString(),
                "title", track.getTitle(),
                "color", track.getColor() == null ? Color.WHITE : track.getColor(), // May be null
                "cover", coverSrc,
                "author", String.join(",",  track.getAuthors().stream()
                        .map(Author::getName)
                        .collect(Collectors.joining(","))
                )));
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentTrack(HttpSession session)
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

        String base64Picture = trackService.encodeBase64Picture(track);
        String coverSrc = (base64Picture != null && !base64Picture.isEmpty())
                ? "data:image/jpeg;base64," + base64Picture
                : "/img/logo-hd.png";

        return ResponseEntity.ok(Map.of(
                "active", true,
                "trackId", track.getId().toString(),
                "title", track.getTitle(),
                "color", track.getColor() == null ? Color.WHITE : track.getColor(), // May be null
                "cover", coverSrc,
                "author", String.join(",",  track.getAuthors().stream()
                        .map(Author::getName)
                        .collect(Collectors.joining(","))
                )));
    }
}
