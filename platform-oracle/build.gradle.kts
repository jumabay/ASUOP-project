dependencies {
    implementation(project(":modules:fleet"))
    implementation(project(":modules:dislocation"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.3")
    implementation("org.springframework:spring-context:6.2.6")
    compileOnly("org.springframework.modulith:spring-modulith-api:1.4.0")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
