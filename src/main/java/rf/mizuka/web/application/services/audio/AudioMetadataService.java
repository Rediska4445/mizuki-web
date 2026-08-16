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
    public record Metadata(String title, Set<String> authors, Duration Duration, byte[] rawImage) {}

    public String convertDurationToString(int totalSeconds)
    {
        return totalSeconds < 3600 ?
                String.format("%d:%d", (totalSeconds % 3600) / 60, totalSeconds % 60)
                : String.format("%d%d:%d", totalSeconds / 3600, (totalSeconds % 3600) / 60, totalSeconds % 60);
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
