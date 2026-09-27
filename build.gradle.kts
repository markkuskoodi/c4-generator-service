plugins {
    java
    application
}

group = "ee.markkuskoodi.c4generator"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("info.picocli:picocli:4.7.7")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.19.0")
    implementation("org.yaml:snakeyaml:2.4")
    implementation("org.apache.maven:maven-model:3.9.16")
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.3.0.202506031305-r")
    runtimeOnly("org.slf4j:slf4j-nop:2.0.17") // silence JGit's SLF4J lookup on the CLI

    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("com.structurizr:structurizr-dsl:4.1.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.encoding = "UTF-8"
}

application {
    mainClass.set("ee.markkuskoodi.c4generator.cli.Main")
}

tasks.test {
    useJUnitPlatform()
}