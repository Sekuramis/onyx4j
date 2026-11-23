plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    withSourcesJar()
    withJavadocJar()
}

group = "de.onyx4j"
version = "0.8.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.socket:socket.io-client:2.1.2")
    api("com.google.code.gson:gson:2.15.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.yaml:snakeyaml:2.7")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release = 25
}