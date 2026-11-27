package cl.duoc.ep3.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductImageStorageServiceTest {

    @TempDir
    Path tempDirectory;

    @Test
    void storesImagesWithGeneratedNamesAndLoadsThemFromTheConfiguredDirectory() throws IOException {
        ProductImageStorageService storage = new ProductImageStorageService(tempDirectory.toString());
        byte[] png = createPng();
        MockMultipartFile upload = new MockMultipartFile(
                "file", "user-supplied-name.jpg", "image/jpeg", png);

        ProductImageStorageService.StoredProductImage stored = storage.store(upload);
        ProductImageStorageService.StoredProductImage loaded = storage.load(stored.filename());

        assertEquals("/uploads/product-images/" + stored.filename(), stored.image());
        assertEquals("image/png", stored.mediaType().toString());
        assertTrue(Files.isRegularFile(tempDirectory.resolve(stored.filename())));
        try (var input = loaded.resource().getInputStream()) {
            assertArrayEquals(png, input.readAllBytes());
        }
    }

    @Test
    void rejectsNonImageFilesAndPathsOutsideTheGeneratedFilenamePattern() {
        ProductImageStorageService storage = new ProductImageStorageService(tempDirectory.toString());
        MockMultipartFile fakeImage = new MockMultipartFile(
                "file", "fake.png", "image/png", "not an image".getBytes());

        assertEquals(400, assertThrows(ResponseStatusException.class, () -> storage.store(fakeImage))
                .getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> storage.load("../secret.png"))
                .getStatusCode().value());
    }

    @Test
    void rejectsImagesLargerThanTheUploadLimit() {
        ProductImageStorageService storage = new ProductImageStorageService(tempDirectory.toString());
        MockMultipartFile largeFile = new MockMultipartFile(
                "file", "large.png", "image/png",
                new byte[(int) ProductImageStorageService.MAX_FILE_SIZE_BYTES + 1]);

        assertEquals(413, assertThrows(ResponseStatusException.class, () -> storage.store(largeFile))
                .getStatusCode().value());
    }

    private byte[] createPng() throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
