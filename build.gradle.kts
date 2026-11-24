import java.util.Locale
import java.util.zip.ZipFile

plugins {
    `java-library`
    `maven-publish`
    id("com.gradleup.shadow") version "9.4.3"
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
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.yaml:snakeyaml:2.7")
}

tasks.withType<Jar>().configureEach {
    from(rootProject.file("LICENSE")) {
        into("META-INF")
    }
}

shadow {
    addShadowVariantIntoJavaComponent = false
}

val dependencyNotices = layout.buildDirectory.dir("dependency-notices")
val runtimeLibraries = configurations.runtimeClasspath

val collectDependencyNotices by tasks.registering {
    inputs.files(runtimeLibraries)
    outputs.dir(dependencyNotices)
    doLast {
        val target = dependencyNotices.get().asFile
        target.deleteRecursively()
        target.mkdirs()

        val inventory = mutableListOf<String>()
        runtimeLibraries.get().files.filter { it.isFile && it.extension.equals("jar", true) }.sortedBy { it.name }.forEach { library ->
            val notices = mutableListOf<String>()
            val libraryDir = target.resolve(library.nameWithoutExtension)
            ZipFile(library).use { archive ->
                archive.entries().asSequence().filterNot { it.isDirectory }.forEach { entry ->
                    val normalized = entry.name.lowercase(Locale.ROOT)
                    val filename = normalized.substringAfterLast('/')
                    if (filename.startsWith("license") || filename.startsWith("licence") || filename.startsWith("notice") || filename.startsWith("copyright") || filename.startsWith("copying") || filename.startsWith("third-party") || filename.startsWith("third_party") || filename == "dependencies" || normalized.startsWith("meta-inf/licenses/")) {
                        val output = libraryDir.resolve(entry.name).normalize()
                        require(output.toPath().startsWith(libraryDir.toPath())) { "Invalid notice path in ${library.name}" }
                        output.parentFile.mkdirs()
                        archive.getInputStream(entry).use { input ->
                            output.outputStream().use { input.copyTo(it) }
                        }
                        notices.add(entry.name)
                    }
                }
            }
            inventory.add(library.name + ": " + if (notices.isEmpty()) "No embedded notices found; refer to upstream distribution and license metadata." else notices.sorted().joinToString(", "))
        }
        require(inventory.isNotEmpty()) { "No runtime dependencies resolved" }
        target.resolve("DEPENDENCIES.txt").writeText(inventory.joinToString("\n", postfix = "\n"), Charsets.UTF_8)
    }
}

tasks.shadowJar {
    archiveClassifier.set("withDependencies")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    filesMatching("META-INF/services/**") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
    mergeServiceFiles()
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/*.EC", "META-INF/INDEX.LIST", "module-info.class", "META-INF/versions/**/module-info.class")
    dependsOn(collectDependencyNotices)
    from(dependencyNotices.map { it.asFile }) {
        into("META-INF/third-party")
    }
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
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
