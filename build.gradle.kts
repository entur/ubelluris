plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
    alias(libs.plugins.shadow)
    alias(libs.plugins.ktlint)
}

group = "org.entur.ror"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("org.entur.ror.ubelluris.UbellurisApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.entur.netex.pipeline)
    implementation(libs.entur.netex.tools.lib)
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
    implementation(libs.logstash.logback.encoder)
    implementation(libs.google.cloud.storage)
    implementation(libs.jdom2)
    implementation(libs.jackson.databind)
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.inline)
    testImplementation(libs.mockito.kotlin)
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}

kotlin {
    jvmToolchain(21)
}
