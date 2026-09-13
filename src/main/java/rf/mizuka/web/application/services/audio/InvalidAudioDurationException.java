package rf.mizuka.web.application.services.audio;

public class InvalidAudioDurationException
        extends RuntimeException
{
    public InvalidAudioDurationException(String message) {
        super(message);
    }
}
