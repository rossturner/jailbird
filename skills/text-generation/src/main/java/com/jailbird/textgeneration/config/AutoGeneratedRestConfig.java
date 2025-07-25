package com.jailbird.textgeneration.config;

import com.jailbird.skills.textgeneration.TextGenerationServiceGrpc;
import com.jailbird.skills.textgeneration.TextGenerationServiceRest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Configuration for TRUE auto-generated REST endpoints from protoc-gen-spring-webflux.
 * 
 * IMPORTANT: This uses ONLY the auto-generated code from build/generated/source/proto/
 * The TextGenerationServiceRest class is generated by protoc-gen-spring-webflux during
 * build time from the .proto file and google.api.http annotations.
 * 
 * NO manual controllers are used - everything is derived from the proto definition.
 */
@Configuration
public class AutoGeneratedRestConfig {

    /**
     * Creates a gRPC channel to connect to our local gRPC service.
     * This will be used by the auto-generated gRPC proxy to call our service.
     */
    @Bean
    public ManagedChannel grpcChannel() {
        return ManagedChannelBuilder.forAddress("localhost", 50005)
                .usePlaintext()
                .build();
    }

    /**
     * Creates a gRPC stub for the auto-generated proxy.
     * This stub is used by the generated code to make gRPC calls.
     */
    @Bean
    public TextGenerationServiceGrpc.TextGenerationServiceStub textGenerationStub(ManagedChannel channel) {
        return TextGenerationServiceGrpc.newStub(channel);
    }

    /**
     * Creates the AUTO-GENERATED REST handler using the protoc-gen-spring-webflux generated code.
     * 
     * This handler is created by the TextGenerationServiceRest.newGrpcProxyBuilder() 
     * which is generated at build time from our .proto file.
     * 
     * The generated code automatically:
     * - Maps google.api.http annotations to REST endpoints
     * - Converts JSON ↔ Protobuf
     * - Proxies REST calls to gRPC service
     */
    @Bean
    public TextGenerationServiceRest.TextGenerationServiceHandler autoGeneratedRestHandler(
            TextGenerationServiceGrpc.TextGenerationServiceStub stub) {
        
        return TextGenerationServiceRest.newGrpcProxyBuilder()
                .setStub(stub)
                .build();
    }

    /**
     * Registers the AUTO-GENERATED REST routes.
     * 
     * These routes are created by the generated TextGenerationServiceHandler.allRoutes()
     * method, which reads the google.api.http annotations from the proto file:
     * 
     * From text_generation.proto:
     * - POST /generate_text (google.api.http annotation)
     * - POST /generate_text_stream (google.api.http annotation)  
     * - GET /health (google.api.http annotation)
     * 
     * Changes to proto annotations → regenerate code → automatic REST endpoint updates
     */
    @Bean
    public RouterFunction<ServerResponse> autoGeneratedRoutes(
            TextGenerationServiceRest.TextGenerationServiceHandler handler) {
        
        return handler.allRoutes();
    }
}