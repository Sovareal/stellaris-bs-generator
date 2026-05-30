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
    implementation("org.springframework.boot:spring-boot-starter-cache")
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

tasks.register("generateTypeScript") {
    group = "stellaris"
    description = "Generate ../frontend/src/types/empire-generated.ts from DTO source files"

    val dtoSourceDir = layout.projectDirectory.dir("src/main/java/com/stellaris/bsgenerator/dto")
    val outputFile = layout.projectDirectory.file("../frontend/src/types/empire-generated.ts")

    inputs.dir(dtoSourceDir)
    outputs.file(outputFile)

    doLast {
        fun splitComma(s: String): List<String> {
            val parts = mutableListOf<String>()
            var depth = 0
            val cur = StringBuilder()
            for (ch in s) {
                when {
                    ch == '<' -> { depth++; cur.append(ch) }
                    ch == '>' -> { depth--; cur.append(ch) }
                    ch == ',' && depth == 0 -> { parts.add(cur.toString().trim()); cur.clear() }
                    else -> cur.append(ch)
                }
            }
            val last = cur.toString().trim()
            if (last.isNotEmpty()) parts.add(last)
            return parts
        }

        fun mapType(raw: String, nullable: Boolean): String {
            val t = raw.trim()
            val base = when {
                t == "String" -> "string"
                t == "boolean" -> "boolean"
                t == "int" -> "number"
                t.startsWith("List<") && t.endsWith(">") ->
                    "${mapType(t.removePrefix("List<").dropLast(1).trim(), false)}[]"
                t.startsWith("Set<") && t.endsWith(">") ->
                    "${mapType(t.removePrefix("Set<").dropLast(1).trim(), false)}[]"
                t == "Map<String, Boolean>" -> "Record<string, boolean>"
                else -> t
            }
            return if (nullable) "$base | null" else base
        }

        fun parseRecord(src: String): Pair<String, List<Triple<String, String, Boolean>>>? {
            val m = Regex("public record (\\w+)\\(([^)]+)\\)").find(src) ?: return null
            val className = m.groupValues[1]
            val components = splitComma(m.groupValues[2]).mapNotNull { raw ->
                val s = raw.trim()
                if (s.isEmpty()) return@mapNotNull null
                val isNullable = s.contains("@Nullable")
                val noAnn = s.replace(Regex("@\\w+\\s*"), "").trim()
                val sp = noAnn.lastIndexOf(' ')
                if (sp < 0) return@mapNotNull null
                Triple(noAnn.substring(0, sp).trim(), noAnn.substring(sp + 1).trim(), isNullable)
            }
            return className to components
        }

        val skip = setOf("RerollRequest")
        val sb = StringBuilder()
        sb.appendLine("// AUTO-GENERATED -- do not edit manually")
        sb.appendLine("// Source: backend/src/main/java/com/stellaris/bsgenerator/dto/")
        sb.appendLine("// Regenerate: gradle :backend:generateTypeScript")
        sb.appendLine()

        dtoSourceDir.asFile.listFiles { f -> f.extension == "java" }
            ?.sortedBy { it.nameWithoutExtension }
            ?.forEach { javaFile ->
                val (className, components) = parseRecord(javaFile.readText()) ?: return@forEach
                if (className in skip) return@forEach
                sb.appendLine("export interface $className {")
                for ((jType, name, isNull) in components) {
                    sb.appendLine("  $name: ${mapType(jType, isNull)};")
                }
                sb.appendLine("}")
                sb.appendLine()
            }

        outputFile.asFile.writeText(sb.toString())
        logger.lifecycle("Generated ${outputFile.asFile}")
    }
}

tasks.named("bootJar") {
    dependsOn("generateTypeScript")
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
