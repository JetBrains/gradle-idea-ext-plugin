# gradle-idea-ext-plugin

[![JetBrains team project](http://jb.gg/badges/team.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![Gradle Plugin Portal][gradle-plugin-badge]][gradle-plugin-page]

Plugin to store some IntelliJ IDEA settings in gradle script

### How to build

as simple as

    ./gradlew build

### How to apply

Apply from Gradle plugin repository

    plugins {
      id "org.jetbrains.gradle.plugin.idea-ext" version "1.4"
    }

Or build and drop resulting jar into root directory of a project and add following snippet to `build.gradle`

    buildscript {
      dependencies {
        classpath files('gradle-idea-ext.jar')
      }
    }
    apply plugin: 'org.jetbrains.gradle.plugin.idea-ext'

### How to use

See the [Wiki](https://github.com/JetBrains/gradle-idea-ext-plugin/wiki) for full DSL documentation


Version 1.0 requires IntelliJ IDEA 2020.3

[gradle-plugin-badge]: https://img.shields.io/gradle-plugin-portal/v/org.jetbrains.gradle.plugin.idea-ext?color=green&label=Gradle%20Plugin%20Portal&logo=gradle
[gradle-plugin-page]: https://plugins.gradle.org/plugin/org.jetbrains.gradle.plugin.idea-ext
