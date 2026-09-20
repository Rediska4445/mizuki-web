package rf.mizuka.web.application.clients.storage.impl;

import io.minio.*;
import io.minio.http.Method;
import rf.mizuka.web.application.clients.storage.StorageClient;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class MiniOStorageClient
        implements StorageClient
{
    private final MinioClient minioClient;

    public MiniOStorageClient(MinioClient minioClient)
    {
        this.minioClient = minioClient;
    }

    @Override
    public String putObject(String bucket, String objectName, InputStream stream, long objectSize, String contentType)
    {
        try
        {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(stream, objectSize, -1)
                            .contentType(contentType)
                            .build()
            );
        }
        catch (Exception e)
        {
            throw new RuntimeException("MiniO error: ", e);
        }

        return objectName;
    }

    @Override
    public String getPresignedObjectUrl(String bucket, String objectName, int duration, TimeUnit timeUnit)
    {
        try
        {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectName)
                            .expiry(duration, timeUnit)
                            .build()
            );
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to generate presigned URL from MinIO storage", e);
        }
    }

    @Override
    public void removeObject(String bucket, String objectName)
    {
        try
        {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to remove object from MinIO storage", e);
        }
    }

    @Override
    public long getObjectSize(String bucket, String objectName)
    {
        try
        {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
            return stat.size();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to retrieve object size from MinIO storage", e);
        }
    }

    @Override
    public InputStream getObject(String bucket, String objectName)
    {
        try
        {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to fetch object stream from MinIO storage", e);
        }
    }

    @Override
    public Map<String, Object> statObject(String bucket, String objectName)
    {
        try
        {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
            return Map.of(
                    "size", stat.size(),
                    "contentType", stat.contentType()
            );
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to fetch object metadata from MinIO storage", e);
        }
    }
}
