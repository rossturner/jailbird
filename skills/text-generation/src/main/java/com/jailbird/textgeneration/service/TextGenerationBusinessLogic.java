package com.jailbird.textgeneration.service;

import com.jailbird.common.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TextGenerationBusinessLogic {

    private static final Logger log = LoggerFactory.getLogger(TextGenerationBusinessLogic.class);

    /**
     * Generate text based on prompt and context.
     * For now, this is a mock implementation that will be replaced with actual LLM integration.
     */
    public String generateText(String prompt, Context context) {
        log.info("Generating text for prompt: '{}'", prompt);
        
        // Mock text generation - in real implementation this would call an LLM
        String response = generateMockResponse(prompt, context);
        
        log.info("Generated response: '{}'", response);
        return response;
    }

    /**
     * Mock response generation - replace with actual LLM integration
     */
    private String generateMockResponse(String prompt, Context context) {
        // Simple mock responses based on prompt content
        if (prompt.toLowerCase().contains("hello") || prompt.toLowerCase().contains("hi")) {
            return "Hello! Nice to meet you! How can I help you today?";
        }
        
        if (prompt.toLowerCase().contains("how are you")) {
            return "I'm doing great, thanks for asking! I'm always excited to chat and help out.";
        }
        
        if (prompt.toLowerCase().contains("what") && prompt.toLowerCase().contains("name")) {
            return "I'm an AI assistant here to help you.";
        }
        
        if (prompt.toLowerCase().contains("weather")) {
            return "I don't have access to current weather data, but I'd be happy to help you with other topics!";
        }
        
        if (prompt.toLowerCase().contains("joke")) {
            return "Why don't scientists trust atoms? Because they make up everything! *giggles*";
        }
        
        // Default response
        return "That's an interesting question! I'd love to help you explore that topic further. " +
               "Could you tell me more about what you're looking for?";
    }

}