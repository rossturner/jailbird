package com.jailbird.common.persona;

/**
 * Simple interface for persona implementations.
 * Each persona provides basic information that skills can use.
 */
public interface Persona {
    
    /**
     * The name of the persona.
     * @return The persona's name
     */
    String getName();
    
    /**
     * A brief description of the persona's personality.
     * @return The persona's personality description
     */
    String getPersonality();
    
    /**
     * A default greeting from this persona.
     * @return The persona's greeting message
     */
    String getGreeting();
}