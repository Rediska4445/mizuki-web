package rf.mizuka.web.application.services.audio.metadata;

import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.Set;

public interface IAudioMetadata
{
    Metadata extractMetadata(AudioMetadataFile tr)
            throws Exception;

    /* DTO for transferred OUTTA "extractMetadata" method */
    record Metadata(
            String title, Set<String> authors, Duration Duration, byte[] rawImage
    ) {}

    /* DTO for transferred INTO "extractMetadata" method */
    record AudioMetadataFile (
            String filePath, MultipartFile file
    ) {}
}
