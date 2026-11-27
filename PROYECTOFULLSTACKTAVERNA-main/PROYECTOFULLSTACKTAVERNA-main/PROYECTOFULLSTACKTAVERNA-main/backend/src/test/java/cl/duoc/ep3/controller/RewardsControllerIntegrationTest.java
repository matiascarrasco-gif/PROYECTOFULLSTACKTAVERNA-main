package cl.duoc.ep3.controller;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.duoc.ep3.model.Account;
import cl.duoc.ep3.model.Producto;
import cl.duoc.ep3.model.UserRole;
import cl.duoc.ep3.repository.AccountRepository;
import cl.duoc.ep3.repository.LoginSessionRepository;
import cl.duoc.ep3.repository.ProductoRepository;
import cl.duoc.ep3.repository.PurchaseOrderRepository;
import cl.duoc.ep3.repository.RewardRedemptionRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RewardsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LoginSessionRepository loginSessionRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private RewardRedemptionRepository redemptionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void resetDatabase() {
        purchaseOrderRepository.deleteAll();
        redemptionRepository.deleteAll();
        loginSessionRepository.deleteAll();
        accountRepository.deleteAll();
        productoRepository.deleteAll();
    }

    @Test
    void awardsRegistrationBonusOnlyForTheAdvertisedCode() throws Exception {
        String customerJson = """
                {
                  "correo": "levelup@example.com",
                  "password": "secure-password-123",
                  "nombres": "Level",
                  "apellidoPaterno": "Up",
                  "apellidoMaterno": "Cliente",
                  "rut": "123456785",
                  "birthDate": "1990-01-01",
                  "telefono": "912345678",
                  "direccion": "Calle de prueba 123",
                  "codigoReferido": "WelcomeToLevelUp"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customerJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.puntos").value(20));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customerJson.replace("levelup@example.com", "other@example.com")
                                .replace("WelcomeToLevelUp", "made-up-code")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void calculatesPointsFromServerPricesAndDoesNotAwardTwiceForRetries() throws Exception {
        Account customer = saveAccount("customer@duoc.cl", 0);
        Producto product = productoRepository.save(new Producto(
                "PC gamer de prueba", "Producto de prueba", 250_000L, "/images/pc.webp",
                "PC Gamers", "", "PC", "Gaming", "PC"));
        String token = loginAndGetToken(customer.getEmail());
        UUID orderId = UUID.fromString("123e4567-e89b-42d3-a456-426614174000");
        String request = """
                {"orderId":"%s","items":[{"productId":%d,"quantity":1}]}
                """.formatted(orderId, product.getId());

        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(post("/api/orders")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.subtotal").value(250000))
                    .andExpect(jsonPath("$.discount").value(50000))
                    .andExpect(jsonPath("$.total").value(200000))
                    .andExpect(jsonPath("$.pointsEarned").value(20))
                    .andExpect(jsonPath("$.puntosSaldo").value(20));
        }

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pricesAndValidatesRedemptionsOnTheServer() throws Exception {
        Account customer = saveAccount("rewards@example.com", 0);
        customer.addPoints(100);
        customer = accountRepository.save(customer);
        Producto shirt = productoRepository.save(new Producto(
                "Polera Gamer", "Polera de prueba", 25_000L, "/images/polera.webp",
                "Poleras Personalizadas", "Poleras.html", "PC", "", "Personalizada"));
        String token = loginAndGetToken(customer.getEmail());
        MvcResult productResponse = mockMvc.perform(get("/api/productos/{id}", shirt.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puntosCanje", greaterThanOrEqualTo(60)))
                .andExpect(jsonPath("$.puntosCanje", lessThanOrEqualTo(80)))
                .andReturn();
        int cost = com.jayway.jsonpath.JsonPath.read(productResponse.getResponse().getContentAsString(), "$.puntosCanje");

        MvcResult redemptionResponse = mockMvc.perform(post("/api/rewards/redemptions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":%d}
                                """.formatted(shirt.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pointsCost").value(cost))
                .andExpect(jsonPath("$.puntosSaldo").value(100 - cost))
                .andReturn();
        String redemptionId = com.jayway.jsonpath.JsonPath.read(
                redemptionResponse.getResponse().getContentAsString(), "$.redemptionId");

        String orderRequest = """
                {"orderId":"123e4567-e89b-42d3-a456-426614174001",
                 "items":[{"productId":%d,"quantity":1,"redemptionId":"%s"}]}
                """.formatted(shirt.getId(), redemptionId);
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(0))
                .andExpect(jsonPath("$.pointsEarned").value(0))
                .andExpect(jsonPath("$.puntosSaldo").value(100 - cost));
    }

    private Account saveAccount(String email, int points) {
        Account account = new Account(
                email,
                passwordEncoder.encode("secure-password-123"),
                "Cliente",
                "La",
                "Taverna",
                "123456785",
                LocalDate.of(1990, 1, 1),
                "912345678",
                "Calle de prueba 123",
                email.endsWith("@duoc.cl") || email.endsWith("@duocuc.cl"),
                UserRole.CUSTOMER);
        account.addPoints(points);
        return accountRepository.save(account);
    }

    private String loginAndGetToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"%s","password":"secure-password-123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }
}
