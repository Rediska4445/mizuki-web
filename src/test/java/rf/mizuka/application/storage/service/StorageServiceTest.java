package rf.mizuka.application.storage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.multipart.MultipartFile;
import rf.mizuka.web.application.clients.storage.StorageClient;
import rf.mizuka.web.application.clients.storage.impl.MiniOStorageClient;
import rf.mizuka.web.application.services.file.FileService;
import rf.mizuka.web.application.services.storage.StorageService;
import rf.mizuka.web.application.services.storage.exceptions.PresignedUrlException;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
public class StorageServiceTest
{
    // Main actor for testing
    private StorageService storageService;

    private StorageClient storageClient;

    @MockitoBean
    private FileService fileService;

    private final String cover = "cover", tracks = "tracks", url = "url";

    @BeforeEach
    void setUp()
    {
        fileService = Mockito.mock(FileService.class);
        storageClient = Mockito.mock(MiniOStorageClient.class);

        storageService = new StorageService(fileService, storageClient, cover, tracks, url);
    }

    /* 1. Service should create without any errors */
    @Test
    @DisplayName("Create")
    void storageServiceShouldBeCreated()
    {
        assertNotNull(storageService);
    }

    /* 1. Service method for get file url should return formatted url (real web url) without any errors */
    @Test
    @DisplayName("Get url")
    void storageServiceGetFileShouldReturnFormattedFileUrl()
    {
        String realUrl = String.format("%s/%s/%s", url, cover, "file");

        String res = storageService.getFile(cover, "file");

        // Possible not use mockito
        assertNotNull(res);
        assertEquals(res, realUrl);
    }

    /* 1. Service method for upload file url should uploading file without any errors */
    @Test
    @DisplayName("Upload file")
    void storageServiceShouldUploadTrackFileWithoutErrors()
            throws IOException
    {
        Mockito.when(fileService.extractExtension(any()))
                .thenAnswer(e -> ".ext");

        String originalFilename = "my_awesome_track.mp3";
        String contentType = "audio/mpeg";
        byte[] content = "random binary data simulation".getBytes();

        MultipartFile file = new MockMultipartFile(
                "file",
                originalFilename,
                contentType,
                content
        );

        String res = storageService.uploadTrack(file);

        Mockito.verify(storageClient, Mockito.times(1))
                .putObject(
                        anyString(),
                        anyString(),
                        any(InputStream.class),
                        anyLong(),
                        anyString()
                );

        assertNotNull(res);
    }

    /* 1. Service method for upload file cover should uploading file without any errors */
    @Test
    @DisplayName("Upload cover")
    void storageServiceShouldUploadCoverFileWithoutErrors()
            throws IOException
    {
        byte[] content = "random binary data simulation".getBytes();

        String res = storageService.uploadTrackPicture(content);

        Mockito.verify(storageClient, Mockito.times(1))
                .putObject(
                        anyString(),
                        anyString(),
                        any(InputStream.class),
                        anyLong(),
                        eq("image/jpeg")
                );

        assertNotNull(res);
    }

    @Test
    @DisplayName("Get presigned url")
    void storageServiceShouldGivePresignedUrlWithoutErrors()
            throws IOException, PresignedUrlException
    {
        Mockito.when(storageClient.getPresignedObjectUrl(anyString(), anyString(), anyInt(), any(TimeUnit.class)))
                .thenAnswer(e -> "presignedUrl");

        String res = storageService.getPresignedUrl(tracks, url, TimeUnit.MINUTES, 10);
        Mockito.verify(storageClient, Mockito.times(1))
                        .getPresignedObjectUrl(
                                eq(tracks),
                                eq(url),
                                eq(10),
                                eq(TimeUnit.MINUTES)
                        );

        assertNotNull(res);
    }

    @Test
    @DisplayName("Remove object")
    void storageServiceShouldRemoveObjectWithoutErrors()
            throws IOException, PresignedUrlException
    {
        storageService.deleteTrack(tracks);

        Mockito.verify(storageClient, Mockito.times(1))
                .removeObject(anyString(), eq(tracks));
    }
}
