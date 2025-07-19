package com.jailbird.textgeneration.service;

import com.jailbird.common.HealthCheckRequest;
import com.jailbird.common.HealthCheckResponse;
import com.jailbird.skills.textgeneration.GenerateTextRequest;
import com.jailbird.skills.textgeneration.GenerateTextResponse;
import com.jailbird.skills.textgeneration.GenerationMetadata;
import com.jailbird.skills.textgeneration.TextGenerationServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@GrpcService
@Component
public class TextGenerationGrpcService extends TextGenerationServiceGrpc.TextGenerationServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(TextGenerationGrpcService.class);

    @Autowired
    private TextGenerationBusinessLogic businessLogic;

    @Override
    public void generateText(GenerateTextRequest request,
                           StreamObserver<GenerateTextResponse> responseObserver) {
        try {
            log.info("Received text generation request: {}", request.getPrompt());
            
            String generatedText = businessLogic.generateText(request.getPrompt(), request.getContext());
            
            GenerationMetadata metadata = GenerationMetadata.newBuilder()
                .setModelName("mock-model")
                .setInputTokens(request.getPrompt().split("\\s+").length)
                .setOutputTokens(generatedText.split("\\s+").length)
                .setGenerationTimeMs(100.0)
                .setTokensPerSecond(50.0)
                .setFinishReason("stop")
                .build();
            
            GenerateTextResponse response = GenerateTextResponse.newBuilder()
                .setGeneratedText(generatedText)
                .setMetadata(metadata)
                .setIsFinal(true)
                .setTokenCount(generatedText.split("\\s+").length)
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
            log.info("Text generation completed successfully");
            
        } catch (Exception e) {
            log.error("Error generating text", e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Failed to generate text: " + e.getMessage())
                .asRuntimeException());
        }
    }

    @Override
    public void generateTextStream(GenerateTextRequest request,
                                 StreamObserver<GenerateTextResponse> responseObserver) {
        try {
            log.info("Received streaming text generation request: {}", request.getPrompt());
            
            // For now, simulate streaming by sending the response in chunks
            String generatedText = businessLogic.generateText(request.getPrompt(), request.getContext());
            String[] words = generatedText.split("\\s+");
            
            StringBuilder accumulated = new StringBuilder();
            
            for (int i = 0; i < words.length; i++) {
                accumulated.append(words[i]);
                if (i < words.length - 1) {
                    accumulated.append(" ");
                }
                
                boolean isFinal = (i == words.length - 1);
                
                GenerationMetadata metadata = GenerationMetadata.newBuilder()
                    .setModelName("mock-model")
                    .setInputTokens(request.getPrompt().split("\\s+").length)
                    .setOutputTokens(i + 1)
                    .setGenerationTimeMs(100.0 * (i + 1))
                    .setTokensPerSecond(50.0)
                    .setFinishReason(isFinal ? "stop" : "")
                    .build();
                
                GenerateTextResponse response = GenerateTextResponse.newBuilder()
                    .setGeneratedText(accumulated.toString())
                    .setMetadata(metadata)
                    .setIsFinal(isFinal)
                    .setTokenCount(i + 1)
                    .build();
                
                responseObserver.onNext(response);
                
                // Simulate streaming delay
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            responseObserver.onCompleted();
            log.info("Streaming text generation completed");
            
        } catch (Exception e) {
            log.error("Error in streaming text generation", e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Failed to generate streaming text: " + e.getMessage())
                .asRuntimeException());
        }
    }

    @Override
    public void health(HealthCheckRequest request,
                      StreamObserver<HealthCheckResponse> responseObserver) {
        try {
            HealthCheckResponse response = HealthCheckResponse.newBuilder()
                .setStatus(HealthCheckResponse.ServingStatus.SERVING)
                .setMessage("Text Generation Service is healthy")
                .setTimestamp(System.currentTimeMillis())
                .putMetadata("service", "text-generation")
                .putMetadata("version", "0.1.0")
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in health check", e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Health check failed: " + e.getMessage())
                .asRuntimeException());
        }
    }
}