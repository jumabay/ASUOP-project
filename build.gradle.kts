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

    tasks.named<Test>("test") {
        useJUnitPlatform {
            // T0 CI: exclude opt-in scale suite (P4-1 A). Run via testScale.
            excludeTags("scale")
        }
    }

    val javaExtension = extensions.getByType(JavaPluginExtension::class.java)
    val testSourceSet = javaExtension.sourceSets.getByName("test")
    tasks.register<Test>("testScale") {
        description = "Run @Tag(\"scale\") tests only (P4-1 synthetic 18k/30k; not Oracle proof)"
        group = "verification"
        dependsOn("testClasses")
        testClassesDirs = testSourceSet.output.classesDirs
        classpath = testSourceSet.runtimeClasspath
        useJUnitPlatform {
            includeTags("scale")
        }
        shouldRunAfter("test")
    }
}
