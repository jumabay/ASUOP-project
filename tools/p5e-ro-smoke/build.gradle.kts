plugins {
    java
    application
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    runtimeOnly("com.oracle.database.jdbc:ojdbc10:19.33.0.0")
    runtimeOnly("com.oracle.database.nls:orai18n:19.33.0.0")
}

application {
    mainClass.set(providers.gradleProperty("probeMain").orElse("platform.oracle.p5e.P5eRoSmoke"))
}

tasks.named<JavaExec>("run") {
    environment(System.getenv())
}
