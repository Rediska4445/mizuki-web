package rf.mizuka.web.application.controllers.home;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import rf.mizuka.web.application.forms.home.TrackForm;
import rf.mizuka.web.application.services.tracks.TrackService;

import java.security.Principal;

@Controller
@RequestMapping("/")
public final class HomeController
{
    private final TrackService trackService;

    public HomeController(TrackService trackService)
    {
        this.trackService = trackService;
    }

    @GetMapping
    public String tracks(
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false, defaultValue = "0") int size,
            Model model,
            Principal principal
    ) {
        Page<TrackForm> tracks = trackService.searchTracks(query, size).map(
                e -> new TrackForm(
                        e,
                        trackService.encodeBase64Picture(e),
                        trackService.audioService().audioMetadataService().convertDurationToString((int) e.getDuration().getSeconds())
                ));

        String username = principal.getName().equals("default") ? "default" : principal.getName();

        model.addAttribute("username", username);
        model.addAttribute("tracks", tracks.getContent());

        return "home";
    }
}