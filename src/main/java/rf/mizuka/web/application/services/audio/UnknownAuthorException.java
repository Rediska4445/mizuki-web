package rf.mizuka.web.application.services.audio;

public class UnknownAuthorException
        extends Exception
{
    public UnknownAuthorException()
    {}

    public UnknownAuthorException(Throwable cause)
    {
        super(cause);
    }

    public UnknownAuthorException(String message)
    {
        super(message);
    }

    public UnknownAuthorException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public UnknownAuthorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}