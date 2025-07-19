package com.jailbird.textgeneration.controller;

import com.google.protobuf.util.JsonFormat;
import com.jailbird.common.HealthCheckRequest;
import com.jailbird.common.HealthCheckResponse;
import com.jailbird.skills.textgeneration.GenerateTextRequest;
import com.jailbird.skills.textgeneration.GenerateTextResponse;
import com.jailbird.textgeneration.service.TextGenerationGrpcService;
import io.grpc.stub.StreamObserver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller automatically derived from protobuf service definitions.
 * 
 * This controller provides REST endpoints that map exactly to the google.api.http
 * annotations in our text_generation.proto file:
 * 
 * - POST /api/v1/text-generation/generate → TextGenerationService.GenerateText
 * - POST /api/v1/text-generation/generate-stream → TextGenerationService.GenerateTextStream  
 * - GET /api/v1/text-generation/health → TextGenerationService.Health
 * 
 * The controller acts as a bridge between HTTP/JSON and gRPC/Protobuf, providing
 * automatic JSON ↔ Protobuf conversion while maintaining full OpenAPI documentation.
 */
@RestController
@RequestMapping("/api/v1/text-generation")
@Tag(name = "Text Generation", description = "AI-powered text generation service derived from protobuf definitions")
public class TextGenerationController {

    private static final Logger log = LoggerFactory.getLogger(TextGenerationController.class);
    
    private static final JsonFormat.Parser JSON_PARSER = JsonFormat.parser().ignoringUnknownFields();
    private static final JsonFormat.Printer JSON_PRINTER = JsonFormat.printer()
            .includingDefaultValueFields()
            .omittingInsignificantWhitespace();

    private final TextGenerationGrpcService grpcService;

    public TextGenerationController(TextGenerationGrpcService grpcService) {
        this.grpcService = grpcService;
    }

    /**
     * Generate text based on prompt and context.
     * Maps to: TextGenerationService.GenerateText
     * Proto annotation: option (google.api.http) = { post: "/api/v1/text-generation/generate" body: "*" };
     */
    @PostMapping(value = "/generate", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Generate text from prompt",
            description = "Generate AI text response based on input prompt and context. Derived from protobuf service definition.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully generated text response",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = String.class),
                                    examples = @ExampleObject(
                                            name = "Generated text response",
                                            value = """
                                                    {
                                                      "generatedText": "Hello! I'm Jailbird, nice to meet you!",
                                                      "metadata": {
                                                        "modelName": "mock-model",
                                                        "inputTokens": 4,
                                                        "outputTokens": 8,
                                                        "generationTimeMs": 100.0,
                                                        "tokensPerSecond": 50.0,
                                                        "finishReason": "stop"
                                                      },
                                                      "isFinal": true,
                                                      "tokenCount": 8
                                                    }
                                                    """))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            })
    public ResponseEntity<String> generateText(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Text generation request with prompt and optional context",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Generate text request",
                                    value = """
                                            {
                                              "prompt": "Hello, how are you?",
                                              "context": {
                                                "persona": {
                                                  "personaId": "default",
                                                  "name": "Jailbird"
                                                },
                                                "timestamp": 1234567890
                                              }
                                            }
                                            """)))
            @RequestBody String jsonRequest) {
        
        log.info("Proto-derived REST API: Received generate text request");

        try {
            GenerateTextRequest request = parseGenerateTextRequest(jsonRequest);
            GenerateTextResponse response = callGenerateText(request);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(toJson(response));
        } catch (Exception e) {
            log.error("Error processing generate text request", e);
            return ResponseEntity.status(500)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Stream text generation for real-time responses.
     * Maps to: TextGenerationService.GenerateTextStream
     * Proto annotation: option (google.api.http) = { post: "/api/v1/text-generation/generate-stream" body: "*" };
     */
    @PostMapping(value = "/generate-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Generate streaming text from prompt",
            description = "Generate AI text response with real-time streaming. Derived from protobuf service definition.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Server-sent events stream of text generation progress",
                            content = @Content(
                                    mediaType = "text/event-stream",
                                    examples = @ExampleObject(
                                            name = "Streaming text generation",
                                            value = """
                                                    data: {"generatedText": "Hello!", "isFinal": false, "tokenCount": 1}
                                                    
                                                    data: {"generatedText": "Hello! I'm", "isFinal": false, "tokenCount": 2}
                                                    
                                                    data: {"generatedText": "Hello! I'm Jailbird", "isFinal": true, "tokenCount": 3}
                                                    """))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            })
    public SseEmitter generateTextStream(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Text generation request for streaming response",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Streaming request",
                                    value = """
                                            {
                                              "prompt": "Count to 5",
                                              "context": {
                                                "persona": {
                                                  "personaId": "default",
                                                  "name": "Jailbird"
                                                }
                                              }
                                            }
                                            """)))
            @RequestBody String jsonRequest) {
        
        log.info("Proto-derived REST API: Received streaming generate text request");

        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L); // 5 minute timeout

        try {
            GenerateTextRequest request = parseGenerateTextRequest(jsonRequest);
            
            grpcService.generateTextStream(request, new StreamObserver<GenerateTextResponse>() {
                @Override
                public void onNext(GenerateTextResponse response) {
                    try {
                        String json = toJson(response);
                        emitter.send(SseEmitter.event().data(json));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    log.error("gRPC streaming call failed", throwable);
                    emitter.completeWithError(throwable);
                }

                @Override
                public void onCompleted() {
                    log.info("gRPC streaming completed");
                    emitter.complete();
                }
            });
        } catch (Exception e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * Health check endpoint.
     * Maps to: TextGenerationService.Health
     * Proto annotation: option (google.api.http) = { get: "/api/v1/text-generation/health" };
     */
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Service health check",
            description = "Check if the text generation service is healthy and operational. Derived from protobuf service definition.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Service is healthy",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "Healthy service response",
                                            value = """
                                                    {
                                                      "status": "SERVING",
                                                      "message": "Text Generation Service is healthy",
                                                      "timestamp": "1752923997400",
                                                      "metadata": {
                                                        "service": "text-generation",
                                                        "version": "0.1.0"
                                                      }
                                                    }
                                                    """))),
                    @ApiResponse(responseCode = "503", description = "Service unavailable")
            })
    public ResponseEntity<String> health() {
        log.info("Proto-derived REST API: Received health check request");

        try {
            HealthCheckRequest healthRequest = HealthCheckRequest.newBuilder()
                    .setService("text-generation")
                    .build();

            HealthCheckResponse response = callHealth(healthRequest);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(toJson(response));
        } catch (Exception e) {
            log.error("Error processing health check request", e);
            return ResponseEntity.status(503)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\": \"Service unavailable: " + e.getMessage() + "\"}");
        }
    }

    // Helper methods for proto ↔ JSON conversion and gRPC calls

    private GenerateTextRequest parseGenerateTextRequest(String jsonBody) throws Exception {
        GenerateTextRequest.Builder builder = GenerateTextRequest.newBuilder();
        JSON_PARSER.merge(jsonBody, builder);
        return builder.build();
    }

    private GenerateTextResponse callGenerateText(GenerateTextRequest request) throws Exception {
        CompletableFuture<GenerateTextResponse> future = new CompletableFuture<>();
        
        grpcService.generateText(request, new StreamObserver<GenerateTextResponse>() {
            @Override
            public void onNext(GenerateTextResponse response) {
                future.complete(response);
            }

            @Override
            public void onError(Throwable throwable) {
                future.completeExceptionally(throwable);
            }

            @Override
            public void onCompleted() {
                // Response already sent in onNext
            }
        });
        
        return future.get(); // Wait for completion
    }

    private HealthCheckResponse callHealth(HealthCheckRequest request) throws Exception {
        CompletableFuture<HealthCheckResponse> future = new CompletableFuture<>();
        
        grpcService.health(request, new StreamObserver<HealthCheckResponse>() {
            @Override
            public void onNext(HealthCheckResponse response) {
                future.complete(response);
            }

            @Override
            public void onError(Throwable throwable) {
                future.completeExceptionally(throwable);
            }

            @Override
            public void onCompleted() {
                // Response already sent in onNext
            }
        });
        
        return future.get(); // Wait for completion
    }

    private String toJson(com.google.protobuf.MessageOrBuilder message) {
        try {
            return JSON_PRINTER.print(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert protobuf to JSON", e);
        }
    }
}