package rf.mizuka.web.application.controllers.home;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import rf.mizuka.web.application.database.entities.user.User;
import rf.mizuka.web.application.database.repository.user.UserRepository;
import rf.mizuka.web.application.forms.home.TrackForm;
import rf.mizuka.web.application.services.storage.StorageService;
import rf.mizuka.web.application.services.tracks.TrackService;

import java.security.Principal;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/")
public class HomeController
{
    private final StorageService storageService;
    private final TrackService trackService;


    public HomeController(
            StorageService storageService,
            TrackService trackService
    ) {
        this.storageService = storageService;
        this.trackService = trackService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String home(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false, defaultValue = "0") int size,
            Principal principal,
            Model model
    ) {
        Page<TrackForm> tracks = trackService.searchTracks(user, query, size);

        model.addAttribute("tracks", tracks.getContent());
        model.addAttribute("username", principal.getName());

        return "home";
    }
}