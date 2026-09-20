package rf.mizuka.web.application.clients.storage;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface StorageClient
{
    /**
     * return objectName in successful
     * */
    String putObject(String bucket, String objectName, InputStream stream, long objectSize, String contentType);
    String getPresignedObjectUrl(String bucket, String objectName, int duration, TimeUnit timeUnit);
    void removeObject(String bucket, String objectName);
    long getObjectSize(String bucket, String objectName);
    InputStream getObject(String bucket, String objectName);
    Map<String, Object> statObject(String bucket, String objectName);
}
