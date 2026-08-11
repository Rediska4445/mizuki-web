package rf.mizuka.web.application.services.audio;

public class UnknownTitleException
        extends Exception
{
    public UnknownTitleException() {

    }

    public UnknownTitleException(Throwable cause) {
        super(cause);
    }

    public UnknownTitleException(String message) {
        super(message);
    }

    public UnknownTitleException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnknownTitleException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
