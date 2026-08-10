package rf.mizuka.web.application.controllers.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import rf.mizuka.web.application.services.user.UserService;
import rf.mizuka.web.application.dto.auth.LoginForm;
import rf.mizuka.web.application.dto.auth.RegisterForm;

@Controller
@RequestMapping("/auth")
public final class AuthController
{
    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

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
    public String login(
            @ModelAttribute("loginForm") LoginForm loginForm,
            Model model
    ) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginForm.getUsername(), loginForm.getPassword())
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            return "redirect:/";
        } catch (AuthenticationException e) {
            model.addAttribute("loginError", "Invalid entered data.");
            model.addAttribute("loginForm", loginForm);

            return "auth/login";
        }
    }

    @PostMapping("/register")
    public String register(
            @ModelAttribute("registerForm") RegisterForm registerForm,
            Model model
    ) {
        try {
            if (!registerForm.getPassword().equals(registerForm.getConfirmPassword())) {
                model.addAttribute("registerError", "Passwords do not match.");
                return "auth/register";
            }

            userService.registerUser(registerForm.getUsername(), registerForm.getPassword());

            return "redirect:/auth/login";
        } catch (IllegalArgumentException | UserExistException e) {
            model.addAttribute("registerError", "Incorrect entered data.");

            return "auth/register";
        }
    }
}