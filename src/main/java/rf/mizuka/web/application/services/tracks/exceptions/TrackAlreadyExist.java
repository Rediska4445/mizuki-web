package rf.mizuka.web.application.services.tracks.exceptions;

public class TrackAlreadyExist
        extends RuntimeException
{
    public TrackAlreadyExist(String message)
    {
        super(message);
    }
}
