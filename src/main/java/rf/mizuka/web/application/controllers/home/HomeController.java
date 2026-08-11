package rf.mizuka.web.application.controllers.home;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import rf.mizuka.web.application.models.tracks.Track;
import rf.mizuka.web.application.services.tracks.TrackService;

import java.security.Principal;

@Controller
@RequestMapping("/")
public final class HomeController
{
    private final TrackService trackService;

    public HomeController(TrackService trackService) {
        this.trackService = trackService;
    }

    @GetMapping
    public String tracks(
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

        return "home";
    }
}