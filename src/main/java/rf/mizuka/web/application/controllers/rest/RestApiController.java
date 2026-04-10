package rf.mizuka.web.application.controllers.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import rf.mizuka.web.application.services.rest.AuthApiService;
import rf.mizuka.web.application.services.user.UserService;
import rf.mizuka.web.application.security.config.SecurityConfig;

import java.util.HashMap;
import java.util.Map;

@org.springframework.web.bind.annotation.RestController
@RequestMapping("/api")
public class RestApiController {
    private UserService userService;
    private final AuthenticationManager authenticationManager;
    private final AuthApiService authApiService;

    public RestApiController(UserService userService, AuthenticationManager authenticationManager, AuthApiService authApiService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.authApiService = authApiService;
    }

    @PostMapping("/auth/token")
    public Map<String, String> getToken(@RequestBody Map<String, String> request) {
        String clientId = request.get("clientId");
        String clientSecret = request.get("clientSecret");

        String token = authApiService.login(clientId, clientSecret);

        Map<String, String> response = new HashMap<>();
        response.put("access_token", token);
        response.put("token_type", "Bearer");
        return response;
    }

    @GetMapping("/teapot")
    public ResponseEntity<Map<String, String>> brewCoffee() {
        return ResponseEntity.status(418).body(Map.of(
                "error", "I'm a teapot",
                "access", "GRANTED_BUT_NO_CAFFEINE",
                "hint", "Token is valid, but the kettle is not a coffee machine. Go to the kitchen for real beans."
        ));
    }
}