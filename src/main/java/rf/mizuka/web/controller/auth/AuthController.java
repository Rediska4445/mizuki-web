package rf.mizuka.web.controller.auth;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import rf.mizuka.web.dto.form.auth.LoginForm;
import rf.mizuka.web.dto.form.auth.RegisterForm;

@Controller
@RequestMapping("/auth")
public class AuthController {
    @GetMapping("/login")
    public String auth(Model model) {
        model.addAttribute("loginForm", new LoginForm());

        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registerForm", new RegisterForm());

        return "auth/register";
    }

    @PostMapping("/login")
    public String login(@ModelAttribute("loginForm") LoginForm loginForm, Model model) {
        String name = loginForm.getUsername();
        String pass = loginForm.getPassword();

        System.out.println("[+] Login User: Name: " + name + ", Password: " + pass);

        return "redirect:/";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("registerForm") RegisterForm registerForm, Model model) {
        String name = registerForm.getUsername();
        String pass = registerForm.getPassword();

        System.out.println("[+] Register User: Name: " + name + ", Password: " + pass);

        if(registerForm.getPassword().equals(registerForm.getConfirmPassword())) {
            return "redirect:/";
        } else {
            model.addAttribute("registerError", "Password and confirm password not equals!");
            model.addAttribute("registerForm", new RegisterForm());
        }

        return "auth/register";
    }
}