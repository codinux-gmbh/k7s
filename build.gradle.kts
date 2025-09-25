import com.github.gradle.node.npm.task.NpxTask
import org.gradle.kotlin.dsl.support.serviceOf
import java.io.ByteArrayOutputStream

plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")

    id("io.quarkus")

    id("com.github.node-gradle.node") version "7.1.0"
}


group = "net.dankito.kubernetes"
version = "1.0.0-SNAPSHOT"


kotlin {
    jvmToolchain(17)

    compilerOptions {
        javaParameters = true
    }
}


repositories {
    mavenCentral()
}


val quarkusVersion: String by project

val klfVersion: String by project
val logFormatterVersion: String by project
val lokiLogAppenderVersion: String by project

dependencies {
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:$quarkusVersion"))

    implementation("io.quarkus:quarkus-kotlin")
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-qute")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.quarkus:quarkus-scheduler")
    implementation("io.quarkus:quarkus-cache")

    implementation("io.quarkus:quarkus-kubernetes-client")
    implementation("org.bouncycastle:bcpkix-jdk18on")

    implementation("io.quarkus:quarkus-smallrye-health")
    implementation("io.quarkus:quarkus-micrometer-registry-prometheus")
    implementation("io.quarkus:quarkus-smallrye-openapi")

    implementation("net.codinux.log:klf:$klfVersion")
    implementation("net.codinux.log:quarkus-log-formatter:$logFormatterVersion")
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

tasks.withType<Test> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}


val isBunInstalled = isProgramInstalled("bun")

// com.github.node-gradle.node plugin's NpmInstallTask did not work as it only supports package.json files in root source directory (honestly?)
val npmInstallFrontend = tasks.register<Exec>("npmInstallFrontend") {
    group = "frontend"
    description = "Run bun/npm install to install frontend dependencies if required. Ensures e.g. that vite is available for buildFrontend task"

    workingDir = File("$projectDir/frontend")

    commandLine(if (isBunInstalled) "bun" else "npm", "install")
}

val buildFrontendTask = tasks.register<NpxTask>("buildFrontend") {
    dependsOn(npmInstallFrontend)
    group = "frontend"
    description = "Transpiles Svelte components to standard JavaScript and CSS files and copies them to src/resources/META-INF/resources"

    workingDir.set(File("$projectDir/frontend"))

    command.set(if (isBunInstalled) "bun" else "npm")
    args.addAll("run", "buildQuarkusDist")
}

tasks.named("processResources") {
    dependsOn(buildFrontendTask)
}

fun isProgramInstalled(program: String, vararg args: String = arrayOf("--version")): Boolean {
    return try {
        val execOperations = project.serviceOf<ExecOperations>()
        val silencedOutputStream = ByteArrayOutputStream()
        execOperations.exec {
            commandLine = listOf(program) + args
            standardOutput = silencedOutputStream
            errorOutput = silencedOutputStream
        }.exitValue == 0
    } catch (e: Exception) {
        println("Running program '$program' failed: $e")
        false
    }
}