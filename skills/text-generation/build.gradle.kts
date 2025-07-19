plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("java")
    id("com.google.protobuf") version "0.9.4"
}

dependencies {
    implementation(project(":libs:common"))
    implementation(project(":libs:proto-generated"))
    
    // Spring Boot starters
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webflux") // For auto-generated REST endpoints
    
    // gRPC
    implementation("net.devh:grpc-spring-boot-starter:${property("grpcSpringBootVersion")}")
    implementation("net.devh:grpc-client-spring-boot-starter:${property("grpcSpringBootVersion")}")
    
    // gRPC-to-REST Gateway dependencies
    implementation("com.google.protobuf:protobuf-java-util:${property("protobufVersion")}")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    
    // Dependencies for auto-generated REST controllers
    implementation("io.grpc:grpc-stub:${property("grpcVersion")}")
    implementation("io.grpc:grpc-protobuf:${property("grpcVersion")}")
    implementation("io.grpc:grpc-services:${property("grpcVersion")}")
    compileOnly("javax.annotation:javax.annotation-api:1.3.2")
    
    // OpenAPI and Swagger UI for WebFlux
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.3.0")
    
    // Development
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter")
}


// Protobuf configuration for generating REST endpoints and OpenAPI
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${property("protobufVersion")}"
    }
    
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${property("grpcVersion")}"
        }
        create("webflux") {
            artifact = "io.github.protobuf-x:protoc-gen-spring-webflux:0.2.1"
        }
    }
    
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("grpc")
                create("webflux") {
                    outputSubDir = "java"
                }
            }
        }
    }
}

// Configure proto source directories
sourceSets {
    main {
        proto {
            srcDir("${rootProject.projectDir}/proto")
        }
    }
}

// Proto files are now referenced directly from the root proto directory
// No copying needed - sourceSets configuration handles this

tasks.register("dev") {
    group = "application"
    description = "Run skill in development mode with hot reload"
    dependsOn("bootRun")
}