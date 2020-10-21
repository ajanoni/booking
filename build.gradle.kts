plugins {
    java
    id("io.quarkus")
    id("io.freefair.lombok") version "5.2.1"
}

repositories {
    mavenLocal()
    mavenCentral()
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    implementation("io.quarkus:quarkus-reactive-mysql-client")
    implementation("io.smallrye.reactive:smallrye-mutiny-vertx-mysql-client")
    implementation("io.quarkus:quarkus-vertx")
    implementation("io.quarkus:quarkus-hibernate-validator")
    implementation("io.quarkus:quarkus-smallrye-openapi")
    implementation("io.quarkus:quarkus-config-yaml")
    implementation("io.quarkus:quarkus-resteasy")
    implementation("io.quarkus:quarkus-resteasy-mutiny")
    implementation("io.quarkus:quarkus-resteasy-jackson")
    implementation("io.quarkus:quarkus-arc")

    implementation("com.google.guava:guava:29.0-jre")
    implementation("org.redisson:redisson:3.13.6")
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("org.signal:embedded-redis:0.8.1")

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.1.0")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("org.assertj:assertj-core:3.17.2")
    testImplementation("org.mockito:mockito-core:3.5.13")
    testImplementation("org.mockito:mockito-junit-jupiter:3.5.13")
    testImplementation("io.quarkus:quarkus-junit5-mockito:1.9.0.Final")

    implementation(platform( "org.testcontainers:testcontainers-bom:1.15.0-rc2"))
    testImplementation("org.testcontainers:mysql")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("mysql:mysql-connector-java:mysql-connector-java")

}

group = "com.ajanoni"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}
