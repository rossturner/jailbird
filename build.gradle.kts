plugins {
    id("java-library")
    id("com.google.protobuf") version "0.9.4" apply false
    id("org.springframework.boot") version "3.2.1" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
}

allprojects {
    group = "com.jailbird"
    version = "1.0.0"
    
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java-library")
    
    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    
    dependencies {
        implementation("org.slf4j:slf4j-api:2.0.10")
        implementation("ch.qos.logback:logback-classic:1.4.14")
        
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
        testImplementation("org.testcontainers:junit-jupiter:1.19.3")
        testImplementation("org.mockito:mockito-core:5.8.0")
        testImplementation("org.mockito:mockito-junit-jupiter:5.8.0")
    }
    
    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

// Root-level tasks
tasks.register("startDev") {
    group = "application"
    description = "Start all services in development mode"
    doLast {
        exec {
            commandLine("docker-compose", "up", "-d")
        }
    }
}

tasks.register("stopDev") {
    group = "application"  
    description = "Stop all services"
    doLast {
        exec {
            commandLine("docker-compose", "down")
        }
    }
}

tasks.register("genProto") {
    group = "build"
    description = "Generate protobuf code"
    dependsOn(":libs:proto-generated:generateProto")
}