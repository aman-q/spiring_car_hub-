package com.carhub.car;

import com.carhub.config.properties.CloudinaryProperties;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Uploads image buffers to Cloudinary and returns their secure URLs — the Java
 * equivalent of {@code config/cloudnaryStorage.js}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private final CloudinaryProperties properties;

    public List<String> uploadImages(MultipartFile[] files) {
        List<String> urls = new ArrayList<>();
        if (files == null) {
            return urls;
        }
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                urls.add(uploadOne(file));
            }
        }
        return urls;
    }

    /**
     * Best-effort deletion of previously-uploaded assets by their secure URL. Used to
     * avoid orphaning images when a car's images are replaced, or to roll back uploads
     * when the owning car fails to persist. Never throws — cleanup must not break the flow.
     */
    public void deleteByUrls(List<String> urls) {
        if (urls == null) {
            return;
        }
        for (String url : urls) {
            String publicId = publicIdFromUrl(url);
            if (publicId == null) {
                continue;
            }
            try {
                cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
                log.debug("Deleted Cloudinary asset {}", publicId);
            } catch (Exception e) {
                log.warn("Failed to delete Cloudinary asset {}: {}", publicId, e.getMessage());
            }
        }
    }

    /**
     * Extracts the Cloudinary {@code public_id} from a delivery URL, e.g.
     * {@code .../upload/v123/collection/images/name.jpg} -> {@code collection/images/name}.
     */
    String publicIdFromUrl(String url) {
        if (url == null || !url.contains("/upload/")) {
            return null;
        }
        String afterUpload = url.substring(url.indexOf("/upload/") + "/upload/".length());
        // Drop a leading version segment ("v1234567890/").
        afterUpload = afterUpload.replaceFirst("^v\\d+/", "");
        int dot = afterUpload.lastIndexOf('.');
        return dot > 0 ? afterUpload.substring(0, dot) : afterUpload;
    }

    private String uploadOne(MultipartFile file) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", properties.folder(),
                    "public_id", baseName(file.getOriginalFilename()),
                    "resource_type", "image"));
            return (String) result.get("secure_url");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to upload image: " + file.getOriginalFilename(), e);
        }
    }

    private String baseName(String filename) {
        if (filename == null || filename.isBlank()) {
            return "image";
        }
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}
