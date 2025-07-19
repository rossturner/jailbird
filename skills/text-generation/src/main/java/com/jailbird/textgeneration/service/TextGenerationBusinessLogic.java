package com.jailbird.textgeneration.service;

import com.jailbird.common.Context;
import com.jailbird.common.config.JailbirdProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TextGenerationBusinessLogic {

    private static final Logger log = LoggerFactory.getLogger(TextGenerationBusinessLogic.class);

    @Autowired
    private JailbirdProperties jailbirdProperties;

    /**
     * Generate text based on prompt and context.
     * For now, this is a mock implementation that will be replaced with actual LLM integration.
     */
    public String generateText(String prompt, Context context) {
        log.info("Generating text for prompt: '{}' with persona: '{}'", 
                prompt, getCurrentPersona(context));
        
        // Mock text generation - in real implementation this would call an LLM
        String response = generateMockResponse(prompt, context);
        
        log.info("Generated response: '{}'", response);
        return response;
    }

    /**
     * Mock response generation - replace with actual LLM integration
     */
    private String generateMockResponse(String prompt, Context context) {
        String persona = getCurrentPersona(context);
        
        // Simple mock responses based on prompt content
        if (prompt.toLowerCase().contains("hello") || prompt.toLowerCase().contains("hi")) {
            return String.format("Hello! I'm %s, nice to meet you! How can I help you today?", persona);
        }
        
        if (prompt.toLowerCase().contains("how are you")) {
            return String.format("I'm doing great, thanks for asking! As %s, I'm always excited to chat and help out.", persona);
        }
        
        if (prompt.toLowerCase().contains("what") && prompt.toLowerCase().contains("name")) {
            return String.format("My name is %s! I'm an AI assistant here to help you.", persona);
        }
        
        if (prompt.toLowerCase().contains("weather")) {
            return "I don't have access to current weather data, but I'd be happy to help you with other topics!";
        }
        
        if (prompt.toLowerCase().contains("joke")) {
            return "Why don't scientists trust atoms? Because they make up everything! *giggles*";
        }
        
        // Default response
        return String.format("That's an interesting question! As %s, I'd love to help you explore that topic further. " +
                           "Could you tell me more about what you're looking for?", persona);
    }

    /**
     * Get current persona name from context or fall back to default
     */
    private String getCurrentPersona(Context context) {
        if (context != null && context.hasPersona() && !context.getPersona().getName().isEmpty()) {
            return context.getPersona().getName();
        }
        
        return jailbirdProperties.getPersona();
    }
}