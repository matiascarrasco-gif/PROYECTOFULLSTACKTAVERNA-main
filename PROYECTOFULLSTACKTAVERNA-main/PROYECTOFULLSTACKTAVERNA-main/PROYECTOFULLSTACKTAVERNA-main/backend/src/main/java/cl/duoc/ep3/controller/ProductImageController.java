package cl.duoc.ep3.controller;

import cl.duoc.ep3.dto.ProductImageResponse;
import cl.duoc.ep3.service.ProductImageStorageService;
import cl.duoc.ep3.service.ProductImageStorageService.StoredProductImage;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ProductImageController {

    private final ProductImageStorageService storage;

    public ProductImageController(ProductImageStorageService storage) {
        this.storage = storage;
    }

    @PostMapping(value = "/uploads/product-images", consumes = "multipart/form-data")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ProductImageResponse> upload(@RequestPart("file") MultipartFile file) {
        StoredProductImage stored = storage.store(file);
        return ResponseEntity.ok(new ProductImageResponse(stored.image()));
    }

    @GetMapping("/uploads/product-images/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        StoredProductImage image = storage.load(filename);
        return ResponseEntity.ok()
                .contentType(image.mediaType())
                .contentLength(image.size())
                .cacheControl(CacheControl.noCache())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + image.filename() + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .body(image.resource());
    }
}
