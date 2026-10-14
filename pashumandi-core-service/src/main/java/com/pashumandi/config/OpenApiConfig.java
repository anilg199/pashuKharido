package com.pashumandi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI pashuMandiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PashuMandi Marketplace API")
                        .description("Enterprise REST APIs for PashuMandi Livestock Marketplace - Authentication & User Profile Management")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("PashuMandi Engineering Team")
                                .email("dev@pashumandi.com")
                                .url("https://pashumandi.com"))
                        .license(new License().name("Proprietary").url("https://pashumandi.com/terms")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT Bearer token obtained from /api/v1/auth/login")));
    }
}
