package rf.mizuka.web.application.services.tracks;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rf.mizuka.web.application.database.tracks.repository.TrackRepository;
import rf.mizuka.web.application.models.tracks.Track;
import rf.mizuka.web.application.services.audio.AudioMetadataService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class TrackService {
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

    public TrackService(AudioMetadataService audioMetadataService, TrackRepository trackRepo) {
        this.audioMetadataService = audioMetadataService;
        this.trackRepo = trackRepo;
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

    public Page<Track> searchTracks(String query, Pageable pageable) {
        if (query == null || query.trim().isEmpty()) {
            return findAllTracks(pageable);
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
    public Track saveTrack(MultipartFile file)
            throws Exception
    {
        String originalFilename = file.getOriginalFilename();

        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String technicalName = UUID.randomUUID() + extension;
        Path targetPath = Paths.get(storageLocation).resolve(technicalName);

        if (!Files.exists(targetPath.getParent())) {
            Files.createDirectories(targetPath.getParent());
        }

        Files.copy(file.getInputStream(), targetPath);

        Track track = new Track();
        track.setName(originalFilename);
        track.setFilePath(targetPath.toString());

        try {
            track = audioMetadataService.extractMetadata(track);

            return trackRepo.save(track);
        } catch (Exception e) {
            Files.delete(targetPath);

            throw e;
        }
    }
}
