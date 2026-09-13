package rf.mizuka.config;

import io.minio.MinioClient;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class TestMinioConfig
{
    @Bean
    @Primary
    public MinioClient minioClient()
    {
        return Mockito.mock(MinioClient.class);
    }
}
