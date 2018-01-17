package org.jetbrains.gradle.ext

import groovy.lang.Closure
import org.gradle.internal.reflect.Instantiator
import org.gradle.util.ConfigureUtil

open class GroovyCompilerConfiguration(instantiator: Instantiator) {

  var heapSize: Int? = null
  var excludes: ExcludesConfig = instantiator.newInstance(ExcludesConfig::class.java)

  fun excludes(closure: Closure<*>) {
    ConfigureUtil.configure(closure, excludes)
  }

  fun toMap(): Map<String, Any?> = mapOf("heapSize" to heapSize.toString(), "excludes" to excludes.data)
}

open class ExcludesConfig {
  internal val data: MutableList<Map<String, Any>> = mutableListOf()

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