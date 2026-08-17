package rf.mizuka.web.application.services.image;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import rf.mizuka.web.application.services.storage.StorageService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Service
@Log4j2
public class ImageService
{
    @Getter
    private final byte[] defaultMusicImage;

    public ImageService(
            @Value("classpath:static/img/default/default_track_image.png")
            Resource resource
    ) throws Exception {
        this.defaultMusicImage = resource.getContentAsByteArray();
    }
}
