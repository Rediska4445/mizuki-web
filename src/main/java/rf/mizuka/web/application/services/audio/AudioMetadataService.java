package rf.mizuka.web.application.services.audio;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.images.Artwork;
import org.springframework.stereotype.Service;
import rf.mizuka.web.application.database.entities.media.tracks.Track;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
public final class AudioMetadataService
{
    /* Constant be defined default value, which returned and presentation for undefined duration
     (from "convertDurationToString" method)
     */
    public static final String UNDEFINED_DURATION_PRESENT
            = "--:--";

    /* DTO for transferred outta "extractMetadata" method */
    public record Metadata(
            String title, Set<String> authors, Duration Duration, byte[] rawImage
    ) {}

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

    public Metadata extractMetadata(final Track tr)
            throws IOException,
                CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException,
                UnknownTitleException, UnknownAuthorException
    {
        File file = new File(tr.getFilePath());
        AudioFile audioFile = AudioFileIO.read(file);
        Tag tag = audioFile.getTag();

        String title = tag.getFirst(FieldKey.TITLE);
        if (title == null || title.isEmpty())
        {
            throw new UnknownTitleException("Unknown title");
        }

        String authors = tag.getFirst(FieldKey.ARTIST);
        if (authors == null || authors.isEmpty())
        {
            throw new UnknownAuthorException("Unknown authors");
        }

        int duration = audioFile.getAudioHeader().getTrackLength();
        if (duration <= 0)
        {
            throw new InvalidAudioDurationException("Duration must be positive");
        }

        byte[] rawImageData = null;

        Artwork artwork = tag.getFirstArtwork();
        if (artwork != null)
        {
            rawImageData = artwork.getBinaryData();
        }

        return new Metadata(
                title,
                new HashSet<>(Arrays.stream(authors.split(",")).toList()),
                Duration.ofSeconds(duration),
                rawImageData
        );
    }
}
