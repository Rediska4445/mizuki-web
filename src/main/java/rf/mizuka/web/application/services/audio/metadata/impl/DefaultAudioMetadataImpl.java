package rf.mizuka.web.application.services.audio.metadata.impl;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import rf.mizuka.web.application.services.audio.metadata.exceptions.InvalidAudioDurationException;
import rf.mizuka.web.application.services.audio.metadata.exceptions.UnknownAuthorException;
import rf.mizuka.web.application.services.audio.metadata.exceptions.UnknownTitleException;
import rf.mizuka.web.application.services.audio.metadata.IAudioMetadata;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;

public final class DefaultAudioMetadataImpl
        implements IAudioMetadata
{
    @Override
    public Metadata extractMetadata(AudioMetadataFile tr)
            throws Exception
    {
        File file = new File(tr.filePath());
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

        return new IAudioMetadata.Metadata(
                title,
                new HashSet<>(Arrays.stream(authors.split(",")).toList()),
                Duration.ofSeconds(duration),
                rawImageData
        );
    }
}
