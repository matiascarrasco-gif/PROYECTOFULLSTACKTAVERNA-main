package cl.duoc.ep3.controller;

import cl.duoc.ep3.model.Account;
import cl.duoc.ep3.model.UserRole;
import cl.duoc.ep3.repository.AccountRepository;
import cl.duoc.ep3.repository.LoginSessionRepository;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductoControllerIntegrationTest {

    private static final Path IMAGE_DIRECTORY = createTemporaryImageDirectory();
    private static final String ADMIN_REGISTRATION_KEY = "test-admin-invitation-key-with-at-least-32-characters";

    @DynamicPropertySource
    static void configureImageDirectory(DynamicPropertyRegistry registry) {
        registry.add("app.product-images.directory", () -> IMAGE_DIRECTORY.toString());
        registry.add("admin.registration-key", () -> ADMIN_REGISTRATION_KEY);
    }

    @AfterAll
    static void removeTemporaryImages() throws IOException {
        if (Files.exists(IMAGE_DIRECTORY)) {
            try (Stream<Path> paths = Files.walk(IMAGE_DIRECTORY)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private static final String PRODUCT_JSON = """
            {
              "title": "Teclado de prueba",
              "description": "Teclado para verificar el CRUD.",
              "price": 54990,
              "image": "/images/accesorios/teclado.webp",
              "category": "Accesorios"
            }
            """;

    private static final String REGISTER_JSON = """
            {
              "correo": "cliente@example.com",
              "password": "cliente-seguro-123",
              "nombres": "Cliente",
              "apellidoPaterno": "Prueba",
              "apellidoMaterno": "Taverna",
              "rut": "123456785",
              "birthDate": "1990-01-01",
              "telefono": "912345678",
              "direccion": "Calle de prueba 123"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private cl.duoc.ep3.repository.ProductoRepository productoRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LoginSessionRepository loginSessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void resetDatabase() {
        loginSessionRepository.deleteAll();
        accountRepository.deleteAll();
        productoRepository.deleteAll();
        accountRepository.save(new Account(
                "admin@example.com", passwordEncoder.encode("admin-password-123"),
                "Admin", "La", "Taverna", "111111111", LocalDate.of(1990, 1, 1),
                "912345678", "Dirección de prueba", false, UserRole.ADMIN));
    }

    @Test
    void exposesPublicReadsAndProtectsProductWritesByRole() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCT_JSON))
                .andExpect(status().isUnauthorized());

        String customerToken = registerCustomer();
        mockMvc.perform(post("/api/productos")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCT_JSON))
                .andExpect(status().isForbidden());

        String adminToken = login("admin@example.com", "admin-password-123");
        mockMvc.perform(post("/api/productos")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCT_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Teclado de prueba"));

        Long productId = productoRepository.findAll().get(0).getId();
        mockMvc.perform(get("/api/productos/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Teclado de prueba"));

        mockMvc.perform(put("/api/productos/{id}", productId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Teclado actualizado",
                                  "description": "Producto editado mediante la API.",
                                  "price": 59990,
                                  "image": "/images/accesorios/teclado.webp",
                                  "category": "Accesorios"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Teclado actualizado"))
                .andExpect(jsonPath("$.price").value(59990));

        mockMvc.perform(delete("/api/productos/{id}", productId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void rejectsInvalidProductsAndReportsMissingProducts() throws Exception {
        String adminToken = login("admin@example.com", "admin-password-123");
        mockMvc.perform(post("/api/productos")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":" ","description":"","price":0,"image":"","category":""}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/productos/999999"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/productos/999999")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void restrictsImageUploadsToAdministratorsAndServesUploadedImagesPublicly() throws Exception {
        byte[] png = createPng();
        MockMultipartFile image = new MockMultipartFile("file", "product.png", "image/png", png);

        mockMvc.perform(multipart("/api/uploads/product-images").file(image))
                .andExpect(status().isUnauthorized());

        String customerToken = registerCustomer();
        mockMvc.perform(multipart("/api/uploads/product-images")
                        .file(image)
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isForbidden());

        String adminToken = login("admin@example.com", "admin-password-123");
        MvcResult upload = mockMvc.perform(multipart("/api/uploads/product-images")
                        .file(image)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.image").exists())
                .andReturn();
        String imagePath = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(upload.getResponse().getContentAsString())
                .get("image").asText();

        mockMvc.perform(get("/api" + imagePath))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentType(MediaType.IMAGE_PNG))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().bytes(png));
    }

    @Test
    void supportsRegistrationLoginMeAndServerSideLogout() throws Exception {
        MvcResult registration = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"))
                .andReturn();
        String token = jsonField(registration, "accessToken");

        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("cliente@example.com"));

        mockMvc.perform(put("/api/auth/me")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombres": "Cliente actualizado",
                                  "apellidoPaterno": "Prueba",
                                  "apellidoMaterno": "Taverna",
                                  "rut": "123456785",
                                  "telefono": "987654321",
                                  "direccion": "Nueva dirección 456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombres").value("Cliente actualizado"));
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.direccion").value("Nueva dirección 456"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"cliente@example.com","password":"incorrecta"}
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/logout").header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsDuplicateRegistrationAndInvalidRut() throws Exception {
        registerCustomer();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_JSON))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_JSON.replace("123456785", "123456784")))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_JSON.replace("cliente@example.com", "otro@example.com")
                                .replace("123456785", "123456784")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiresInvitationKeyToRegisterAnAdministrator() throws Exception {
        String adminRegistration = REGISTER_JSON
                .replace("cliente@example.com", "admin-invitado@example.com")
                .replace("cliente-seguro-123", "admin-seguro-123");

        mockMvc.perform(post("/api/auth/admin/register")
                        .header("X-Admin-Registration-Key", "clave incorrecta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adminRegistration))
                .andExpect(status().isForbidden());

        MvcResult registration = mockMvc.perform(post("/api/auth/admin/register")
                        .header("X-Admin-Registration-Key", ADMIN_REGISTRATION_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adminRegistration))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andReturn();

        String token = jsonField(registration, "accessToken");
        mockMvc.perform(post("/api/productos")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCT_JSON))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"));
    }

    @Test
    void exposesSwaggerAndAllowsConfiguredDevelopmentOrigin() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/productos']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/login']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/admin/register']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/auth/me'].get.security[0].bearerAuth").exists());

        mockMvc.perform(options("/api/productos")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    private String registerCustomer() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_JSON))
                .andExpect(status().isCreated())
                .andReturn();
        return jsonField(result, "accessToken");
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return jsonField(result, "accessToken");
    }

    private String jsonField(MvcResult result, String field) throws Exception {
        return new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(result.getResponse().getContentAsString())
                .get(field)
                .asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private static Path createTemporaryImageDirectory() {
        try {
            return Files.createTempDirectory("la-taverna-product-images-test-");
        } catch (IOException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    private byte[] createPng() throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
