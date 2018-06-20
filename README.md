# gradle-idea-ext-plugin

[![JetBrains team project](http://jb.gg/badges/team.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

Proof-of-concept plugin to store arbitrary IJ settings in gradle script

### How to build

as simple as

    ./gradlew build

### How to apply

Apply from Gradle plugin repository

    plugins {
      id "org.jetbrains.gradle.plugin.idea-ext" version "0.2"
    }

Or build and drop resulting jar into root directory of a project and add following snippet to `build.grade`

    buildscript {
      dependencies {
        classpath files('gradle-idea-ext.jar')
      }
    }
    apply plugin: 'org.jetbrains.gradle.plugin.idea-ext'

### How to use

See the Wiki for documentation: [DSL of version 0.1](https://github.com/JetBrains/gradle-idea-ext-plugin/wiki/DSL-spec-v.-0.1) and [changes in version 0.2](https://github.com/JetBrains/gradle-idea-ext-plugin/wiki/Change-log-DSL-v0.2).

Version 0.1 requires IntelliJ IDEA 2018.1; version 0.2 requires IntelliJ IDEA 2018.2.
