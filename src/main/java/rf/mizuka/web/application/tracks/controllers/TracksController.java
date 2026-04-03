package rf.mizuka.web.application.tracks.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import rf.mizuka.web.application.tracks.database.repository.TrackRepository;
import rf.mizuka.web.application.tracks.models.Track;

@Controller
@RequestMapping("/app")
public class TracksController {
    @Autowired
    private TrackRepository trackRepo;

    @GetMapping("/tracks")
    public String list(Model model) {
        model.addAttribute("tracks", trackRepo.findAll());

        return "app/tracks";
    }

    @PostMapping("/tracks")
    public String upload(@RequestParam("file") MultipartFile file, RedirectAttributes redirect) {
        if (!file.isEmpty()) {
            Track track = new Track();
            track.setName(file.getOriginalFilename());
            trackRepo.save(track);
            redirect.addFlashAttribute("message", "Загружен: " + file.getOriginalFilename());
        }

        return "redirect:/app/tracks";
    }
}