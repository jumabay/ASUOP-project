plugins {
    java
    application
}

group = "platform.oracle.smoke"
version = "0.1.0-smoke"

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
    // ADR-0003 (2026-11-14): JDBC 19c on Maven Central = ojdbc10, not ojdbc11.
    // Do not substitute ojdbc11:23.x.
    runtimeOnly(approvedJdbcCoordinate)
    // NLS / charset support (CL8MSWIN1251) — required with ojdbc10 against this DB.
    runtimeOnly(approvedNlsCoordinate)

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("platform.oracle.smoke.OracleSmokeCheck")
}

tasks.named<JavaExec>("run") {
    args = listOf("--dry-run")
    environment(System.getenv())
}

tasks.register("verifyApprovedDriver") {
    group = "verification"
    description = "Resolve ojdbc10 + orai18n 19.33.0.0 and refuse any 23.x jar on the runtime classpath."
    val runtimeCp = configurations.runtimeClasspath
    dependsOn(runtimeCp)
    doLast {
        val files = runtimeCp.get().files
        files.filter { it.name.startsWith("ojdbc") || it.name.startsWith("orai18n") }
            .forEach { f -> logger.lifecycle("Resolved jar: ${f.name} (${f.length()} bytes)") }

        val ojdbc = files.filter { it.name.startsWith("ojdbc") }
        if (ojdbc.isEmpty()) {
            throw GradleException("Approved driver not resolved: $approvedJdbcCoordinate")
        }
        ojdbc.forEach { f ->
            if (f.name.contains("23.") || f.name.contains("ojdbc11")) {
                throw GradleException("Forbidden JDBC jar on classpath: ${f.name}")
            }
            if (!f.name.contains("ojdbc10") || !f.name.contains("19.33.0.0")) {
                throw GradleException("Unexpected JDBC jar (want ojdbc10-19.33.0.0*.jar): ${f.name}")
            }
        }

        val orai18n = files.filter { it.name.startsWith("orai18n") }
        if (orai18n.isEmpty()) {
            throw GradleException("Approved NLS jar not resolved: $approvedNlsCoordinate")
        }
        orai18n.forEach { f ->
            if (!f.name.contains("19.33.0.0")) {
                throw GradleException("Unexpected orai18n jar (want 19.33.0.0): ${f.name}")
            }
        }

        logger.lifecycle("Driver/NLS check OK: $approvedJdbcCoordinate + $approvedNlsCoordinate")
    }
}
