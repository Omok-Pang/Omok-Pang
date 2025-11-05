plugins {
    id("java")
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0" // JavaFX 런타임 옵션 자동 설정
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(org.gradle.jvm.toolchain.JavaLanguageVersion.of(21)) // 17도 가능
    }
}

dependencies {
    // WebSocket (Tyrus)
    implementation("org.glassfish.tyrus:tyrus-server:2.1.1")
    implementation("org.glassfish.tyrus:tyrus-client:2.1.1")

    // PostgreSQL + HikariCP
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("com.zaxxer:HikariCP:5.1.0")

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
}

application {
    mainClass.set("com.omokpang.App")
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml")
}
