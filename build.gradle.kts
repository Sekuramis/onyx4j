plugins {
    `java-library`
    `maven-publish`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    withSourcesJar()
    withJavadocJar()
}

group = "de.onyx4j"
version = "1.0.0-RC2"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.socket:socket.io-client:2.1.2")
    api("com.google.code.gson:gson:2.15.0")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.yaml:snakeyaml:2.7")
}

tasks.withType<Jar>().configureEach {
    from(rootProject.file("LICENSE")) {
        into("META-INF")
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release = 25
}

tasks.register<JavaExec>("liveSmoke") {
    group = "verification"
    description = "Opt-in read-only live Onyx integration test"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("de.onyx4j.test.LiveSmokeTest")
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(25))
    })
}

publishing {
    publications {
        create<MavenPublication>("onyx4j") {
            from(components["java"])

            groupId = "de.onyx4j"
            artifactId = "onyx4j"

            pom {
                name.set("Onyx4J")
                description.set("Java API for Onyx")
                url.set("https://onyx4j.de")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
            }
        }
    }

    repositories {
        maven {
            name = "Reposilite"
            url = uri(
                if (project.version.toString().endsWith("-SNAPSHOT"))
                    "https://repo.onyx4j.de/snapshots"
                else
                    "https://repo.onyx4j.de/releases"
            )

            credentials {
                username = providers.environmentVariable("REPOSILITE_USERNAME").orNull
                password = providers.environmentVariable("REPOSILITE_PASSWORD").orNull
            }

            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}
