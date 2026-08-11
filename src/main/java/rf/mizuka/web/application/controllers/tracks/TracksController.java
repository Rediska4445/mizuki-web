package rf.mizuka.web.application.controllers.tracks;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import rf.mizuka.web.application.models.tracks.Track;
import rf.mizuka.web.application.services.audio.UnknownAuthorException;
import rf.mizuka.web.application.services.audio.UnknownTitleException;
import rf.mizuka.web.application.services.tracks.TrackService;

import java.io.IOException;
import java.security.Principal;

@Controller
@RequestMapping("/app/tracks")
public final class TracksController
{
    private final TrackService trackService;

    public TracksController(TrackService trackService) {
        this.trackService = trackService;
    }

    @GetMapping
    public String list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model,
            Principal principal
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").descending());

        Page<Track> trackPage = trackService.searchTracks(search, pageable);
        String username = principal.getName().equals("default") ? "default" : principal.getName();

        model.addAttribute("tracks", trackPage.getContent());
        model.addAttribute("username", username);
        model.addAttribute("searchQuery", search);
        model.addAttribute("currentPage", trackPage.getNumber());
        model.addAttribute("totalPages", trackPage.getTotalPages());
        model.addAttribute("totalElements", trackPage.getTotalElements());
        model.addAttribute("pageSize", size);

        return "app/tracks";
    }

    @PostMapping
    public String upload(@RequestParam("file") MultipartFile file, RedirectAttributes redirect)
            throws Exception
    {
        if (!file.isEmpty()) {
            try {
                Track savedTrack = trackService.saveTrack(file);

                redirect.addFlashAttribute("message", "Uploaded: " + savedTrack.getTitle());
            } catch (UnknownTitleException | UnknownAuthorException e) {
                redirect.addAttribute("error", e.getMessage());
            } catch (IOException e) {
                redirect.addAttribute("error", "Unknown client error");
            }
        } else {
            redirect.addAttribute("error", "File is empty");
        }

        return "redirect:/app/tracks";
    }
}