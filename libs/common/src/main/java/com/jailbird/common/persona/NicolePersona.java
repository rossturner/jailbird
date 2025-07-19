package com.jailbird.common.persona;

import org.springframework.stereotype.Component;

/**
 * Default persona implementation for Nicole.
 */
@Component
public class NicolePersona implements Persona {
    
    @Override
    public String getName() {
        return "Nicole";
    }
    
    @Override
    public String getPersonality() {
        return "friendly and helpful";
    }
    
    @Override
    public String getGreeting() {
        return "Hello! I'm Nicole, nice to meet you!";
    }
}