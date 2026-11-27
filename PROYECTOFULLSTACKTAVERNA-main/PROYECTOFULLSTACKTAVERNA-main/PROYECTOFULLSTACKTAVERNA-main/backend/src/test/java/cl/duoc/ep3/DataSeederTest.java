package cl.duoc.ep3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.duoc.ep3.model.Producto;
import cl.duoc.ep3.repository.ProductoRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;

class DataSeederTest {

    @Test
    void addsOriginalCatalogWithoutDuplicatingOrReplacingCustomizedProducts() {
        ProductoRepository repository = org.mockito.Mockito.mock(ProductoRepository.class);
        List<Producto> storedProducts = new ArrayList<>();
        Producto customizedProduct = new Producto(
                "Mouse Gamer Pro",
                "Producto personalizado por la tienda.",
                59000L,
                "/images/accesorios/mouse.webp",
                "Accesorios",
                "",
                "PC",
                "FPS",
                "Ergonómico"
        );
        Producto unchangedLegacySample = new Producto(
                "Teclado Mecánico RGB",
                "Teclado con switches silenciosos y retroiluminación RGB.",
                89000L,
                "/images/accesorios/teclado.webp",
                "Accesorios",
                "",
                "PC",
                "Escritura",
                "Mecánico"
        );
        storedProducts.add(customizedProduct);
        storedProducts.add(unchangedLegacySample);

        when(repository.findAll()).thenAnswer(invocation -> new ArrayList<>(storedProducts));
        when(repository.saveAll(any())).thenAnswer(invocation -> {
            Iterable<Producto> products = invocation.getArgument(0);
            products.forEach(storedProducts::add);
            return storedProducts;
        });
        doAnswer(invocation -> {
            Iterable<Producto> products = invocation.getArgument(0);
            products.forEach(storedProducts::remove);
            return null;
        }).when(repository).deleteAll(any());

        DataSeeder seeder = new DataSeeder(repository);
        seeder.run((ApplicationArguments) null);

        assertThat(storedProducts).hasSize(122);
        assertThat(storedProducts).contains(customizedProduct);
        assertThat(storedProducts).doesNotContain(unchangedLegacySample);
        assertThat(storedProducts).anyMatch(product ->
                product.getTitle().equals("Tarjeta de Video RTX 5090")
                        && product.getImage().equals("/images/accesorios/Rtx5090.webp"));
        assertThat(storedProducts).allMatch(product -> product.getImage().startsWith("/images/"));

        seeder.run((ApplicationArguments) null);

        assertThat(storedProducts).hasSize(122);
        verify(repository, times(1)).saveAll(any());
    }
}
