package rf.mizuka.web.application.controllers.tracks;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import rf.mizuka.web.application.brokers.tracks.TrackProducer;
import rf.mizuka.web.application.brokers.tracks.events.TrackLikeEvent;
import rf.mizuka.web.application.database.entities.media.authors.Author;
import rf.mizuka.web.application.database.entities.media.tracks.Track;
import rf.mizuka.web.application.database.entities.user.User;
import rf.mizuka.web.application.forms.home.TrackForm;
import rf.mizuka.web.application.services.audio.metadata.exceptions.UnknownAuthorException;
import rf.mizuka.web.application.services.audio.metadata.exceptions.UnknownTitleException;
import rf.mizuka.web.application.services.tracks.exceptions.TrackAlreadyExist;
import rf.mizuka.web.application.services.tracks.TrackService;

import java.io.IOException;
import java.security.Principal;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/tracks")
public final class TracksController
{
    private final TrackProducer trackProducer;
    private final TrackService trackService;

    public TracksController(TrackProducer trackProducer, TrackService trackService)
    {
        this.trackProducer = trackProducer;
        this.trackService = trackService;
    }

    @ResponseBody
    @GetMapping(path = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<TrackForm>> searchTracksJson(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false, defaultValue = "10") int size)
    {
        Page<TrackForm> tracksPage = trackService.searchTracks(user, query, size);

        log.info("searchTracksJson endpoint returned: {}", tracksPage);

        return ResponseEntity.ok(tracksPage);
    }

    @GetMapping(path = "/search", produces = MediaType.TEXT_HTML_VALUE)
    public String searchTracksHtml(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false, defaultValue = "10") int size,
            Model model
    ) {
        Page<TrackForm> tracksPage = trackService.searchTracks(user, query, size);

        model.addAttribute("tracks", tracksPage.getContent());
        model.addAttribute("searchQuery", query);

        log.info("searchTracksHtml endpoint returned: {}", tracksPage);

        return "fragments/tracks/tracks-fragment :: tracks-list";
    }

    @GetMapping("/upload")
    public String getUploadPage(
            Principal principal,
            Model model
    ) {
        model.addAttribute("username", principal.getName());

        return "pages/tracks/upload-track";
    }

    @PostMapping("/like")
    public ResponseEntity<Void> likeTrack(
            @AuthenticationPrincipal User user,
            @RequestParam("trackId") Long trackId,
            @RequestParam("isLike") Boolean isLike
    ) {
        if(trackId < 0)
            throw new IllegalArgumentException("trackId must be non-null");

        trackProducer.logTrackLikeAction(TrackLikeEvent.builder()
                .trackId(trackId)
                .userId(user.getId())
                .isLike(isLike)
                .build()
        );

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .build();
    }

    @PostMapping(path = "/upload")
    public String uploadTrack(
            @AuthenticationPrincipal User fileOwner,
            @RequestParam("file") MultipartFile trackFile,
            @RequestParam("cover") MultipartFile coverFile,
            @RequestParam(value = "track", required = false) Track track,
            @RequestParam(value = "title") String title,
            @RequestParam(value = "authors") String authors,
            @RequestParam(value = "duration") Integer durationInMs,
            @RequestParam(value = "explicit") Boolean isExplicit,
            RedirectAttributes redirect
    ) throws Exception {
        if (!trackFile.isEmpty())
        {
            try
            {
                if(track == null)
                    track = new Track();

                if(track.getFileOwner() == null)
                    track.setFileOwner(fileOwner);

                track.setTitle(title);
                track.setAuthors(Arrays.stream(authors.split(TrackService.AUTHORS_SEPARATOR)).map(Author::new).collect(Collectors.toSet()));
                track.setDuration(Duration.ofMillis(durationInMs));
                track.setIsExplicit(isExplicit);

                redirect.addFlashAttribute("message", "Uploaded: "
                        + trackService.saveTrack(track, coverFile, trackFile).getTitle());
            }
            catch (TrackAlreadyExist | UnknownTitleException | UnknownAuthorException e)
            {
                log.warn("Failed to upload track due to validation error: {}", e.getMessage(), e);

                redirect.addAttribute("error", e.getMessage());
            }
            catch (IOException e)
            {
                log.error("An I/O error occurred while saving the file to storage", e);

                redirect.addAttribute("error", "Unknown client error");
            }
        }
        else
        {
            log.error("Transferred file is empty");

            redirect.addAttribute("error", "File is empty");
        }

        return "redirect:/tracks/upload";
    }
}