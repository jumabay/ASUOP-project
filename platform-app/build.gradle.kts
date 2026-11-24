plugins {
    id("org.springframework.boot") version "3.5.0"
    id("io.spring.dependency-management") version "1.1.7"
}

dependencies {
    implementation(project(":modules:fleet"))
    implementation(project(":modules:dislocation"))
    implementation(project(":platform-oracle"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.modulith:spring-modulith-starter-core")

    // Opt-in for profile dbtc-ro (P5-E time-boxed). Synthetic default does not open DataSource.
    runtimeOnly("com.oracle.database.jdbc:ojdbc10:19.33.0.0")
    runtimeOnly("com.oracle.database.nls:orai18n:19.33.0.0")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.modulith:spring-modulith-bom:1.4.0")
    }
}
