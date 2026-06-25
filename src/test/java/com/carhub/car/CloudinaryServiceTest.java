package com.carhub.car;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CloudinaryServiceTest {

    private final CloudinaryService service = new CloudinaryService(null, null);

    @Test
    void extractsPublicIdFromDeliveryUrl() {
        String url = "https://res.cloudinary.com/divgo2tut/image/upload/v1731863293/collection/images/nick-unsplash.jpg";
        assertEquals("collection/images/nick-unsplash", service.publicIdFromUrl(url));
    }

    @Test
    void handlesUrlWithoutVersionOrExtension() {
        assertEquals("folder/name", service.publicIdFromUrl("https://res.cloudinary.com/x/image/upload/folder/name"));
    }

    @Test
    void returnsNullForNonCloudinaryOrNull() {
        assertNull(service.publicIdFromUrl("https://example.com/whatever.jpg"));
        assertNull(service.publicIdFromUrl(null));
    }
}
