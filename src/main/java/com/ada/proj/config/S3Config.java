package com.ada.proj.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

    private final AwsS3Properties awsS3Properties;

    public S3Config(AwsS3Properties awsS3Properties) {
        this.awsS3Properties = awsS3Properties;
    }

    @Bean
    public S3Client s3Client() {
        AwsCredentialsProvider credentialsProvider;
        String accessKey = awsS3Properties.getAccessKey();
        String secretKey = awsS3Properties.getSecretKey();

        if (accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank()) {
            // 로컬 개발 환경: .env의 명시적 키 사용
            credentialsProvider = StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey));
        } else {
            // 운영(EC2) 환경: IAM Role(IMDS) / 환경변수 등 자동 탐색
            credentialsProvider = DefaultCredentialsProvider.create();
        }

        return S3Client.builder()
                .region(Region.of(awsS3Properties.getRegion()))
                .credentialsProvider(credentialsProvider)
                .build();
    }
}
