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
import rf.mizuka.web.application.services.audio.AudioMetadataService;
import rf.mizuka.web.application.services.audio.AudioService;
import rf.mizuka.web.application.services.color.ColorService;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TrackService
{
    @Value("${storage.uploads.tracks.location}")
    private String storageLocation;

    private final AudioService audioService;
    private final TrackRepository trackRepository;
    private final AuthorRepository authorRepository;
    private final ColorService colorService;

    public TrackService(AudioService audioService, TrackRepository trackRepository, AuthorRepository authorRepository, ColorService colorService)
    {
        this.audioService = audioService;
        this.trackRepository = trackRepository;
        this.authorRepository = authorRepository;
        this.colorService = colorService;
    }

    public AudioService audioService()
    {
        return audioService;
    }

    public TrackRepository trackRepository() {
        return trackRepository;
    }

    public Page<Track> searchTracks(String query, int size)
    {
        return trackRepository.findByNameContaining(query, size <= 0 ? Pageable.unpaged() : Pageable.ofSize(size));
    }

    public String encodeBase64Picture(Track track)
    {
        if (track.getPicture() != null && track.getPicture().length > 0)
        {
            return java.util.Base64.getEncoder().encodeToString(track.getPicture());
        }

        return null;
    }

    /**
     * Detect color from track picture. Result must be in HEX format.
     * @param   track
     *          the track to extract picture and after detect color
     * @return color in HEX format
     * **/
    public String getColorFromAlbumArt(Track track)
            throws IOException
    {
        return "#" + Integer.toHexString(
                colorService.findMostContrastingColor(ImageIO.read(new ByteArrayInputStream(track.getPicture())))
                        .getRGB() & 0x00FFFFFF).toUpperCase();
    }

    @Transactional(rollbackOn = Exception.class)
    public Track saveTrack(Track track, MultipartFile file)
            throws Exception
    {
        String originalFilename = file.getOriginalFilename();

        String extension = "";
        if (originalFilename != null && originalFilename.contains("."))
        {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String technicalName = UUID.randomUUID() + extension;
        Path targetPath = Paths.get(storageLocation).resolve(technicalName);

        if (Files.notExists(targetPath.getParent()))
        {
            Files.createDirectories(targetPath.getParent());
        }

        Files.copy(file.getInputStream(), targetPath);

        if(track.getName() == null)
            track.setName(originalFilename);
        if(track.getFilePath() == null)
            track.setFilePath(targetPath.toString());

        try
        {
            AudioMetadataService.Metadata meta;

            // Check on exist metadata into track
            if(track.getTitle() != null
                    && track.getAuthors() != null
                    && track.getDuration() != null) {
                 meta = new AudioMetadataService.Metadata(
                        track.getTitle(), track.getAuthors().stream().map(Author::getName).collect(Collectors.toSet()), track.getDuration(), null
                );
            } else {
                meta = audioService.audioMetadataService().extractMetadata(track);
            }

            // Immediately call before check on exist.
            Set<Author> authors = meta.authors().stream()
                    .map(authorRepository::buildOrGet)
                    .collect(Collectors.toSet());

            if (existsByTitleAndExactAuthors(meta.title(), authors))
            {
                throw new TrackAlreadyExist("Track by these authors must be unique!");
            }

            track.setTitle(meta.title());
            track.setAuthors(authors);
            track.setDuration(meta.Duration());
            track.setPicture(meta.rawImage());
            track.setColor(getColorFromAlbumArt(track).toString());

            return trackRepository.save(track);
        }
        catch (Exception e)
        {
            Files.deleteIfExists(targetPath);

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
