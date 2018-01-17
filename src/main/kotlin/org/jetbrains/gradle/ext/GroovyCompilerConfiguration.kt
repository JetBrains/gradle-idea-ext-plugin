package org.jetbrains.gradle.ext

import org.gradle.api.Action

class GroovyCompilerConfiguration {

  var heapSize: Int? = null

  val excludes: MutableList<Map<String, Any>> = mutableListOf()
  fun excludes(configure: Action<ExcludesConfig>) {
    configure.execute(ExcludesConfig(excludes))
  }
}

class ExcludesConfig(val data: MutableList<Map<String, Any>>) {
  fun file(path: String) {
    data.add(mapOf(
            "url" to path,
            "includeSubdirectories" to false,
            "isFile" to true
    ))
  }
  fun dir(path: String, includeSubdirectories: Boolean = false) {
    data.add(mapOf(
            "url" to path,
            "includeSubdirectories" to includeSubdirectories,
            "isFile" to false
    ))
  }
}