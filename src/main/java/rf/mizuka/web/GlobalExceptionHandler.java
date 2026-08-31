package rf.mizuka.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler
{
    @ExceptionHandler({
            // All possibles exceptions
            Exception.class, RuntimeException.class
    })
    public ResponseEntity<Map<String, String>> handleAnyException(Exception e)
    {
        log.error("exception by GlobalExceptionHandler-handleAnyException: ", e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "The server was rude to me :(",
                        "message", "ask the administrator to make the server apologize!"
                ));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentialsException(RuntimeException e)
    {
        log.error("exception by GlobalExceptionHandler-handleBadCredentialsException: ", e);

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Entity is unauthorized"));
    }
}
