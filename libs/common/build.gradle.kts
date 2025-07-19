plugins {
    id("java-library")
    id("org.springframework.boot") version "3.2.1" apply false
    id("io.spring.dependency-management") version "1.1.4"
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${property("springBootVersion")}")
    }
}

dependencies {
    api("org.springframework:spring-context")
    api("org.springframework:spring-web")
    api("org.springframework.boot:spring-boot-starter-logging")
    api("org.springframework.boot:spring-boot-autoconfigure")
    
    // Configuration
    api("org.springframework.boot:spring-boot-configuration-processor")
    api("org.yaml:snakeyaml")
    
    // Validation
    api("org.springframework.boot:spring-boot-starter-validation")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}