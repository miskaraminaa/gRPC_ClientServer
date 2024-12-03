// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("com.google.protobuf") version "0.9.4" apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.gradle)
        classpath(libs.protobuf.gradle.plugin)
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
