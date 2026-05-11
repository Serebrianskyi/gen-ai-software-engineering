# OpenAPI Specification Module

This module is responsible for generating Kotlin data classes from the OpenAPI specification.

## Purpose

- Generates API models from `../openapi.yaml`
- Provides a versioned dependency that can be used by other modules
- Ensures single source of truth for API contracts

## Generated Packages

- **Models**: `com.banking.model.generated.*`
- **APIs**: `com.banking.api.*`

## Versioning

Current version: **0.0.1**

Update the version in `build.gradle.kts` and `settings.gradle.kts` when the API spec changes significantly.

## Usage

Other modules depend on this module like:

```kotlin
dependencies {
    implementation(project(":openapi-spec"))
}
```

All generated models are automatically available with full validation annotations.

## Building

```bash
./gradlew :openapi-spec:build
```

The generated sources are compiled into a JAR and available to dependent modules.