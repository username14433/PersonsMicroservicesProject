import org.openapitools.generator.gradle.plugin.tasks.GenerateTask
import org.gradle.api.publish.maven.MavenPublication

//Выносим все версии в отдельный блок
val versions = mapOf(
    "mapstructVersion" to "1.5.5.Final",
    "springdocOpenapiStarterWebmvcUiVersion" to "2.5.0",
    "javaxAnnotationApiVersion" to "1.3.2",
    "javaxValidationApiVersion" to "2.0.0.Final",
    "comGoogleCodeFindbugs" to "3.0.2",
    "springCloudStarterOpenfeign" to "4.1.1",
    "javaxServletApiVersion" to "2.5",
    "logbackClassicVersion" to "1.5.18",
    "comGoogleCodeFindbugs" to "3.0.2",
    "springCloudStarterOpenfeign" to "4.1.1",
    "hibernateEnversVersion" to "6.4.4.Final",
    "testContainersVersion" to "1.19.3",
    "junitJupiterVersion" to "5.10.0",
    "feignMicrometerVersion" to "13.6"
)

plugins {
    idea
    java
    id("org.springframework.boot") version "3.5.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("maven-publish")
    id("org.openapi.generator") version "7.13.0"
}

group = "org.rockend"
version = "1.0.0-SNAPSHOT"
description = "Persons domain service for study project"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

repositories {
    mavenCentral()
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
    // SPRING
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${versions["springdocOpenapiStarterWebmvcUiVersion"]}")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign:${versions["springCloudStarterOpenfeign"]}")

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

    // PERSISTENCE
    implementation("org.hibernate.orm:hibernate-envers:${versions["hibernateEnversVersion"]}")
    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-database-postgresql")

    // HELPERS
    compileOnly("org.projectlombok:lombok")
    compileOnly("org.mapstruct:mapstruct:${versions["mapstructVersion"]}")
    compileOnly("com.google.code.findbugs:jsr305:${versions["comGoogleCodeFindbugs"]}")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.mapstruct:mapstruct-processor:${versions["mapstructVersion"]}")
    implementation("javax.validation:validation-api:${versions["javaxValidationApiVersion"]}")
    implementation("javax.annotation:javax.annotation-api:${versions["javaxAnnotationApiVersion"]}")

    // TEST
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
    -----------------------------------
    ========== Building jars ==========
    -----------------------------------

    Многоартефактная сборка (по спецификациям)
    Для каждой спецификации:
        - Создаётся отдельный 'Source Set' -> собственная компиляция (compile<Spec>Java)
        - Создаётся отдельная 'Jar' задача -> артефакт '<spec.jar>' в build/lib
    Задумка: собрать отдельные JAR'ы сгенерированного клиента/моделей для каждой спецификации.
*/


val generatedJars = foundSpecifications.map { specFile ->
    val name = specFile.nameWithoutExtension
    val generateTaskName = buildGenerateApiTaskName(name)
    val jarTaskName = buildJarTaskName(name)

    // Папка генерации берётся как Provider (лениво), чтобы Gradle мог корректно строить граф задач.
    // generateSrcDir указывает на src/main/java внутри директории генерации.
    val outDirProvider = getAbsolutePath(name)
    val generateSrcDir = outDirProvider.map { File(it).resolve("src/main/java") }

    val sourcesSetName = name

    /*
        Создаём отдельный sourceSet(“набор исходников” + связанные с ним classpath’ы, конфигурации, output и т.д.)
        под каждую спецификацию, чтобы компилировать её сгенерированные исходники отдельно.
        compileClasspath расширяем classpath'ом main,
        чтобы были доступны зависимости (Spring/Feign/Validation и т.д.).
    */
    val sourceSet = sourceSets.create(sourcesSetName) {
        java.srcDir(generateSrcDir)
        compileClasspath += sourceSets["main"].compileClasspath
    }

    // Регистрируем отдельную задачу компиляции Java для этого sourceSet.
    val compileTaskName = "compile${sourcesSetName.replaceFirstChar(Char::uppercase)}Java"
    tasks.register<JavaCompile>(compileTaskName) {
        source = sourceSet.java
        classpath = sourceSet.compileClasspath
        destinationDirectory.set(layout.buildDirectory.dir("classes/${sourcesSetName}"))
        dependsOn(generateTaskName)
    }

    // Собираем отдельный JAR из скомпилированных классов (не из исходников).
    tasks.register<Jar>(jarTaskName) {
        group = "build"
        archiveBaseName.set(name)
        destinationDirectory.set(layout.buildDirectory.dir("libs"))

        val classOutput = layout.buildDirectory.dir("classes/${sourcesSetName}")
        from(classOutput)
        dependsOn(compileTaskName)

        doFirst {
            println("Building JAR for $name from compiled classes in ${classOutput.get().asFile}")
        }
    }
}

// Делаем так, чтобы обычный ./gradlew build также собирал все JAR'ы, сгенерированные из OpenAPI-спек.
tasks.named("build") {
    dependsOn(generatedJars)
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


/*
    --------------------------------------
    ========== NEXUS Publishing ==========
    --------------------------------------

    Настройка публикации артефактов в Maven-репозиторий (Nexus) через плагин maven-publish.

    Идея:
      - для каждой OpenAPI-спеки ищем уже собранный jar/zip в build/libs
      - если файл найден — создаём MavenPublication с координатами groupId/artifactId/version
      - репозиторий публикации — Nexus (url + credentials из .env / env vars)
*/

publishing {
    publications {
        foundSpecifications.forEach { specFile ->
            val name = specFile.nameWithoutExtension
            val jarBaseName = name

            // Пытаемся найти готовый артефакт в build/libs.
            val jarFile = file("build/libs")
                .listFiles()
                ?.firstOrNull{ it.name.contains(name) && (it.extension == "jar" || it.extension == "zip") }

            // Публикация создаётся только если файл реально найден (иначе для этой спеки ничего не публикуется).
            if (jarFile != null) {
                logger.lifecycle("publishing: ${jarFile?.name}")

                create<MavenPublication>("publish${name.replaceFirstChar(Char::uppercase)}Jar") {

                    // Публикуем конкретный файл как артефакт (а не components["java"])
                    artifact(jarFile)

                    // Maven-координаты: по ним зависимость подключают другие проекты
                    groupId = "org.rockend"
                    artifactId = jarBaseName
                    version = "1.0.0-SNAPSHOT"

                    // Метаданные для POM (видно в Nexus)
                    pom {
                        this.name.set("Generated API $jarBaseName")
                        this.description.set("OpenAPI generated code for $jarBaseName")
                    }
                }
            }
        }
    }

    repositories {
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
}

