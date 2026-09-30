plugins {
    java
}

group = "me.uc_hussein"
version = "1.1.0"

// Target: Paper 1.21.x on Java 21.
// Compiled against the oldest supported 1.21 API so it also runs on newer 1.21.x builds.
// Override if needed:  ./gradlew build -PpaperApi=1.21.8-R0.1-SNAPSHOT
val paperApi = providers.gradleProperty("paperApi").getOrElse("1.21.4-R0.1-SNAPSHOT")

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") { name = "papermc" }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApi")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    val props = mapOf("version" to project.version.toString(), "apiVersion" to "1.21")
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") { expand(props) }
}

tasks.jar {
    archiveFileName.set("ULTRAS_Chat_v1.jar")
}
