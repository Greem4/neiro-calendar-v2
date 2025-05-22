package ru.greemlab.neirocalendarv2.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;

@Configuration
public class OpenApiConfig {

    private static final String SCHEME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                        )
                )
                .addSecurityItem(new SecurityRequirement().addList(SCHEME));
    }

    @Bean
    public OpenApiCustomizer loginUnsecured() {
        return openApi -> {
            Paths paths = openApi.getPaths();
            if (paths.containsKey("/api/v1/auth/login")) {
                paths.get("/api/v1/auth/login")
                        .readOperations()
                        .forEach(op -> op.setSecurity(new ArrayList<>()));
            }
        };
    }
}
