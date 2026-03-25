package me.dhiya.hr.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("HR Management API")
                        .version("1.0")
                        .description("HR Management System REST API"))
                .components(new Components()
                        .addSecuritySchemes("Bearer", new SecurityScheme()
                                .name("Bearer")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    @Bean
    public OperationCustomizer globalResponses() {
        return (operation, handlerMethod) -> {
            operation.getResponses().addApiResponse("500", new ApiResponse()
                    .description("Internal Server Error")
                    .content(new Content().addMediaType("application/json",
                            new MediaType().addExamples("default", new Example().value(serverErrorExample())))));
            return operation;
        };
    }

    private Map<String, Object> serverErrorExample() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "An unexpected error occurred. Please try again later.");
        body.put("errors", List.of());
        body.put("timestamp", "2026-03-25T10:00:00Z");
        return body;
    }
}
