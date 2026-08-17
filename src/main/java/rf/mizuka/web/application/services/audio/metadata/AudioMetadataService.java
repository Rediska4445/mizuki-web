package rf.mizuka.web.application.services.audio.metadata;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rf.mizuka.web.application.database.entities.media.tracks.Track;
import rf.mizuka.web.application.services.audio.metadata.impl.DefaultAudioMetadataImpl;
import rf.mizuka.web.application.services.file.FileService;

import java.time.Duration;

@Service
public final class AudioMetadataService
{
    /* Constant be defined default value, which returned and presentation for undefined duration
     (from "convertDurationToString" method)
     */
    public static final String UNDEFINED_DURATION_PRESENT
            = "--:--";

    // Default IAudioMetadata implementation for extract metadata
    private final IAudioMetadata iAudioMetadata
            = new DefaultAudioMetadataImpl();

    private final FileService fileService;

    public AudioMetadataService(FileService fileService)
    {
        this.fileService = fileService;
    }

    /**
     * Converts a track duration into a human-readable string format.
     * <p>
     * This method accepts instances of the standard {@link java.time.Duration} class
     * and generates a formatted duration taking into account hours, minutes, and seconds.
     * Leading zeros are handled exclusively for seconds (and minutes if hours are present).<br>
     * If the duration represents an invalid or NaN time state, the method returns the
     * constant {@code UNDEFINED_DURATION_PRESENT}.
     * <p>
     * Examples of behavior:
     * <pre>{@code
     * // 1. Duration less than a minute (5 seconds):
     * convertDurationToString(Duration.ofSeconds(5));
     * // Returns: "0:05"
     *
     * // 2. Duration over a minute, but less than an hour (1 minute 1 second):
     * convertDurationToString(Duration.ofSeconds(61));
     * // Returns: "1:01"
     *
     * // 3. Exact minutes boundary (2 minutes 0 seconds):
     * convertDurationToString(Duration.ofSeconds(120));
     * // Returns: "2:00"
     *
     * // 4. Exact hours boundary (1 hour 0 minutes 0 seconds):
     * convertDurationToString(Duration.ofSeconds(3600));
     * // Returns: "1:00:00"
     *
     * // 5. Duration with hours, minutes, and seconds (1 hour 1 minute 1 second):
     * convertDurationToString(Duration.ofSeconds(3661));
     * // Returns: "1:01:01"
     *
     * // 6. Invalid, negative or NaN duration handling:
     * convertDurationToString(Duration.ofSeconds(-10));
     * // Returns: UNDEFINED_DURATION_PRESENT (constant value, e.g., "--:--")
     *
     * // 7. Null safety check:
     * convertDurationToString(null);
     * // Returns: UNDEFINED_DURATION_PRESENT
     * }</pre>
     *
     * @param totalSeconds an instance of {@link java.time.Duration} representing the total duration
     * @return a human-readable formatted string, or {@code UNDEFINED_DURATION_PRESENT} if the duration is null, negative, or invalid
     */
    public String convertDurationToString(Duration totalSeconds)
    {
        if (totalSeconds == null || totalSeconds.isNegative())
        {
            return UNDEFINED_DURATION_PRESENT;
        }

        long hours = totalSeconds.toHours();
        if (hours > 0)
        {
            return String.format(
                    "%d:%02d:%02d", hours, totalSeconds.toMinutesPart(), totalSeconds.toSecondsPart()
            );
        }
        else
        {
            return String.format(
                    "%d:%02d", totalSeconds.toMinutesPart(), totalSeconds.toSecondsPart()
            );
        }
    }

    // Only for tests
    public IAudioMetadata.Metadata extractMetadata(Track tr)
            throws Exception
    {
        // File already exist
        return extractMetadata(new IAudioMetadata.AudioMetadataFile(tr.getFilePath(), null));
    }

    public IAudioMetadata.Metadata extractMetadata(MultipartFile tr)
            throws Exception
    {
        // Need to create file from MultipartFile
        return fileService.createTempFile("track-upload-", fileService.extractExtension(tr.getOriginalFilename()),
                tr,
                e -> {
                    try {
                        return extractMetadata(new IAudioMetadata.AudioMetadataFile(e.toFile().getAbsolutePath(), tr));
                    } catch (Exception ex) {
                        throw ex;
                    }
                }
        );
    }

    public IAudioMetadata.Metadata extractMetadata(IAudioMetadata.AudioMetadataFile tr)
            throws Exception
    {
        return iAudioMetadata.extractMetadata(tr);
    }
}
