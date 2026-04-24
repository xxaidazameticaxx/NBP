package ba.unsa.etf.NBP.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI configuration for Swagger UI.
 * <p>
 * Configures API documentation metadata and JWT bearer authentication
 * for interactive API exploration via Swagger UI.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        String schemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info().title("NBP API").version("1.0"))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .schemaRequirement(schemeName, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Access token from /auth/login or /auth/refresh"));
    }
}
