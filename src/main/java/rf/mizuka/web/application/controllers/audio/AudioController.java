package rf.mizuka.web.application.controllers.audio;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import rf.mizuka.io.AudioInputStream;
import rf.mizuka.web.application.brokers.audio.AudioStreamProducer;
import rf.mizuka.web.application.brokers.audio.events.TrackListenEvent;
import rf.mizuka.web.application.database.entities.media.tracks.Track;
import rf.mizuka.web.application.database.entities.user.User;
import rf.mizuka.web.application.services.authors.AuthorService;
import rf.mizuka.web.application.services.storage.StorageService;
import rf.mizuka.web.application.services.storage.exceptions.PresignedUrlException;
import rf.mizuka.web.application.services.tracks.TrackService;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Optional;

import static rf.mizuka.utilities.AudioUtilities.getResourceRegion;

@Slf4j
@Controller
@RequestMapping("/audio")
public class AudioController
{
    private final AudioStreamProducer audioStreamProducer;
    private final TrackService trackService;
    private final AuthorService authorService;
    private final StorageService storageService;

    public AudioController(AudioStreamProducer audioStreamProducer, TrackService trackService, AuthorService authorService, StorageService storageService)
    {
        this.audioStreamProducer = audioStreamProducer;
        this.trackService = trackService;
        this.authorService = authorService;
        this.storageService = storageService;
    }

    @ResponseBody
    @GetMapping(value = "/stream/{trackId}")
    public void streamAudio(
            @PathVariable Long trackId,
            @RequestHeader HttpHeaders headers,
            jakarta.servlet.http.HttpSession session,
            HttpServletResponse response
    ) throws PresignedUrlException, IOException {
        final Optional<Track> idTrack
                = trackService.trackRepository().findById(trackId);

        if(idTrack.isEmpty())
        {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        session.setAttribute("currentTrackId", trackId);

        Resource audioResource = storageService.getTrackResource(idTrack.get().getFilePath());
        ResourceRegion region = getResourceRegion(audioResource, headers);

        response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");

        long start = region.getPosition();
        long length = region.getCount();
        long end = start + length - 1;
        long totalLength = audioResource.contentLength();

        response.setContentType("audio/mpeg");

        if (headers.getRange().isEmpty())
        {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(totalLength));
            return;
        }
        else
        {
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + totalLength);
            response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(length));
        }

        try (InputStream chunkedStream
                     = new AudioInputStream(audioResource.getInputStream(), start, length);
             OutputStream outputStream
                     = response.getOutputStream())
        {
            chunkedStream.transferTo(outputStream);
            outputStream.flush();
        }
        catch (AsyncRequestNotUsableException | ClientAbortException e)
        {
            log.warn(e.getMessage());
        }
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(value = "/stream/{trackId}/complete")
    public void completeStreamAudio(
            @PathVariable Long trackId,
            @AuthenticationPrincipal User currentUser
    ) {
        audioStreamProducer.logTrackListen(TrackListenEvent.builder()
                .userId(currentUser.getId())
                .trackId(trackId)
                .build()
        );
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

        return buildTrackApiAnswer(trackOpt.get());
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentTrack(HttpSession session)
            throws PresignedUrlException
    {
        return getTrack((Long) session.getAttribute("currentTrackId"));
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
            "author", authorService.joinAuthors(track.getAuthors())
        ));
    }
}
