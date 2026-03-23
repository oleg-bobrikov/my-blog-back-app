plugins {
    java
    id("org.springframework.boot") version "3.5.12"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "ru.yandex.practicum"
version = "0.0.1-SNAPSHOT"
description = "my-blog-back-app"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    runtimeOnly("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
}

/**
 * Мы создаём свою конфигурацию mockitoAgent.
 * В ней будут лежать только jar-файлы, которые нужны для javaagent.
 * Это нужно, чтобы не смешивать agent-зависимости с обычным кодом.
 */
val mockitoAgent by configurations.creating

dependencies {
    mockitoAgent("org.mockito:mockito-core") { isTransitive = false }
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("-XX:+EnableDynamicAgentLoading", "-Xshare:off", "-javaagent:${mockitoAgent.asPath}")
}

val fullIntegrationTest = tasks.register<Test>("fullIntegrationTest") {
    group = "verification"
    description = "Runs the full integration tests."

    testClassesDirs = tasks.named<Test>("test").get().testClassesDirs
    classpath = tasks.named<Test>("test").get().classpath

    useJUnitPlatform()
    jvmArgs("-XX:+EnableDynamicAgentLoading", "-Xshare:off", "-javaagent:${mockitoAgent.asPath}")

    include("**/*FullIntegrationTest*")

    mustRunAfter(tasks.named("test"))
}

tasks.named<Test>("test") {
    exclude("**/*FullIntegrationTest*")
}

tasks.named("check") {
    dependsOn(fullIntegrationTest)
}

val testReport = tasks.register<TestReport>("testReport") {
    group = "verification"
    description = "Generates a combined test report."
    destinationDirectory.set(layout.buildDirectory.dir("reports/all-tests"))
    testResults.from(tasks.named<Test>("test"), fullIntegrationTest)
}

tasks.withType<Test> {
    finalizedBy(testReport)
}
