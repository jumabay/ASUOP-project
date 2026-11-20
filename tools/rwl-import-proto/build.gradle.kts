plugins {
    java
    application
}

group = "platform.rwl"
version = "0.1.0-proto"

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

    // Local automated checks / optional --db=h2 only. Not proof of Oracle compatibility.
    implementation("com.h2database:h2:2.3.232")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("platform.rwl.proto.RwlImportCli")
}

tasks.named<JavaExec>("run") {
    environment(System.getenv())
}
