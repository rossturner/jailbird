package com.jailbird.textgeneration.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI configuration for auto-generated REST endpoints.
 * 
 * This configuration sets up Swagger UI and OpenAPI documentation
 * for the REST endpoints that are automatically generated from proto files.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI textGenerationOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Jailbird Text Generation API")
                        .description("REST API automatically generated from protobuf definitions with google.api.http annotations")
                        .version("0.1.0")
                        .contact(new Contact()
                                .name("Jailbird API Team")
                                .url("https://github.com/jailbird/api")
                                .email("api@jailbird.dev")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development server"),
                        new Server()
                                .url("http://172.22.53.175:8080")
                                .description("WSL Development server (accessible from Windows)")));
    }
}