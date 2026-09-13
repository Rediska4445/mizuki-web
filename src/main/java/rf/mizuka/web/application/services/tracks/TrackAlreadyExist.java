package rf.mizuka.web.application.services.tracks;

public class TrackAlreadyExist
        extends RuntimeException
{
    public TrackAlreadyExist(String message)
    {
        super(message);
    }
}
