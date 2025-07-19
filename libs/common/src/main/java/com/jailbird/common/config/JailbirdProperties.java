package com.jailbird.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "jailbird")
public class JailbirdProperties {
    
    private String persona = "default";
    private PersonaConfig personas = new PersonaConfig();
    
    public String getPersona() {
        return persona;
    }
    
    public void setPersona(String persona) {
        this.persona = persona;
    }
    
    public PersonaConfig getPersonas() {
        return personas;
    }
    
    public void setPersonas(PersonaConfig personas) {
        this.personas = personas;
    }
    
    public static class PersonaConfig {
        private Map<String, Map<String, Object>> config;
        
        public Map<String, Map<String, Object>> getConfig() {
            return config;
        }
        
        public void setConfig(Map<String, Map<String, Object>> config) {
            this.config = config;
        }
    }
}