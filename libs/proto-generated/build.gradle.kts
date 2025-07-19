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