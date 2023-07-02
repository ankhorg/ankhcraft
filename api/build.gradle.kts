plugins {
    id("java")
}

dependencies {
    @Suppress("VulnerableLibrariesLocal") // We won't include it
    compileOnly("com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT")
    compileOnly("org.inksnow.ankh.core:api:1.1-73-SNAPSHOT")
}