plugins {
    kotlin("jvm") version "2.2.20"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.3.1")

    implementation("org.entur.ror:netex-pipeline:0.0.29")
    implementation("org.entur.ror:netex-tools-lib:0.0.29")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}