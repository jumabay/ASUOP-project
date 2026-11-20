plugins {
    java
    application
}

group = "platform.oracle.dict"
version = "0.1.0-p3b"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

val approvedJdbcCoordinate = "com.oracle.database.jdbc:ojdbc10:19.33.0.0"
val approvedNlsCoordinate = "com.oracle.database.nls:orai18n:19.33.0.0"

dependencies {
    runtimeOnly(approvedJdbcCoordinate)
    runtimeOnly(approvedNlsCoordinate)

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("platform.oracle.dict.DictionaryInspectorMain")
}

tasks.named<JavaExec>("run") {
    environment(System.getenv())
}

tasks.register<JavaExec>("runP3c") {
    group = "application"
    description = "P3C-1 semantics probes (G0 + G31/RP1/WH1/KIT_FK; skips NK1/DT1/EXPLAIN)"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("platform.oracle.dict.P3cSemanticsMain")
    environment(System.getenv())
}
