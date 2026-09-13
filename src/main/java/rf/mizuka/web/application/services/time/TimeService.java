package rf.mizuka.web.application.services.time;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class TimeService
{
    public String now()
    {
        return now(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public String now(DateTimeFormatter format)
    {
        return LocalDateTime.now().format(format);
    }
}
