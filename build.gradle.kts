import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.gradleup.shadow") version "8.3.5"
    `maven-publish`
}

group = "com.alkacode"
version = "1.0.8"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    // Vault e a unica dependencia "de rede" que o Core tem - abstrai a economia
    // sem depender de nenhum plugin Alka* (regra de ouro do Core). AlkaEconomy
    // ja registra um provider Vault (com.alkacode.economy.vault.VaultHook, COINS).
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("net.luckperms:api:5.4")
    // ProtocolLib nao tem coordenada Maven publica estavel - mesmo jar local ja
    // usado no AlkaEffects/AlkaEssentials, so compile-time (o jar real vem do
    // plugin instalado no servidor).
    compileOnly(files("libs/ProtocolLib.jar"))

    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.xerial:sqlite-jdbc:3.46.1.3")
    implementation("com.mysql:mysql-connector-j:8.4.0")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    // org.sqlite NAO pode ser relocado: a lib nativa (sqlitejdbc.dll/.so) exporta
    // simbolos JNI fixos com o nome original (Java_org_sqlite_core_NativeDB_...);
    // relocar a classe Java quebra esse binding e vira UnsatisfiedLinkError em
    // runtime (ja aconteceu no AlkaClasses - ver o historico do projeto).
    relocate("com.zaxxer.hikari", "com.alkacode.core.libs.hikari")
    relocate("com.mysql", "com.alkacode.core.libs.mysql")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.processResources {
    filteringCharset = "UTF-8"
    // sem isso, o Gradle nao percebe que so `version` mudou e reusa o plugin.yml
    // antigo do cache (processResources fica UP-TO-DATE incorretamente).
    inputs.property("version", project.version)
    filesMatching("plugin.yml") {

        expand("version" to project.version)

    }
}

// publica o jar "puro" (classes do plugin, sem as libs de banco relocadas do
// shadowJar) no repositorio Maven local, para os outros plugins Alka* consumirem
// a API (compileOnly) e o AlkaCore.jar de verdade (com libs) ir pro servidor.
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = "AlkaCore"
            version = project.version.toString()
            from(components["java"])
        }
    }
}
