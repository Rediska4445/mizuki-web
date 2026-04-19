package rf.mizuka.web.application.controllers.developer;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import rf.mizuka.web.application.database.rest.repository.DeveloperRepository;
import rf.mizuka.web.application.models.rest.Application;
import rf.mizuka.web.application.models.user.User;
import rf.mizuka.web.application.services.user.UserService;


@Controller
@RequestMapping("/developers/developer-dashboard")
public class DeveloperDashboardController {
    private final DeveloperRepository developerRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    public DeveloperDashboardController(DeveloperRepository developerRepository,
                                        PasswordEncoder passwordEncoder,
                                        UserService userService) {
        this.developerRepository = developerRepository;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
    }

    @GetMapping
    public String showDashboard(Model model, Principal principal) {
        User user = userService.findByUsername(principal.getName());

        List<Application> myApps = developerRepository.findAllByOwner(user);

        model.addAttribute("apps", myApps);

        return "developers/developer-dashboard";
    }

    @PostMapping("/create")
    public String createNewApp(@RequestParam String appName,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        User user = userService.findByUsername(principal.getName());

        String clientId = UUID.randomUUID().toString();
        String rawSecret = UUID.randomUUID().toString().replace("-", "");

        Application newDev = new Application();
        newDev.setClientId(clientId);
        newDev.setClientSecret(passwordEncoder.encode(rawSecret));
        newDev.setDeveloperName(appName);
        newDev.setOwner(user);
        newDev.setScopes(Set.of("read"));

        developerRepository.save(newDev);

        redirectAttributes.addFlashAttribute("newAppSecret", rawSecret);
        redirectAttributes.addFlashAttribute("newAppClientId", clientId);

        return "redirect:/developers/developer-dashboard";
    }

    @PostMapping("/regenerate")
    public String regenerateSecret(@RequestParam Long appId,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes)
    {
        User user = userService.findByUsername(principal.getName());

        Application dev = developerRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        if (!(dev.getOwner().getId() == user.getId())) {
            throw new RuntimeException("Access denied: You are not the owner of this app");
        }

        String newRawSecret = UUID.randomUUID().toString().replace("-", "");

        dev.setClientSecret(passwordEncoder.encode(newRawSecret));
        developerRepository.save(dev);

        redirectAttributes.addFlashAttribute("newAppSecret", newRawSecret);
        redirectAttributes.addFlashAttribute("newAppClientId", dev.getClientId());
        redirectAttributes.addFlashAttribute("regenerated", true);

        return "redirect:/developers/developer-dashboard";
    }
}