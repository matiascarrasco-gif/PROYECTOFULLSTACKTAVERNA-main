package cl.duoc.ep3.controller;

import cl.duoc.ep3.dto.ProductoRequest;
import cl.duoc.ep3.model.Producto;
import cl.duoc.ep3.service.ProductoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping("/productos")
    public List<Producto> listarTodos() {
        return productoService.listarTodos();
    }

    @GetMapping("/productos/categoria/{categoria}")
    public List<Producto> listarPorCategoria(@PathVariable String categoria) {
        return productoService.listarPorCategoria(categoria);
    }

    @GetMapping("/productos/{id}")
    public Producto buscarPorId(@PathVariable Long id) {
        return productoService.buscarPorId(id);
    }

    @PostMapping("/productos")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Producto> crear(@Valid @RequestBody ProductoRequest request) {
        Producto creado = productoService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/productos/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public Producto actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest request) {
        return productoService.actualizar(id, request);
    }

    @DeleteMapping("/productos/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
