package cl.duoc.ep3.service;

import cl.duoc.ep3.dto.ProductoRequest;
import cl.duoc.ep3.model.Producto;
import cl.duoc.ep3.repository.ProductoRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public List<Producto> listarTodos() {
        return productoRepository.findAllByOrderByIdAsc();
    }

    public List<Producto> listarPorCategoria(String categoria) {
        return productoRepository.findByCategoryIgnoreCaseOrderByIdAsc(categoria);
    }

    public Producto buscarPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado."));
    }

    public Producto crear(ProductoRequest request) {
        Producto producto = request.toEntity();
        validar(producto);
        return productoRepository.save(producto);
    }

    public Producto actualizar(Long id, ProductoRequest request) {
        Producto productoActual = buscarPorId(id);
        Producto productoNuevo = request.toEntity();

        productoActual.setTitle(productoNuevo.getTitle());
        productoActual.setDescription(productoNuevo.getDescription());
        productoActual.setPrice(productoNuevo.getPrice());
        productoActual.setImage(productoNuevo.getImage());
        productoActual.setCategory(productoNuevo.getCategory());
        if (productoNuevo.getSourceId() != null && !productoNuevo.getSourceId().isBlank()) {
            productoActual.setSourceId(productoNuevo.getSourceId());
        }
        productoActual.setSourceRoute(productoNuevo.getSourceRoute());
        productoActual.setPlatform(productoNuevo.getPlatform());
        productoActual.setGenre(productoNuevo.getGenre());
        productoActual.setSubcategory(productoNuevo.getSubcategory());

        validar(productoActual);
        return productoRepository.save(productoActual);
    }

    public void eliminar(Long id) {
        if (!productoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado.");
        }
        productoRepository.deleteById(id);
    }

    private void validar(Producto producto) {
        // Estas reglas también se aplican si alguien llama la API sin pasar por React.
        if (producto.getTitle() == null || producto.getTitle().trim().length() < 2 || producto.getTitle().length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre debe tener entre 2 y 120 caracteres.");
        }
        if (producto.getDescription() == null || producto.getDescription().trim().isEmpty() || producto.getDescription().length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La descripción es obligatoria y no puede superar 2000 caracteres.");
        }
        if (producto.getCategory() == null || producto.getCategory().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría es obligatoria.");
        }
        if (producto.getImage() == null || producto.getImage().trim().isEmpty() || producto.getImage().length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La ruta de imagen es obligatoria.");
        }
        if (producto.getPrice() == null || producto.getPrice() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El precio debe ser mayor que cero.");
        }
    }
}
