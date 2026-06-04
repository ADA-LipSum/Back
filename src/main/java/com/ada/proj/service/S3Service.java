package com.ada.proj.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ada.proj.config.AwsS3Properties;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3Service {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private static final Set<String> ALLOWED_COMMUNITY_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml"
    );

    private static final Set<String> ALLOWED_POST_MEDIA_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml", "video/mp4"
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
     * 게시글 첨부 미디어(이미지·mp4)를 S3에 업로드하고 퍼블릭 URL을 반환합니다.
     */
    public String uploadPostMedia(MultipartFile file, String userUuid) {
        boolean isVideo = "video/mp4".equals(file.getContentType());
        int maxSizeMb = isVideo ? props.getMaxCommunityVideoSizeMb() : props.getMaxCommunityImageSizeMb();
        validateMedia(file, ALLOWED_POST_MEDIA_TYPES, maxSizeMb,
                "허용되지 않는 파일 형식입니다. (jpeg, png, gif, webp, svg, mp4만 허용)");
        String key = "community/posts/" + userUuid + "/" + UUID.randomUUID() + getExtension(file);
        return upload(file, key);
    }

    /**
     * 공지사항 첨부파일(이미지, 영상, PDF, HWP, 문서 등)을 S3에 업로드하고 퍼블릭 URL을 반환합니다.
     */
    public String uploadNoticeAttachment(MultipartFile file, String userUuid) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 없습니다.");
        }
        long maxBytes = 50L * 1024 * 1024; // 50MB
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("파일 크기는 50MB를 초과할 수 없습니다.");
        }
        String key = "notices/attachments/" + userUuid + "/" + UUID.randomUUID() + getExtension(file);
        return upload(file, key);
    }

    /**
     * 댓글 첨부 이미지(mp4 제외)를 S3에 업로드하고 퍼블릭 URL을 반환합니다.
     */
    public String uploadCommentImage(MultipartFile file, String userUuid) {
        validateMedia(file, ALLOWED_COMMUNITY_IMAGE_TYPES,
                props.getMaxCommunityImageSizeMb(),
                "허용되지 않는 파일 형식입니다. (jpeg, png, gif, webp, svg만 허용)");
        String key = "community/comments/" + userUuid + "/" + UUID.randomUUID() + getExtension(file);
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

    // ── S3 브라우저 ──────────────────────────────────────────────────

    public record S3Item(String key, String name, boolean folder, long size, String url) {}

    public List<S3Item> listObjects(String prefix) {
        if (!prefix.isEmpty() && !prefix.endsWith("/")) prefix += "/";

        ListObjectsV2Request req = ListObjectsV2Request.builder()
                .bucket(props.getBucket())
                .prefix(prefix)
                .delimiter("/")
                .build();

        ListObjectsV2Response res = s3Client.listObjectsV2(req);
        List<S3Item> items = new ArrayList<>();

        final String finalPrefix = prefix;
        for (var cp : res.commonPrefixes()) {
            String key = cp.prefix();
            String name = key.substring(finalPrefix.length());
            if (name.endsWith("/")) name = name.substring(0, name.length() - 1);
            items.add(new S3Item(key, name, true, 0, null));
        }
        for (var obj : res.contents()) {
            if (obj.key().equals(finalPrefix)) continue;
            String name = obj.key().substring(finalPrefix.length());
            if (name.isEmpty()) continue;
            items.add(new S3Item(obj.key(), name, false, obj.size(), buildUrl(obj.key())));
        }
        return items;
    }

    public record S3ObjectData(byte[] bytes, String contentType) {}

    public S3ObjectData getObject(String key) {
        ResponseBytes<GetObjectResponse> resp = s3Client.getObjectAsBytes(
                GetObjectRequest.builder().bucket(props.getBucket()).key(key).build());
        String ct = resp.response().contentType();
        return new S3ObjectData(resp.asByteArray(), ct != null ? ct : "application/octet-stream");
    }

    public String overwriteByKey(MultipartFile file, String key) {
        validateImage(file, props.getMaxBannerSizeMb());
        return upload(file, key);
    }

    public void deleteByKey(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .build());
    }

    public void createFolder(String prefix, String name) {
        if (!prefix.isEmpty() && !prefix.endsWith("/")) prefix += "/";
        String key = prefix + name.replaceAll("[^a-zA-Z0-9._\\-가-힣]", "_") + "/";
        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .contentType("application/x-directory")
                .contentLength(0L)
                .build();
        s3Client.putObject(req, RequestBody.empty());
    }

    public String uploadStickerImage(MultipartFile file, String userUuid) {
        validateImage(file, props.getMaxProfileSizeMb());
        String key = "stickers/" + userUuid + "/" + UUID.randomUUID() + getExtension(file);
        return upload(file, key);
    }

    public String uploadToPrefix(MultipartFile file, String prefix) {
        if (!prefix.isEmpty() && !prefix.endsWith("/")) prefix += "/";
        validateImage(file, props.getMaxBannerSizeMb());
        String key = prefix + UUID.randomUUID() + getExtension(file);
        return upload(file, key);
    }

    public String uploadToPrefixAdmin(MultipartFile file, String prefix) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("파일이 비어 있습니다.");
        if (!prefix.isEmpty() && !prefix.endsWith("/")) prefix += "/";
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String key = prefix + original;
        return upload(file, key);
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

        return buildUrl(key);
    }

    private String buildUrl(String key) {
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

    private void validateMedia(MultipartFile file, Set<String> allowedTypes, int maxSizeMb, String typeErrorMsg) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어 있습니다.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new IllegalArgumentException(typeErrorMsg);
        }
        long maxBytes = (long) maxSizeMb * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException(maxSizeMb + "MB까지만 업로드 가능합니다.");
        }
    }

    private String getContentType(MultipartFile file) {
        return file.getContentType() != null ? file.getContentType() : "";
    }

    private String getExtension(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            return original.substring(original.lastIndexOf('.'));
        }
        return "";
    }
}
