package com.jailbird.textgeneration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = {
    "com.jailbird.textgeneration",
    "com.jailbird.common"
})
@ConfigurationPropertiesScan
public class TextGenerationApplication {

    public static void main(String[] args) {
        SpringApplication.run(TextGenerationApplication.class, args);
    }
}