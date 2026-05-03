plugins {
    kotlin("jvm") version "2.3.21"
    `java-library`
    `maven-publish`
}

group = "io.github.avasiaxx"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    api(kotlin("stdlib"))

    implementation("com.google.apis:google-api-services-sheets:v4-rev20230227-2.0.0")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.30.1")
    implementation("com.google.api-client:google-api-client:2.9.0")
    implementation("com.google.http-client:google-http-client-gson:1.45.3")
    implementation("org.xerial:sqlite-jdbc:3.51.3.0")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "sheets-to-sql"
        }
    }
}
