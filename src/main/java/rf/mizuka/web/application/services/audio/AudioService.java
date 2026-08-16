package rf.mizuka.web.application.services.audio;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public final class AudioService
{
    private final AudioMetadataService audioMetadataService;

    public AudioService(AudioMetadataService audioMetadataService)
    {
        this.audioMetadataService = audioMetadataService;
    }

    public AudioMetadataService audioMetadataService()
    {
        return audioMetadataService;
    }

    public ResourceRegion resourceRegion
            (Resource resource, HttpHeaders headers) throws IOException
    {
        long chunkSize = 1024 * 1024;
        long contentLength = resource.contentLength();
        HttpRange range = headers.getRange().stream().findFirst().orElse(null);

        if (range != null) {
            final long start = range.getRangeStart(contentLength);
            return new ResourceRegion(resource, start, Math.min(chunkSize, range.getRangeEnd(contentLength) - start + 1));
        } else {
            return new ResourceRegion(resource, 0, Math.min(chunkSize, contentLength));
        }
    }
}
