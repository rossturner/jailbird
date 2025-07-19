package com.jailbird.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jailbird")
public class JailbirdProperties {
    // Reserved for future jailbird-specific configuration
    // Currently empty as persona system has been simplified
}