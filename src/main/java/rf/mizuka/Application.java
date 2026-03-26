package rf.mizuka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
@RestController
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@PostMapping("/hello")
	public String hello(@RequestBody UserRequest request) {
		return String.format("Hello %s!", request.getName());
	}
}
