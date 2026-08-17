package rf.mizuka.web.application.services.tracks;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rf.mizuka.web.application.database.entities.media.authors.Author;
import rf.mizuka.web.application.database.entities.media.tracks.Track;
import rf.mizuka.web.application.database.repository.AuthorRepository;
import rf.mizuka.web.application.database.repository.TrackRepository;
import rf.mizuka.web.application.services.audio.AudioService;
import rf.mizuka.web.application.services.audio.metadata.IAudioMetadata;
import rf.mizuka.web.application.services.color.ColorService;
import rf.mizuka.web.application.services.image.ImageService;
import rf.mizuka.web.application.services.storage.StorageService;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TrackService
{
    /* Dependency */
    private final ImageService imageService;
    private final AudioService audioService;
    private final TrackRepository trackRepository;
    private final AuthorRepository authorRepository;
    private final ColorService colorService;
    private final StorageService storageService;

    public TrackService(
            ImageService imageService, AudioService audioService, TrackRepository trackRepository, AuthorRepository authorRepository, ColorService colorService, StorageService storageService
    ) {
        this.imageService = imageService;
        this.audioService = audioService;
        this.trackRepository = trackRepository;
        this.authorRepository = authorRepository;
        this.colorService = colorService;
        this.storageService = storageService;
    }

    public AudioService audioService()
    {
        return audioService;
    }

    // That must not be affected from outside!
    public TrackRepository trackRepository()
    {
        return trackRepository;
    }

    public String joinAuthors(Collection<Author> authors)
    {
        return String.join(",",  authors.stream()
                .map(Author::getName)
                .collect(Collectors.joining(","))
        );
    }

    @org.springframework.transaction.annotation.Transactional(
            readOnly = true // need guarantee immutability data
    )
    public Track getTrack(Long id)
    {
        Track track = trackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Track not found"));

        track.setPicturePath(
                storageService.getTrackPictureUrl(track.getPicturePath())
        );
        track.setFilePath(
                storageService.getTrackPresignedUrl(track.getFilePath())
        );

        return track;
    }

    @org.springframework.transaction.annotation.Transactional(
            readOnly = true
    )
    public Page<Track> searchTracks(String query, int size)
    {
        Page<Track> trackPage = trackRepository.findByNameContaining(
                query, size <= 0 ? Pageable.unpaged() : Pageable.ofSize(size)
        );

        return trackPage;
    }

    /**
     * Detect color from track picture. Result must be in HEX format.
     * <p>
     * Note: The color detection logic is non-binding and subject to change.
     * <p>
     * Examples of behavior:
     * <pre>{@code
     * // 1. When colorService detects pure Blue color:
     * // Track contains bytes of an image where the dominant contrasting color is Blue (RGB: 0, 0, 255)
     * findMostContrastingColor(trackWithBlueImage);
     * // Returns: "#0000FF" (guaranteed 6 characters after '#' with leading zeros)
     *
     * // 2. When track has no picture bytes:
     * track.setPicture(null);
     * findMostContrastingColor(track);
     * // Returns: null
     *
     * // 3. Handling a null track reference:
     * findMostContrastingColor(null);
     * // Returns: null
     * }</pre>
     * @param   raw
     *          the byte array of picture to extract picture and after detect color
     * @return color in HEX format
     * **/
    @org.springframework.transaction.annotation.Transactional(
            readOnly = true
    )
    public String getColorFromAlbumArt(byte[] raw)
            throws IOException
    {
        return colorService.convertColorToHex(
                colorService.calculateMainColorFromImage(ImageIO.read(
                        new ByteArrayInputStream(raw)
                )));
    }

    @Transactional(rollbackOn = Exception.class)
    public Track saveTrack(Track track, MultipartFile file)
            throws Exception
    {
        // Upload track to file storage (AWS S3)
        String audioKey
                = storageService.uploadTrack(file);
        // Picture key for upload
        String audioPictureKey
                = null;

        // Set file to DB from file storage (in key view, not really path or URL)
        track.setFilePath(audioKey);

        try
        {
            IAudioMetadata.Metadata meta;

            // Check on exist metadata into track
            if(track.getTitle() != null
                    && track.getAuthors() != null
                    && track.getDuration() != null)
            {
                 meta = new IAudioMetadata.Metadata(
                        track.getTitle(),
                         track.getAuthors().stream().map(Author::getName).collect(Collectors.toSet()),
                         track.getDuration(),
                         imageService.getDefaultMusicImage()
                );
            }
            else
            {
                meta = audioService.audioMetadataService().extractMetadata(file);
            }

            // Immediately call before check on exist.
            Set<Author> authors = meta.authors().stream()
                    .map(authorRepository::buildOrGet)
                    .collect(Collectors.toSet());

            // If this track already exist
            if (existsByTitleAndExactAuthors(meta.title(), authors))
            {
                throw new TrackAlreadyExist("Track by these authors must be unique!");
            }

            // Main ease metadata for track
            track.setName(file.getName());
            track.setTitle(meta.title());
            track.setAuthors(authors);
            track.setDuration(meta.Duration());

            // Save picture to file storage
            track.setPicturePath(
                    audioPictureKey = storageService.uploadTrackPicture(meta.rawImage())
            );

            if(track.getColor() == null)
                track.setColor(getColorFromAlbumArt(meta.rawImage()));

            return trackRepository.save(track);
        }
        catch (Exception e)
        {
            storageService.deleteTrack(audioKey);

            if(audioPictureKey != null)
                storageService.deletePicture(audioPictureKey);

            throw e;
        }
    }

    @Transactional(rollbackOn = Exception.class)
    public Track saveTrack(MultipartFile file)
            throws Exception
    {
        return saveTrack(new Track(), file);
    }

    public boolean existsByTitleAndExactAuthors(String title, Set<Author> targetAuthors)
    {
        if (targetAuthors == null || targetAuthors.isEmpty())
        {
            return false;
        }

        return trackRepository.findTracksByTitleAndFirstAuthor(title, targetAuthors.iterator().next().getName()).stream()
                .anyMatch(track -> track.getAuthors().stream()
                        .map(Author::getName)
                        .collect(Collectors.toSet()).equals(
                                targetAuthors.stream()
                                .map(Author::getName)
                                .collect(Collectors.toSet())
                        ));
    }
}
