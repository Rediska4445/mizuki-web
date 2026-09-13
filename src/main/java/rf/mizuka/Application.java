package rf.mizuka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;

/**<h3>Mizuki-WEB</h3>
 * <p>
 * Application for listen and upload music-tracks.<br>
 * Based on <a href="https://github.com/Rediska4445/mizuki-player">Mizuki-Player</a>,
 * and provides too more service function for usually users.<br>
 * Official GitHub repository - <a href="https://github.com/Rediska4445/mizuki-web">Mizuki</a>
 * <p>
 * For further information, please refer to the relevant resources and documentation.<br>
 * All rights reserved!
 * @version 1.0
 * @author Rediska4445
 * */
@EnableKafka
@EnableCaching
@SpringBootApplication
public class Application
{
    /**
     * Start Mizuki-WEB application
     * */
	public static void main(String[] args)
    {
        // Start Spring application for start server
		SpringApplication.run(Application.class, args);
	}
}