//Импортируем задачи для генерации
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer

//Запихиваем все версии в одну переменную
val versions = mapOf(
    "personApiVersion" to "1.0.0-SNAPSHOT",
    "keycloakAdminClientVersion" to "22.0.3",
    "springdocOpenapiStarterWebfluxVersion" to "2.5.0",
    "mapstructVersion" to "1.5.5.Final",
    "javaxAnnotationApiVersion" to "1.3.2",
    "javaxValidationsApiVersion" to "2.0.0.Final",
    "logbackClassicVersion" to "1.5.18",
    "nettyResolverVersion" to "4.1.121.Final:osx-aarch_64",
    "feignMicrometerVersion" to "13.6",
    "testContainersVersion" to "1.19.3",
    "junitJupiterVersion" to "5.10.0",

    "swaggerAnnotationsVersion" to "2.2.44"
)

plugins {
    idea
    java
    id("org.springframework.boot") version "3.5.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.openapi.generator") version "7.13.0"
}

group = "org.rockend"
version = "1.0.0-SNAPSHOT"
description = "API Gateway for persons project"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.0.0")
        mavenBom("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom:2.15.0")
    }
}

/*
    Отключает кэш для changing-зависимостей.
    Это удобно для SNAPSHOT, но замедляет сборку.
    Использовать осознанно
 */
configurations.all { resolutionStrategy.cacheChangingModulesFor(0, "seconds") }

dependencies {
    implementation("org.rockend:person-api:${versions["personApiVersion"]}")


    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    // KEYCLOAK
    implementation("org.keycloak:keycloak-admin-client:${versions["keycloakAdminClientVersion"]}")

    // OBSERVABILITY
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.github.openfeign:feign-micrometer:${versions["feignMicrometerVersion"]}")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("io.micrometer:micrometer-observation")
    implementation("io.micrometer:micrometer-tracing")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    implementation("io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter")
    implementation("ch.qos.logback:logback-classic:${versions["logbackClassicVersion"]}")


    // HELPERS
    compileOnly("org.projectlombok:lombok")
    compileOnly("org.mapstruct:mapstruct:${versions["mapstructVersion"]}")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.mapstruct:mapstruct-processor:${versions["mapstructVersion"]}")
    implementation("javax.validation:validation-api:${versions["javaxValidationsApiVersion"]}")
    implementation("javax.annotation:javax.annotation-api:${versions["javaxAnnotationApiVersion"]}")
    implementation("io.netty:netty-resolver-dns-native-macos:${versions["nettyResolverVersion"]}")

    implementation("io.swagger.core.v3:swagger-annotations-jakarta:${versions["swaggerAnnotationsVersion"]}")

    // TEST
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.wiremock.integrations.testcontainers:wiremock-testcontainers-module:1.0-alpha-15")

    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testImplementation("org.junit.jupiter:junit-jupiter:${versions["junitJupiterVersion"]}")
    testImplementation("org.testcontainers:testcontainers:${versions["testContainersVersion"]}")
    testImplementation("org.testcontainers:postgresql:${versions["testContainersVersion"]}")
    testImplementation("org.testcontainers:junit-jupiter:${versions["testContainersVersion"]}")
}

tasks.withType<Test> {
    useJUnitPlatform()
}


/*
    ------------------------------------
    ========== API generation ==========
    ------------------------------------

    Конфигурируем процесс автогенерации DTO и клиентов на основе OpenAPI артефактов

    Автогенерация по всем спецификациям
    Поиск спецификаций в openapi/.yaml|yml, логируется список
    Для каждой задачи регистрируется задача Generate Task
*/

val openApiDir = file("${rootDir}/openapi")

val foundSpecifications = openApiDir.listFiles { f -> f.extension in listOf("yaml", "yml") } ?: emptyArray()
logger.lifecycle("Found ${foundSpecifications.size} specifications: " + foundSpecifications.joinToString { it.name })

foundSpecifications.forEach { specFile ->
    val ourDir = getAbsolutePath(specFile.nameWithoutExtension)
    val packageName = defineJavaPackageName(specFile.nameWithoutExtension)

    val taskName = buildGenerateApiTaskName(specFile.nameWithoutExtension)
    logger.lifecycle("Register task ${taskName} from ${ourDir.get()}")
    val basePackage = "org.rockend.${packageName}"

    /*
        Создаём генератор spring с библиотекой 'spring-cloud' -> feign-клиенты + модели
        Пакеты: api, dto, config внутри org.rockend.<имя_пакета>
    */
    tasks.register(taskName, GenerateTask::class) {
        generatorName.set("spring")
        inputSpec.set(specFile.absolutePath)
        outputDir.set(ourDir)


        /*
            Прописываем опции
            generateAllOpenApi - зависит от всех 'generate*' задач
            Компиляция Java зависит от generateAllOpenApi, добавляются сгенерированные srcDir в sourceSets.main
        */
        configOptions.set(
            mapOf(
                "library" to "spring-cloud",
                "skipDefaultInterface" to "true",
                "useBeanValidation" to "true",
                "openApiNullable" to "false",
                "useFeignClientUrl" to "true",
                "useTags" to "true",
                "apiPackage" to "${basePackage}.api",
                "modelPackage" to "${basePackage}.dto",
                "configPackage" to "${basePackage}.config"
            )
        )

        doFirst {
            logger.lifecycle("$taskName: starting generation from ${specFile.name}")
        }
    }
}


fun getAbsolutePath(nameWithoutExtension: String): Provider<String> {
    return layout.buildDirectory
        .dir("generated-sources/openapi/${nameWithoutExtension}")
        .map { it.asFile.absolutePath }
}

fun defineJavaPackageName(name: String): String {
    val beforeDash = name.substringBefore('-')
    val match = Regex("^[a-z]+]").find(beforeDash)

    //Если match null - возвращаем beforeDash.lowercase()
    return match?.value ?: beforeDash.lowercase()
}

fun buildGenerateApiTaskName(name: String): String {
    return buildTaskName("generate", name)
}

fun buildJarTaskName(name: String): String {
    return buildTaskName("jar", name)
}

fun buildTaskName(taskPrefix: String, name: String): String {
    val prepareName = name
        .split(Regex("[^A-Za-z0-9]"))
        .filter { it.isNotBlank() }
        .joinToString("") { it.replaceFirstChar(Char::uppercase) }

    return "${taskPrefix}-${prepareName}"
}


/*
    === Подключение сгенерированного кода к основной компиляции ===

    Добавляем папки сгенерированных исходников в sourceSets.main,
    чтобы compileJava видел их как обычные Java-файлы.
*/
val withoutExtensionNames = foundSpecifications.map { it.nameWithoutExtension }

sourceSets.named("main") {
    withoutExtensionNames.forEach { name ->
        java.srcDir(layout.buildDirectory.dir("generated-sources/openapi/$name/src/main/java"))
    }
}


/*
    === Агрегирующая задача generateAllOpenApi ===

    Эта задача зависит от всех generate-... задач.
    Удобна для ручного запуска генерации:
      ./gradlew generateAllOpenApi
*/
tasks.register("generateAllOpenApi") {
    foundSpecifications.forEach { specFile ->
        dependsOn(buildGenerateApiTaskName(specFile.nameWithoutExtension))
    }
    doLast {
        logger.lifecycle("generateAllOpenApi: all specifications has been generated")
    }
}


/*
    === Гарантия порядка выполнения ===

    compileJava зависит от generateAllOpenApi.

    Итоговый пайплайн:
      compileJava
          ↓
      generateAllOpenApi
          ↓
      generate-<Spec> (для каждой спецификации)

    Таким образом генерация всегда происходит до компиляции.
*/
tasks.named("compileJava") {
    dependsOn("generateAllOpenApi")
}

/*
    -----------------------------------------------
    ========== Resolve NEXUS credentials ==========
    -----------------------------------------------
*/

// Чтение переменных для NEXUS из .env файла
file(".env").takeIf { it.exists() }?.readLines()?.forEach {
    val (k, v) = it.split("=", limit = 2)
    System.setProperty(k.trim(), v.trim())
    logger.lifecycle("${k.trim()}=${v.trim()}")
}

//Получение параметров для Nexus, сохранённых в gradle properties
val nexusUrl = System.getenv("NEXUS_URL") ?: System.getProperty("NEXUS_URL")
val nexusUser = System.getenv("NEXUS_USERNAME") ?: System.getProperty("NEXUS_USERNAME")
val nexusPassword = System.getenv("NEXUS_PASSWORD") ?: System.getProperty("NEXUS_PASSWORD")

//Проверка на наличие параметров Nexus, необходимых для публикации в него библиотек
if (nexusUrl.isNullOrBlank() || nexusUser.isNullOrBlank() || nexusPassword.isNullOrBlank()) {
    throw GradleException("NEXUS details are not set. Create a .env file with correct properties: " +
            "NEXUS_URL, NEXUS_USERNAME, NEXUS_PASSWORD"
    )
}

repositories {
    mavenCentral()
    maven {

        // Имя репозитория влияет на названия publish-задач (…ToNexusRepository)
        name = "nexus"

        // URL Nexus-репозитория (обычно snapshots или releases)
        url = uri(nexusUrl)

        isAllowInsecureProtocol = true
        credentials {
            username = nexusUser
            password = nexusPassword
        }
    }
}
