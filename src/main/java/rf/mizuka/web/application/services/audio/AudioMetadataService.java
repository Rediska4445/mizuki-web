package rf.mizuka.web.application.services.audio;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.springframework.stereotype.Service;
import rf.mizuka.web.application.models.tracks.Track;

import java.io.File;

@Service
public final class AudioMetadataService
{
    public Track extractMetadata(Track res)
            throws Exception
    {
        File file = new File(res.getFilePath());
        AudioFile audioFile = AudioFileIO.read(file);
        Tag tag = audioFile.getTag();

        res.setTitle(tag.getFirst(FieldKey.TITLE));
        if (res.getTitle() == null || res.getTitle().isEmpty()) {
            throw new UnknownTitleException("Unknown title");
        }

        res.setAuthor(tag.getFirst(FieldKey.ARTIST));
        if (res.getAuthor() == null || res.getAuthor().isEmpty()) {
            throw new UnknownAuthorException("Unknown author");
        }

        res.setDuration((short) audioFile.getAudioHeader().getTrackLength());

        Artwork artwork = tag.getFirstArtwork();

        if (artwork != null) {
            byte[] rawImageData = artwork.getBinaryData();
            res.setPicture(rawImageData);
        }

        return res;
    }

    public Track extractMetadata(String filePath)
            throws Exception
    {
        Track tr = new Track();
        tr.setFilePath(filePath);
        return extractMetadata(tr);
    }
}
