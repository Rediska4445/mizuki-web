package rf.mizuka.web.application.services.tracks;

import jakarta.transaction.Transactional;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rf.mizuka.web.application.brokers.BaseEvent;
import rf.mizuka.web.application.brokers.audio.events.TrackListenEvent;
import rf.mizuka.web.application.brokers.tracks.events.TrackLikeEvent;
import rf.mizuka.web.application.database.entities.media.authors.Author;
import rf.mizuka.web.application.database.entities.media.tracks.Track;
import rf.mizuka.web.application.database.entities.user.User;
import rf.mizuka.web.application.database.repository.media.authors.AuthorRepository;
import rf.mizuka.web.application.database.repository.media.tracks.TrackRepository;
import rf.mizuka.web.application.database.repository.user.UserRepository;
import rf.mizuka.web.application.forms.home.TrackForm;
import rf.mizuka.web.application.services.audio.AudioService;
import rf.mizuka.web.application.services.audio.metadata.IAudioMetadata;
import rf.mizuka.web.application.services.audio.metadata.exceptions.InvalidAudioDurationException;
import rf.mizuka.web.application.services.color.ColorService;
import rf.mizuka.web.application.services.image.ImageService;
import rf.mizuka.web.application.services.storage.StorageService;
import rf.mizuka.web.application.services.tracks.exceptions.TrackAlreadyExist;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TrackService
{
    /* Dependency */
    private final ImageService imageService;
    private final AudioService audioService;
    private final ColorService colorService;
    private final StorageService storageService;

    private final UserRepository userRepository;
    private final TrackRepository trackRepository;
    private final AuthorRepository authorRepository;

    /*
     * Constant define the type of audio transfer
     * True: Use "/stream/{id}" endpoint, which return byte stream audio
     * False: Use "/{trackId}" endpoint, which return url to audio-file into storage
     * */
    @Getter
    private final boolean isStreamingAudio;

    public TrackService(
            ImageService imageService, AudioService audioService,
            TrackRepository trackRepository, AuthorRepository authorRepository,
            ColorService colorService, StorageService storageService,
            UserRepository userRepository,
            @Value("${mizuki.audio.stream}") Boolean isStreamingAudio
    ) {
        this.imageService = imageService;
        this.audioService = audioService;
        this.trackRepository = trackRepository;
        this.authorRepository = authorRepository;
        this.colorService = colorService;
        this.storageService = storageService;
        this.userRepository = userRepository;
        this.isStreamingAudio = isStreamingAudio;
    }

    /* Straight access to inside dependency outta service */
    public AudioService audioService()
    {
        return audioService;
    }

    public TrackRepository trackRepository()
    {
        return trackRepository;
    }

    @org.springframework.transaction.annotation.Transactional(
            rollbackFor = Exception.class
    )
    public void saveAggregatedListens(List<? extends BaseEvent<String>> records)
    {
        Map<Long, Long> aggregatedMap = records.stream()
                .filter(TrackListenEvent.class::isInstance)
                .map(TrackListenEvent.class::cast)
                .filter(event -> event.getTrackId() != null)
                .collect(Collectors.groupingBy(
                        TrackListenEvent::getTrackId,
                        Collectors.counting()
                ));

        aggregatedMap.forEach(trackRepository::addListenCount);
    }

    @org.springframework.transaction.annotation.Transactional(
            rollbackFor = Exception.class
    )
    public void saveAggregatedLikes(List<TrackLikeEvent> records)
    {
        // Stages of transaction
        // 1. Update like count in track
        Map<Long, Long> trackChanges = records.stream()
                .collect(Collectors.groupingBy(
                        TrackLikeEvent::getTrackId,
                        Collectors.summingLong(e -> e.isLike() ? 1L : -1L)
                ));

        List<Long> sortedTrackIds = trackChanges.keySet().stream()
                .sorted()
                .toList();

        for (Long trackId : sortedTrackIds)
        {
            Long delta = trackChanges.get(trackId);
            if (delta != 0)
            {
                trackRepository.addTrackLikes(trackId, delta);
            }
        }

        // 2. Update liked track for user
        Map<Long, List<TrackLikeEvent>> userEventsMap = records.stream()
                .collect(Collectors.groupingBy(TrackLikeEvent::getUserId));

        List<Long> sortedUserIds = userEventsMap.keySet().stream()
                .sorted()
                .toList();

        for (Long userId : sortedUserIds)
        {
            List<TrackLikeEvent> userRecords = userEventsMap.get(userId);

            for (TrackLikeEvent event : userRecords)
            {
                if (event.isLike())
                {
                    userRepository.insertLike(userId, event.getTrackId());
                }
                else
                {
                    userRepository.deleteLike(userId, event.getTrackId());
                }
            }
        }
    }

    @org.springframework.transaction.annotation.Transactional(
            readOnly = true
    )
    public Page<Track> searchTracks(String query, int size)
    {
        Page<Track> trackPage = trackRepository.searchTracks(
                query, size <= 0 ? Pageable.unpaged() : Pageable.ofSize(size)
        );

        return trackPage;
    }

    @org.springframework.transaction.annotation.Transactional(
            readOnly = true
    )
    public Page<TrackForm> searchTracks(User user, String query, int size)
    {
        List<Long> likedTrackIds = userRepository.findLikedTrackIdsByUserId(user.getId());

        Page<TrackForm> tracks = searchTracks(query, size).map(
        e -> TrackForm.of(
                e,
                isStreamingAudio ? "/audio/stream/" + e.getId() : storageService.getTrackUrl(e.getFilePath()),
                storageService.getTrackPictureUrl(e.getPicturePath()),
                audioService.audioMetadataService().convertDurationToString(e.getDuration()),
                likedTrackIds.contains(e.getId())
        ));

        return tracks;
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
    public Color getColorFromAlbumArt(byte[] raw)
            throws IOException
    {
        return colorService.calculateMainColorFromImage(ImageIO.read(
                new ByteArrayInputStream(raw)
        ));
    }

    private String getHexColorFromImage(byte[] raw)
            throws IOException
    {
        return colorService.convertColorToHex(getColorFromAlbumArt(raw));
    }

    @Transactional(rollbackOn = Exception.class)
    public Track saveTrack(Track track, MultipartFile file)
            throws Exception
    {
        return saveTrack(track, null, file);
    }

    @Transactional(rollbackOn = Exception.class)
    public Track saveTrack(Track track, MultipartFile cover, MultipartFile file)
            throws Exception
    {
        if(file == null || file.isEmpty())
            throw new IllegalArgumentException("file is must be non-null and not be empty");
        if(track == null)
            throw new IllegalArgumentException("track is must be non-null");

        // Upload track to file storage (AWS S3)
        String audioKey
                = storageService.uploadTrack(file);
        // Picture key for upload
        String audioPictureKey
                = null;

        // Set file to DB from file storage (in key view, not really path or URL)
        track.setFilePath(audioKey);
        track.setName(file.getName());

        try
        {
            IAudioMetadata.Metadata meta = new IAudioMetadata.Metadata(
                    track.getTitle(),
                    track.getAuthors().stream().map(Author::getName).collect(Collectors.toSet()),
                    track.getDuration(),
                    cover == null ? imageService.getDefaultMusicImage() : cover.getBytes()
            );

            // Collection authors to Author type collection
            // Immediately call before check on exist.
            Set<Author> authors = meta.authors()
                    .stream()
                    .map(authorRepository::buildOrGet)
                    .collect(Collectors.toSet());

            // If this track already exist
            if (existsByTitleAndExactAuthors(meta.title(), authors))
                throw new TrackAlreadyExist("Track by these authors must be unique!");

            // Main ease metadata for track
            track.setTitle(meta.title());
            track.setDuration(meta.Duration());
            track.setAuthors(authors);

            if(track.getDuration().toSeconds() < 0)
                throw new InvalidAudioDurationException("audio duration less than 0");

            // Save picture to file storage
            track.setPicturePath(
                    audioPictureKey = storageService.uploadTrackPicture(meta.rawImage())
            );

            if(track.getColor() == null)
                track.setColor(getHexColorFromImage(meta.rawImage()));

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
            return false;

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
