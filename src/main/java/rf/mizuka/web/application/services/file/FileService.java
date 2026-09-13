package rf.mizuka.web.application.services.file;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rf.mizuka.web.application.services.FunctionWithException;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class FileService
{
    public String extractExtension(String originalFilename)
        throws NullPointerException
    {
        if (originalFilename != null && originalFilename.contains("."))
        {
            return originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        else
        {
            throw new NullPointerException(originalFilename + " have not extension, or be null");
        }
    }

    public <T> T createTempFile(
            String prefix, String suffix,
            MultipartFile multipartFile,
            FunctionWithException<Path, T, Exception> duraIntoLifeFile
    ) throws Exception {
        Path tempFile = Files.createTempFile(prefix, suffix);
        multipartFile.transferTo(tempFile.toFile());

        try
        {
            return duraIntoLifeFile.apply(tempFile);
        }
        finally
        {
            Files.deleteIfExists(tempFile);
        }
    }
}
