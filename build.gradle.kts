plugins {
  kotlin("jvm") version "1.8.10"
  id("maven-publish")
}

allprojects {
  if (project.buildscript.sourceFile?.exists() != true) {
    project.tasks.forEach { it.enabled = false }
    return@allprojects
  }

  apply(plugin = "java-library")
  apply(plugin = "maven-publish")

  if (rootProject == project) {
    group = "org.inksnow.ankh"
  } else {
    group = "org.inksnow.ankh.craft"
  }

  val buildNumber = System.getenv("BUILD_NUMBER")
  version = if (System.getenv("CI")?.isNotEmpty() == true && buildNumber != null) {
    "1.1-$buildNumber-SNAPSHOT"
  } else {
    "1.1-dev-SNAPSHOT"
  }

  repositories {
    mavenCentral()
    maven("https://r.irepo.space/maven/")
    maven("https://repo.inker.bot/repository/maven-snapshots/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
  }

  java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
    withJavadocJar()
  }


  afterEvaluate {
    publishing {
      repositories {
        if (System.getenv("CI")?.isNotEmpty() == true) {
          if (project.version.toString().endsWith("-SNAPSHOT")) {
            maven("https://repo.inker.bot/repository/maven-snapshots/") {
              credentials {
                username = System.getenv("NEXUS_USERNAME")
                password = System.getenv("NEXUS_PASSWORD")
              }
            }
          } else {
            maven("https://repo.inker.bot/repository/maven-releases/") {
              credentials {
                username = System.getenv("NEXUS_USERNAME")
                password = System.getenv("NEXUS_PASSWORD")
              }
            }
            maven("https://s0.blobs.inksnow.org/maven/") {
              credentials {
                username = System.getenv("REPO_USERNAME")
                password = System.getenv("REPO_PASSWORD")
              }
            }
          }
        } else {
          maven(rootProject.buildDir.resolve("publish"))
        }
      }

      publications {
        create<MavenPublication>("mavenJar") {
          artifactId = project.path
            .removePrefix(":")
            .replace(':', '-')
            .ifEmpty { "craft" }

          pom {
            name.set("AnkhCore ${project.name}")
            description.set("A bukkit plugin loader named AnkhCore")
            url.set("https://github.com/ankhorg/ankhcraft")
            properties.set(mapOf())
            licenses {
              license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
              }
            }
            developers {
              developer {
                id.set("inkerbot")
                name.set("InkerBot")
                email.set("im@inker.bot")
              }
            }
            scm {
              connection.set("scm:git:git://github.com/ankhorg/ankhcraft.git")
              developerConnection.set("scm:git:ssh://github.com/ankhorg/ankhcraft.git")
              url.set("https://github.com/ankhorg/ankhcraft")
            }
          }

          if(project.ext.has("publishAction")){
            (project.ext["publishAction"] as Action<MavenPublication>)(this)
          }else{
            from(components["java"])
          }
        }
      }
    }
  }
}

dependencies {
  implementation(project(":api"))
  @Suppress("VulnerableLibrariesLocal") // We won't include it
  compileOnly("com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT")
  compileOnly("org.inksnow.ankh:core:1.1-86-SNAPSHOT")

  // lombok
  compileOnly("org.projectlombok:lombok:1.18.26")
  annotationProcessor("org.projectlombok:lombok:1.18.26")
}


tasks.compileKotlin {
  kotlinOptions {
    jvmTarget = "11"
    freeCompilerArgs = listOf("-Xjvm-default=all")
  }
}