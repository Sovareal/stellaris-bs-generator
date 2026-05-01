plugins {
    java
    id("org.springframework.boot") version "4.0.2"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.stellaris"
version = "1.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.twelvemonkeys.imageio:imageio-dds:3.12.0")

    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")

    testCompileOnly("org.projectlombok:lombok:1.18.42")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.42")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

springBoot {
    buildInfo()
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("extractNamePool") {
    group = "stellaris"
    description = "One-off: extract name_lists from game files and merge into name_pool.json"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.stellaris.bsgenerator.namepool.extractor.NamePoolExtractorRunner")
    args = listOf(
        "--game-dir=F:/Games/SteamLibrary/steamapps/common/Stellaris",
        "--output=${projectDir}/src/main/resources/data/name_pool.json"
    )
}
