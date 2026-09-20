package rf.mizuka.web.application.services.storage;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rf.mizuka.web.application.clients.storage.StorageClient;
import rf.mizuka.web.application.services.file.FileService;
import rf.mizuka.web.application.services.storage.exceptions.PresignedUrlException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
public class StorageService
{
    private final FileService fileService;
    private final StorageClient storageClient;
    private final String coversBucket;
    private final String tracksBucket;
    private final String publicUrl;

    public StorageService(FileService fileService,
                          @Qualifier("storageClient") StorageClient storageClient,
                          @Value("${minio.buckets.covers}") String coversBucket,
                          @Value("${minio.buckets.tracks}") String tracksBucket,
                          @Value("${minio.public-url}") String publicUrl)
    {
        this.fileService = fileService;
        this.storageClient = storageClient;
        this.coversBucket = coversBucket;
        this.tracksBucket = tracksBucket;
        this.publicUrl = publicUrl;
    }

    /* Return formatted file url */
    public String getFile(String bucket, String fileName)
    {
        return String.format("%s/%s/%s", publicUrl, bucket, fileName);
    }

    private String uploadFile(String bucketName, MultipartFile file)
            throws IOException
    {
        if (file == null || file.isEmpty())
        {
            return null;
        }

        String extension = fileService.extractExtension(file.getOriginalFilename());
        String fileKey = keyFromString(
                Objects.requireNonNull(file.getOriginalFilename()) + Objects.requireNonNull(extension)
        ) + extension;

        try
        {
            storageClient.putObject(bucketName, fileKey, file.getInputStream(), file.getSize(), file.getContentType());

            return fileKey;
        }
        catch (Exception e)
        {
            throw new IOException("Error for during upload file. Bucket: " + bucketName, e);
        }
    }

    public String uploadImage(MultipartFile file)
            throws IOException
    {
        if (file == null || file.getBytes().length == 0)
        {
            return null;
        }

        String extension = file.getContentType() == null ? "image/jpeg" : file.getContentType();
        String coverKey = keyFromString(
                Objects.requireNonNull(file.getOriginalFilename())
        ) + extension;

        storageClient.putObject(coversBucket, coverKey, file.getInputStream(), file.getSize(), extension);

        return coverKey;
    }

    private void deleteFile(String bucketName, String fileName)
    {
        if (fileName == null || fileName.isBlank())
        {
            return;
        }

        try
        {
            storageClient.removeObject(bucketName, fileName);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Could not remove file from MinIO. Bucket: " + bucketName, e);
        }
    }

    public Resource getTrackResource(String filePath)
    {
        try
        {
            final long trackSize = (long) storageClient.statObject(tracksBucket, filePath).get("size");

            return new InputStreamResource(storageClient.getObject(tracksBucket, filePath))
            {
                @Override
                public long contentLength()
                {
                    return trackSize;
                }
            };
        }
        catch (Exception e)
        {
            throw new RuntimeException("Could not get resource from MinIO: " + filePath, e);
        }
    }

    // Overloads
    public String getTrackPictureUrl(String coverFileName)
    {
        return getFile(coversBucket, coverFileName);
    }

    public String uploadTrack(MultipartFile file)
            throws IOException
    {
        return uploadFile(tracksBucket, file);
    }

    public String getPresignedUrl(
            String bucket, String fileName, TimeUnit timeUnit, int duration
    ) throws PresignedUrlException {
        try
        {
            return storageClient.getPresignedObjectUrl(bucket, fileName, duration, timeUnit);
        }
        catch (Exception e)
        {
            throw new PresignedUrlException("I could not generate the URL");
        }
    }

    public String getTrackPresignedUrl(String trackFileName)
            throws PresignedUrlException
    {
        return getPresignedUrl(tracksBucket, trackFileName, TimeUnit.SECONDS, 5);
    }

    public String getTrackUrl(String trackFileName)
    {
        return getFile(tracksBucket, trackFileName);
    }

    public void deleteTrack(String trackFileName)
    {
        deleteFile(tracksBucket, trackFileName);
    }

    public void deletePicture(String coverFileName)
    {
        deleteFile(coversBucket, coverFileName);
    }

    /* Build pseudo-unique UUID key from string */
    private String keyFromString(String str)
    {
        return Base64.getEncoder().encodeToString(
                Objects.requireNonNull(str).getBytes(StandardCharsets.UTF_8)
        );
    }
}
