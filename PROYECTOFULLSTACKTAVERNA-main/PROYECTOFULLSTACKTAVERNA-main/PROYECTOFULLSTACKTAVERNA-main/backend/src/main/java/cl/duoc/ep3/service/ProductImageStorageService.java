package cl.duoc.ep3.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductImageStorageService {

    public static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;
    private static final long MAX_IMAGE_PIXELS = 8_000_000;
    private static final String IMAGE_ROUTE = "/uploads/product-images/";

    private final Path directory;

    public ProductImageStorageService(@Value("${app.product-images.directory}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    public StoredProductImage store(MultipartFile file) {
        // Se guarda fuera del código; MySQL conserva solo la ruta que devuelve el servicio.
        if (file == null || file.isEmpty()) {
            throw badRequest("Selecciona una imagen.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(413),
                    "La imagen no puede superar los 5 MB.");
        }

        ImageFormat format = detectImageFormat(file);
        String filename = UUID.randomUUID() + "." + format.extension();
        Path target = directory.resolve(filename).normalize();
        if (!target.startsWith(directory)) {
            throw new IllegalStateException("La ruta de almacenamiento de imágenes no es válida.");
        }

        try {
            Files.createDirectories(directory);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target);
            }
        } catch (IOException error) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo guardar la imagen.", error);
        }

        return new StoredProductImage(filename, IMAGE_ROUTE + filename, format.mediaType());
    }

    public StoredProductImage load(String filename) {
        ImageFormat format = ImageFormat.fromFilename(filename);
        if (format == null) {
            throw notFound();
        }

        Path image = directory.resolve(filename).normalize();
        if (!image.startsWith(directory) || !Files.isRegularFile(image)) {
            throw notFound();
        }
        try {
            return new StoredProductImage(filename, IMAGE_ROUTE + filename, format.mediaType(),
                    new FileSystemResource(image), Files.size(image));
        } catch (IOException error) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo leer la imagen.", error);
        }
    }

    private ImageFormat detectImageFormat(MultipartFile file) {
        // Se revisa el contenido real, no el nombre ni el MIME que envía el navegador.
        try (InputStream stream = file.getInputStream();
                ImageInputStream imageInput = ImageIO.createImageInputStream(stream)) {
            if (imageInput == null) {
                throw badRequest("El archivo no contiene una imagen PNG, JPG o GIF válida.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw badRequest("El archivo no contiene una imagen PNG, JPG o GIF válida.");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, false, true);
                ImageFormat format = ImageFormat.fromFormatName(reader.getFormatName());
                if (format == null) {
                    throw badRequest("Solo se permiten imágenes PNG, JPG o GIF.");
                }

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_IMAGE_PIXELS) {
                    throw badRequest("La imagen supera las dimensiones permitidas.");
                }
                if (reader.read(0) == null) {
                    throw badRequest("El archivo de imagen está dañado.");
                }
                return format;
            } finally {
                reader.dispose();
            }
        } catch (IOException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El archivo no contiene una imagen PNG, JPG o GIF válida.", error);
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Imagen no encontrada.");
    }

    public record StoredProductImage(
            String filename,
            String image,
            MediaType mediaType,
            Resource resource,
            long size
    ) {
        private StoredProductImage(String filename, String image, MediaType mediaType) {
            this(filename, image, mediaType, null, 0);
        }
    }

    private enum ImageFormat {
        PNG("png", MediaType.IMAGE_PNG),
        JPEG("jpg", MediaType.IMAGE_JPEG),
        GIF("gif", MediaType.IMAGE_GIF);

        private final String extension;
        private final MediaType mediaType;

        ImageFormat(String extension, MediaType mediaType) {
            this.extension = extension;
            this.mediaType = mediaType;
        }

        private String extension() {
            return extension;
        }

        private MediaType mediaType() {
            return mediaType;
        }

        private static ImageFormat fromFormatName(String format) {
            return switch (format.toLowerCase(Locale.ROOT)) {
                case "png" -> PNG;
                case "jpeg", "jpg" -> JPEG;
                case "gif" -> GIF;
                default -> null;
            };
        }

        private static ImageFormat fromFilename(String filename) {
            if (filename == null || !filename.matches(
                    "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(png|jpg|gif)")) {
                return null;
            }
            return switch (filename.substring(filename.lastIndexOf('.') + 1)) {
                case "png" -> PNG;
                case "jpg" -> JPEG;
                case "gif" -> GIF;
                default -> null;
            };
        }
    }
}
