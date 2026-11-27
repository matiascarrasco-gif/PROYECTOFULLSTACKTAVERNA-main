package cl.duoc.ep3.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

// Declara metadatos generales que Springdoc muestra en Swagger UI.
@Configuration
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@OpenAPIDefinition(
        info = @Info(
                title = "API La Taverna",
                version = "1.0",
                description = "API REST para el catálogo de productos de La Taverna.",
                contact = @Contact(name = "Proyecto La Taverna")
        )
)
public class OpenApiConfig {
}
