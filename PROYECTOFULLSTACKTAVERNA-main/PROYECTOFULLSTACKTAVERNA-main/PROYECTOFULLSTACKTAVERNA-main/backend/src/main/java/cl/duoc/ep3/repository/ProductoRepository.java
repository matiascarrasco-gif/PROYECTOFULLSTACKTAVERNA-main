package cl.duoc.ep3.repository;

import cl.duoc.ep3.model.Producto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findAllByOrderByIdAsc();

    List<Producto> findByCategoryIgnoreCaseOrderByIdAsc(String category);
}
