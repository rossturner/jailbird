rootProject.name = "jailbird"

include(
    // Shared libraries
    "libs:common",
    "libs:proto-generated",
    
    // Skills (microservices)
    "skills:text-generation"
)