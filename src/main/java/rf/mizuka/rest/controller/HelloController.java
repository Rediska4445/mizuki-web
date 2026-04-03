package rf.mizuka.rest.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import rf.mizuka.rest.model.UserRequest;

@RestController
public class HelloController {

    @PostMapping("/hello")
    public String hello(@RequestBody UserRequest request) {
        return String.format("Hello %s!", request.getName());
    }
}
