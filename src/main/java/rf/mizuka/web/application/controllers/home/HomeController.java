package rf.mizuka.web.application.controllers.home;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public final class HomeController
{
    @GetMapping
    public String home() {
        return "home";
    }
}