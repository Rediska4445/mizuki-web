// Doesnt working
//
// package rf.mizuka;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
//import org.springframework.test.context.TestPropertySource;
//
//@TestPropertySource(locations = "classpath:settings-test.properties")
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
//public class ApplicationTest
//{
//    @Test
//    void mainMethodStartsApplicationWithoutException()
//    {
//        Application.main(new String[] {
//                "--spring.profiles.active=test",
//                "--minio.endpoint=http://localhost:9001",
//                "--minio.accessKey=mock-access-key",
//                "--minio.secretKey=mock-secret-key",
//                "--minio.bucketName=mock-bucket"
//        });
//    }
//}