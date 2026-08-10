package rf.mizuka.web.application.services.tracks;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rf.mizuka.web.application.database.tracks.repository.TrackRepository;
import rf.mizuka.web.application.models.tracks.Track;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class TrackService {
    @Value("${storage.uploads.tracks.location}")
    private String storageLocation;

    private TrackRepository trackRepo;

    public TrackService(TrackRepository trackRepo) {
        this.trackRepo = trackRepo;
    }

    public Page<Track> findAllTracks(Pageable pageable) {
        return trackRepo.findAll(pageable);
    }

    public Page<Track> searchTracks(String query, Pageable pageable) {
        if (query == null || query.trim().isEmpty()) {
            return findAllTracks(pageable);
        }

        return trackRepo.findByNameContainingIgnoreCase(query.trim(), pageable);
    }

    @Transactional
    public Track saveTrack(MultipartFile file)
            throws IOException
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

        return trackRepo.save(track);
    }
}
