package org.jetbrains.gradle.ext.runConfigurations

/**
 * Created by Nikita.Skvortsov
 * date: 01.10.2017.
 */

open class RunConfiguration(val name: String, val type: String)


open class Application(name: String) : RunConfiguration(name, "application") {
  var mainClass: String? = null
  var workingDirectory: String? = null
  var jvmArgs: String? = null
}

open class JUnit(name: String): RunConfiguration(name, "junit") {
  var className: String? = null
}

