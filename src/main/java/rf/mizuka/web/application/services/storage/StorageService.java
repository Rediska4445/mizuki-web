package rf.mizuka.web.application.services.storage;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rf.mizuka.web.application.services.file.FileService;
import rf.mizuka.web.application.services.storage.exceptions.PresignedUrlException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class StorageService
{
    private final FileService fileService;
    private final MinioClient minioClient;
    private final String coversBucket;
    private final String tracksBucket;
    private final String publicUrl;

    public StorageService(FileService fileService, MinioClient minioClient,
                          @Value("${minio.buckets.covers}") String coversBucket,
                          @Value("${minio.buckets.tracks}") String tracksBucket,
                          @Value("${minio.public-url}") String publicUrl)
    {
        this.fileService = fileService;
        this.minioClient = minioClient;
        this.coversBucket = coversBucket;
        this.tracksBucket = tracksBucket;
        this.publicUrl = publicUrl;
    }

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

        String fileKey = UUID.randomUUID() + extension;

        try
        {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileKey)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            return fileKey;
        }
        catch (Exception e)
        {
            throw new IOException("Error for during upload file. Bucket: " + bucketName, e);
        }
    }

    public String getTrackPictureUrl(String coverFileName)
    {
        return getFile(coversBucket, coverFileName);
    }

    public String uploadTrack(MultipartFile file)
            throws IOException
    {
        return uploadFile(tracksBucket, file);
    }

    public String uploadTrackPicture(byte[] rawImage)
            throws IOException,
            ServerException, InsufficientDataException,
            ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException
    {
        if (rawImage == null || rawImage.length == 0)
        {
            return null;
        }

        String coverKey = UUID.randomUUID() + "-picture.jpg";

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(coversBucket)
                        .object(coverKey)
                        .stream(new ByteArrayInputStream(rawImage), rawImage.length, -1)
                        .contentType("image/jpeg")
                        .build()
        );

        return coverKey;
    }

    public String getPresignedUrl(
            String bucket, String fileName, TimeUnit timeUnit, int duration
    ) throws PresignedUrlException {
        try
        {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(fileName)
                            .expiry(duration, timeUnit)
                            .build()
            );
        }
        catch (Exception e)
        {
            throw new PresignedUrlException("I could not generate the URL");
        }
    }

    public String getTrackPicturePresignedUrl(String coverFileName)
            throws PresignedUrlException
    {
        return getPresignedUrl(coversBucket, coverFileName, TimeUnit.SECONDS, 5);
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

    private void deleteFile(String bucketName, String fileName)
    {
        if (fileName == null || fileName.isBlank())
        {
            return;
        }

        try
        {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );
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
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(tracksBucket)
                            .object(filePath)
                            .build()
            );
            long trackSize = stat.size();

            InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(tracksBucket)
                            .object(filePath)
                            .build()
            );

            return new InputStreamResource(inputStream)
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
}
