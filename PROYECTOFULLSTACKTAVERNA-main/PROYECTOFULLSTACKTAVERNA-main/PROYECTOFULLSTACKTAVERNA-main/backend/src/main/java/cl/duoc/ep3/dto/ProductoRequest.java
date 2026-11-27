package cl.duoc.ep3.dto;

import cl.duoc.ep3.model.Producto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProductoRequest(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(min = 2, max = 120, message = "El nombre debe tener entre 2 y 120 caracteres.")
        String title,

        @NotBlank(message = "La descripción es obligatoria.")
        @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres.")
        String description,

        @NotNull(message = "El precio es obligatorio.")
        @Positive(message = "El precio debe ser mayor que cero.")
        Long price,

        @NotBlank(message = "La imagen es obligatoria.")
        @Size(max = 500, message = "La ruta de la imagen es demasiado larga.")
        String image,

        @NotBlank(message = "La categoría es obligatoria.")
        String category,

        String sourceId,
        String sourceRoute,
        String platform,
        String genre,
        String subcategory
) {
    public Producto toEntity() {
        Producto producto = new Producto();
        producto.setTitle(title.trim());
        producto.setDescription(description.trim());
        producto.setPrice(price);
        producto.setImage(image.trim());
        producto.setCategory(category.trim());
        producto.setSourceId(sourceId == null ? "" : sourceId.trim());
        producto.setSourceRoute(sourceRoute == null ? "" : sourceRoute.trim());
        producto.setPlatform(platform == null ? "" : platform.trim());
        producto.setGenre(genre == null ? "" : genre.trim());
        producto.setSubcategory(subcategory == null ? "" : subcategory.trim());
        return producto;
    }
}
