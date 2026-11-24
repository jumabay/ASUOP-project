dependencies {
    implementation(project(":modules:fleet"))
    implementation(project(":modules:dislocation"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.3")
    implementation("org.springframework:spring-context:6.2.6")
    implementation("org.springframework:spring-jdbc:6.2.6")
    compileOnly("org.springframework.modulith:spring-modulith-api:1.4.0")

    // P5-E dbtc-ro profile only (not pulled into synthetic default runtime of platform-app
    // unless that profile activates and app adds ojdbc — see platform-app).
    compileOnly("com.oracle.database.jdbc:ojdbc10:19.33.0.0")
    compileOnly("com.oracle.database.nls:orai18n:19.33.0.0")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Copy offline migration golden pack into test classpath (no Oracle).
tasks.named<ProcessResources>("processTestResources") {
    from("${rootProject.projectDir}/fixtures/migration/v1") {
        into("fixtures/migration/v1")
        exclude("_generate.ps1", "validate.ps1")
    }
}
