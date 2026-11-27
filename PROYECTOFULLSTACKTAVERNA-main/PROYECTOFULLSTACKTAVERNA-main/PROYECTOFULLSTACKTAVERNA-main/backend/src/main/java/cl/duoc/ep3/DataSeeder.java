package cl.duoc.ep3;

import cl.duoc.ep3.model.Producto;
import cl.duoc.ep3.repository.ProductoRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements ApplicationRunner {

    private static final TypeReference<List<CatalogProduct>> CATALOG_TYPE = new TypeReference<>() {
    };

    private static final List<LegacyProduct> LEGACY_SAMPLE_PRODUCTS = List.of(
            new LegacyProduct("Teclado Mecánico RGB", "Teclado con switches silenciosos y retroiluminación RGB.",
                    89000L, "/images/accesorios/teclado.webp", "Accesorios"),
            new LegacyProduct("Mouse Gamer Pro", "Mouse ergonómico para gaming con múltiples botones y sensor preciso.",
                    59000L, "/images/accesorios/mouse.webp", "Accesorios"),
            new LegacyProduct("PlayStation 5",
                    "Consola de nueva generación con amplio catálogo de juegos y rendimiento 4K.",
                    649990L, "/images/consolas/ps5.webp", "Consolas"),
            new LegacyProduct("Xbox Series X", "Consola de alta potencia y soporte para juegos en 4K y ray tracing.",
                    599990L, "/images/consolas/xbox.webp", "Consolas"),
            new LegacyProduct("Cyberpunk 2077", "Juego de mundo abierto con historia intensa y un gran componente RPG.",
                    49990L, "/images/juegos/cyberpunk.webp", "Juegos"),
            new LegacyProduct("God of War Ragnarök",
                    "Aventura épica con combates potentes y una narrativa inolvidable.",
                    59990L, "/images/juegos/godofwar.webp", "Juegos"),
            new LegacyProduct("Mousepad XL",
                    "Alfombrilla de gran tamaño para mayor precisión y comodidad durante sesiones largas.",
                    32000L, "/images/mousepads/mousepadxl.webp", "Mouse / Mousepad"),
            new LegacyProduct("PC Gamer Titan",
                    "Computadora para gaming con GPU potente, SSD rápido y amplio almacenamiento.",
                    1499990L, "/images/pcgamers/pc-gamer-titan.webp", "PC Gamers"),
            new LegacyProduct("Polera Gamer Retro",
                    "Polera con diseño retro de estilo gamer y alta calidad de impresión.",
                    25000L, "/images/poleras/polera-retro.webp", "Poleras Personalizadas"),
            new LegacyProduct("Polerón Taverna",
                    "Polerón de lana para uso diario y diseño exclusivo del proyecto.",
                    45000L, "/images/polerones/poleron-taverna.webp", "Polerones Gamers Personalizados"),
            new LegacyProduct("Silla Gamer Elite",
                    "Silla ergonómica con soporte lumbar, reposabrazos y diseño gamer moderno.",
                    229990L, "/images/sillasgamers/silla-elite.webp", "Sillas Gamers")
    );

    private final ProductoRepository productoRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DataSeeder(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Producto> existingProducts = productoRepository.findAll();
        List<Producto> obsoleteSamples = existingProducts.stream()
                .filter(this::isUnchangedLegacySample)
                .toList();
        if (!obsoleteSamples.isEmpty()) {
            productoRepository.deleteAll(obsoleteSamples);
            existingProducts = productoRepository.findAll();
        }

        Set<String> existingKeys = new HashSet<>();
        for (Producto product : existingProducts) {
            existingKeys.add(productKey(product.getTitle(), product.getCategory()));
        }

        List<Producto> missingProducts = new ArrayList<>();
        for (CatalogProduct item : loadCatalog()) {
            String key = productKey(item.title(), item.category());
            if (existingKeys.add(key)) {
                missingProducts.add(new Producto(
                        item.title(),
                        item.description(),
                        item.price(),
                        item.image(),
                        item.category(),
                        item.sourceRoute(),
                        item.platform(),
                        item.genre(),
                        item.subcategory()
                ));
                missingProducts.get(missingProducts.size() - 1).setSourceId(item.sourceId());
            }
        }

        if (!missingProducts.isEmpty()) {
            productoRepository.saveAll(missingProducts);
        }
    }

    private List<CatalogProduct> loadCatalog() {
        try (InputStream input = getClass().getResourceAsStream("/data/catalogo-inicial.json")) {
            if (input == null) {
                throw new IllegalStateException("No se encontró el catálogo inicial de productos.");
            }
            List<CatalogProduct> catalog = objectMapper.readValue(input, CATALOG_TYPE);
            if (catalog.isEmpty()) {
                throw new IllegalStateException("El catálogo inicial de productos está vacío.");
            }
            return catalog;
        } catch (IOException error) {
            throw new IllegalStateException("No se pudo leer el catálogo inicial de productos.", error);
        }
    }

    private boolean isUnchangedLegacySample(Producto product) {
        return LEGACY_SAMPLE_PRODUCTS.stream().anyMatch(sample ->
                sample.title().equals(product.getTitle())
                        && sample.description().equals(product.getDescription())
                        && sample.price().equals(product.getPrice())
                        && sample.image().equals(product.getImage())
                        && sample.category().equals(product.getCategory()));
    }

    private static String productKey(String title, String category) {
        return category.trim().toLowerCase(Locale.ROOT) + "|" + title.trim().toLowerCase(Locale.ROOT);
    }

    private record CatalogProduct(
            String sourceId,
            String title,
            String description,
            Long price,
            String image,
            String category,
            String sourceRoute,
            String platform,
            String genre,
            String subcategory
    ) {
    }

    private record LegacyProduct(String title, String description, Long price, String image, String category) {
    }
}
