package rf.mizuka.web.application.storage.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig
{
    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.accessKey}")
    private String accessKey;

    @Value("${minio.secretKey}")
    private String secretKey;

    @Bean
    public MinioClient minioClient()
            throws Exception
    {
        MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();

        String coversBucket = "tracks-covers";
        String tracksBucket = "tracks";

        if (!client.bucketExists(BucketExistsArgs.builder().bucket(tracksBucket).build()))
        {
            client.makeBucket(MakeBucketArgs.builder().bucket(tracksBucket).build());
        }

        if (!client.bucketExists(BucketExistsArgs.builder().bucket(coversBucket).build()))
        {
            client.makeBucket(MakeBucketArgs.builder().bucket(coversBucket).build());

        String publicPolicyTemplate = """
        {
        "Version": "2012-10-17",
        "Statement": [
                {
                    "Sid": "PublicRead",
                    "Effect": "Allow",
                    "Principal": "*",
                    "Action": [
                        "s3:GetObject"
                    ],
                    "Resource": [
                        "arn:aws:s3:::%s/*"
                    ]
                }
            ]
        }
        """;

            client.setBucketPolicy(
                    SetBucketPolicyArgs.builder().bucket(coversBucket).config(publicReadPolicy).build()
            );
        }

        return client;
    }
}