package com.ada.proj.service;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ada.proj.config.AwsS3Properties;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3Service {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private final S3Client s3Client;
    private final AwsS3Properties props;

    public S3Service(S3Client s3Client, AwsS3Properties props) {
        this.s3Client = s3Client;
        this.props = props;
    }

    /**
     * 프로필 이미지를 S3에 업로드하고 퍼블릭 URL을 반환합니다.
     *
     * @param file 업로드할 이미지 파일
     * @param uuid 사용자 UUID (폴더 구분용)
     * @return 업로드된 파일의 S3 URL
     */
    public String uploadProfileImage(MultipartFile file, String uuid) {
        validateImage(file, props.getMaxProfileSizeMb());
        String key = "profiles/" + uuid + "/" + UUID.randomUUID() + getExtension(file);
        return upload(file, key);
    }

    /**
     * 배너 이미지를 S3에 업로드하고 퍼블릭 URL을 반환합니다.
     *
     * @param file 업로드할 이미지 파일
     * @param uuid 사용자 UUID (폴더 구분용)
     * @return 업로드된 파일의 S3 URL
     */
    public String uploadBanner(MultipartFile file, String uuid) {
        validateImage(file, props.getMaxBannerSizeMb());
        String key = "banners/" + uuid + "/" + UUID.randomUUID() + getExtension(file);
        return upload(file, key);
    }

    /**
     * S3 오브젝트 키(URL에서 추출)로 파일을 삭제합니다.
     */
    public void deleteByUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }
        String prefix = "https://" + props.getBucket() + ".s3." + props.getRegion() + ".amazonaws.com/";
        if (!fileUrl.startsWith(prefix)) {
            return;
        }
        String key = fileUrl.substring(prefix.length());
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .build());
    }

    // ── private helpers ──────────────────────────────────────────────
    private String upload(MultipartFile file, String key) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(props.getBucket())
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
            throw new IllegalStateException("파일 업로드 중 오류가 발생했습니다.", e);
        }

        return "https://" + props.getBucket() + ".s3." + props.getRegion() + ".amazonaws.com/" + key;
    }

    private void validateImage(MultipartFile file, int maxSizeMb) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어 있습니다.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다. (jpeg, png, gif, webp만 허용)");
        }
        long maxBytes = (long) maxSizeMb * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException(maxSizeMb + "MB까지만 업로드 가능합니다.");
        }
    }

    private String getExtension(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            return original.substring(original.lastIndexOf('.'));
        }
        return "";
    }
}
