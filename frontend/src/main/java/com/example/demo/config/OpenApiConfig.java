package com.example.demo.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Travel App API",
                version = "1.0",
                description = "Documentazione ufficiale delle API per la gestione di viaggi e prenotazioni"
        ),
        security = @SecurityRequirement(name = "bearerAuth") // Applica la sicurezza a tutte le rotte di default
)
@SecurityScheme(
        name = "bearerAuth",
        description = "Fai il login su /api/auth/login, copia il token (senza virgolette) e incollalo qui",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}