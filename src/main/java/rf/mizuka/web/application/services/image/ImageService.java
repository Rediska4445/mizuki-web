package rf.mizuka.web.application.services.image;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Log4j2
public class ImageService
{
    @Value(
            "${storage.uploads.pictures.location:data/uploads/pictures}"
    )
    private String uploadDir;

    @Getter
    private final byte[] defaultMusicImage;

    public ImageService(
            @Value("classpath:static/img/default/default_track_image.png")
            Resource resource
    ) throws Exception {
        this.defaultMusicImage = resource.getContentAsByteArray();
    }

    public Path savePicture(byte[] rawImage, String trackName)
            throws IOException
    {
        if (rawImage == null || rawImage.length == 0)
        {
            throw new NullPointerException("rawImage is null or have length equal to 0");
        }

        try
        {
            Path directoryPath = Paths.get(uploadDir).toAbsolutePath().normalize();

            if (!Files.exists(directoryPath))
            {
                Files.createDirectories(directoryPath);
            }

            String safeTrackName = trackName.replaceAll("[^a-zA-Z0-9-_]", "_");
            String fileName = safeTrackName + "_" + UUID.randomUUID() + ".png";

            return Files.write(directoryPath.resolve(fileName), rawImage);
        }
        catch (IOException e)
        {
            throw e;
        }
    }

    public byte[] getPictureBytes(String picturePath)
    {
        if (picturePath == null || picturePath.isBlank())
        {
            return null;
        }

        try
        {
            Path path = Paths.get(picturePath);

            if (!Files.exists(path))
            {
                return null;
            }

            return Files.readAllBytes(path);
        }
        catch (IOException e)
        {
            return null;
        }
    }
}
