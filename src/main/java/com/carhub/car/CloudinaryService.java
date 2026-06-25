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
