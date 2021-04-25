// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    val compose_version by extra("1.0.0-beta02")
    repositories {
        maven("https://maven.aliyun.com/nexus/content/groups/public/" )
        maven( "https://maven.aliyun.com/nexus/content/repositories/google" )
        maven("https://maven.aliyun.com/nexus/content/repositories/gradle-plugin" )
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.0.0-alpha14")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.32")

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle.kts files
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}