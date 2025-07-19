plugins {
    id("java-library")
    id("com.google.protobuf") version "0.9.4"
}

dependencies {
    api("io.grpc:grpc-stub:${property("grpcVersion")}")
    api("io.grpc:grpc-protobuf:${property("grpcVersion")}")
    api("com.google.protobuf:protobuf-java:${property("protobufVersion")}")
    
    // For @Generated annotation
    compileOnly("javax.annotation:javax.annotation-api:1.3.2")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${property("protobufVersion")}"
    }
    
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${property("grpcVersion")}"
        }
    }
    
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("grpc")
            }
        }
    }
}

// Copy proto files from root to this module for generation
val copyProtos = tasks.register<Copy>("copyProtos") {
    from("${rootProject.projectDir}/proto")
    into("${project.projectDir}/src/main/proto")
}

tasks.named("generateProto") {
    dependsOn(copyProtos)
}

tasks.named("processResources") {
    dependsOn(copyProtos)
}

// Clean up copied protos
tasks.named("clean") {
    doLast {
        delete("${project.projectDir}/src/main/proto")
    }
}