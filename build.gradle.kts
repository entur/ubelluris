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
    implementation("org.entur.ror:netex-pipeline:0.0.24")
    implementation("org.entur.ror:netex-tools-lib:0.0.24")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}