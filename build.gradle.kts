plugins {
    java
}

allprojects {
    group = "platform"
    version = "0.1.0-r1"
}

subprojects {
    apply(plugin = "java")

    repositories {
        mavenCentral()
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
