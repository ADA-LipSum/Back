package com.ada.proj.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aws.s3")
public class AwsS3Properties {

    /**
     * 로컬 개발용. EC2 IAM Role 환경에서는 설정하지 않아도 됩니다.
     */
    private String accessKey;
    private String secretKey;

    private String region;
    private String bucket;
    private int maxProfileSizeMb = 5;
    private int maxBannerSizeMb = 10;
    private int maxCommunityImageSizeMb = 15;
    private int maxCommunityVideoSizeMb = 100;
}
