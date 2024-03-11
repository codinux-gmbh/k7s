plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")

    id("io.quarkus")
}


group = "net.dankito.kubernetes"
version = "1.0.0-SNAPSHOT"


kotlin {
    jvmToolchain(17)
}


repositories {
    mavenCentral()
}


val quarkusVersion: String by project

val kmpLogVersion: String by project
val lokiLogAppenderVersion: String by project

dependencies {
    implementation(enforcedPlatform("io.quarkus:quarkus-bom:$quarkusVersion"))

    implementation("io.quarkus:quarkus-kotlin")
    implementation("io.quarkus:quarkus-resteasy-reactive")
    implementation("io.quarkus:quarkus-resteasy-reactive-jackson")
    implementation("io.quarkus:quarkus-resteasy-reactive-qute")
    implementation("io.quarkus:quarkus-scheduler")

    implementation("io.quarkus:quarkus-kubernetes-client")
    implementation("org.bouncycastle:bcpkix-jdk18on")

    implementation("io.quarkus:quarkus-smallrye-health")
    implementation("io.quarkus:quarkus-micrometer-registry-prometheus")
    implementation("io.quarkus:quarkus-smallrye-openapi")

    implementation("net.codinux.log:kmp-log:$kmpLogVersion")
    implementation("net.codinux.log:quarkus-loki-log-appender:$lokiLogAppenderVersion")
    implementation("net.codinux.log.kubernetes:codinux-kubernetes-info-retriever:$lokiLogAppenderVersion")


    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
}


allOpen {
    annotation("jakarta.ws.rs.Path")
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("jakarta.persistence.Entity")
    annotation("io.quarkus.test.junit.QuarkusTest")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.javaParameters = true
}

tasks.withType<Test> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}