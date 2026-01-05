plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.9.25"
    id("com.google.devtools.ksp") version "1.9.25-1.0.20"
    id("io.micronaut.application") version "4.6.1"
    id("com.gradleup.shadow") version "8.3.9"
    id("io.micronaut.aot") version "4.6.1"
}

version = "0.1"
group = "com.agentx"

val kotlinVersion = project.properties.get("kotlinVersion")
repositories {
    mavenCentral()
}

dependencies {
    ksp("io.micronaut:micronaut-http-validation")
    ksp("io.micronaut.serde:micronaut-serde-processor")
    ksp("io.micronaut.langchain4j:micronaut-langchain4j-processor")
    ksp("io.micronaut.data:micronaut-data-processor")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
    compileOnly("io.micronaut:micronaut-http-client")
    runtimeOnly("ch.qos.logback:logback-classic")
    runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")
    testImplementation("io.micronaut:micronaut-http-client")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("com.h2database:h2")

    // LangChain4j dependencies
    implementation("io.micronaut.langchain4j:micronaut-langchain4j-core")
    implementation("io.micronaut.langchain4j:micronaut-langchain4j-openai")
    implementation("dev.langchain4j:langchain4j-open-ai")

    // PostgreSQL & Micronaut Data
    implementation("io.micronaut.sql:micronaut-jdbc-hikari")
    implementation("io.micronaut.data:micronaut-data-jdbc")
    runtimeOnly("org.postgresql:postgresql")

    // Flyway for database migrations
    implementation("io.micronaut.flyway:micronaut-flyway")
    implementation("org.flywaydb:flyway-core:10.21.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.21.0")
}


application {
    mainClass = "com.agentx.ApplicationKt"
}
java {
    sourceCompatibility = JavaVersion.toVersion("21")
}


graalvmNative.toolchainDetection = false

micronaut {
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("com.agentx.*")
    }
    aot {
        // Please review carefully the optimizations enabled below
        // Check https://micronaut-projects.github.io/micronaut-aot/latest/guide/ for more details
        optimizeServiceLoading = false
        convertYamlToJava = false
        precomputeOperations = true
        cacheEnvironment = true
        optimizeClassLoading = true
        deduceEnvironment = true
        optimizeNetty = true
        replaceLogbackXml = true
    }
}


tasks.named<io.micronaut.gradle.docker.NativeImageDockerfile>("dockerfileNative") {
    jdkVersion = "21"
}


