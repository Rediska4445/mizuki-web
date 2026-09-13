package rf.mizuka.utilities;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;

import java.io.IOException;
import java.util.List;

public class AudioUtilities
{
    public static ResourceRegion getResourceRegion(
            Resource resource, HttpHeaders headers
    ) throws IOException {
        long contentLength = resource.contentLength();
        long chunkSize = 1024 * 1024;

        List<HttpRange> ranges = headers.getRange();

        if (ranges.isEmpty())
        {
            long rangeLength = Math.min(chunkSize, contentLength);
            return new ResourceRegion(resource, 0, rangeLength);
        }
        else
        {
            HttpRange range = ranges.get(0);
            long start = range.getRangeStart(contentLength);
            long end = range.getRangeEnd(contentLength);
            long rangeLength = Math.min(chunkSize, end - start + 1);
            return new ResourceRegion(resource, start, rangeLength);
        }
    }
}
