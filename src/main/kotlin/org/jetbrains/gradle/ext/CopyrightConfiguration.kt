package org.jetbrains.gradle.ext

import groovy.lang.Closure
import org.gradle.api.Project
import java.util.*


class CopyrightConfiguration(val project: Project) {
  var useDefault: String? = null
  var scopes: Map<String, String> = emptyMap()
  val profiles = project.container(CopyrightProfile::class.java)
  fun profiles(action: Closure<*>) {
    profiles.configure(action)
  }

  fun toMap(): Map<String, *> = HashMap<String, Any>().apply {
    useDefault?.let { put("useDefault", it)}
    put("scopes", scopes)
    put("profiles", profiles.asMap)
  }
}

class CopyrightProfile(val name: String) {
  var notice: String? = null
  var keyword: String? = null
  var allowReplaceRegexp: String? = null
}