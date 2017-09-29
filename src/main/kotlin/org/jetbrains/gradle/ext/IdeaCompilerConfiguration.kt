package org.jetbrains.gradle.ext

import java.util.*

/**
 * Created by Nikita.Skvortsov
 * date: 29.09.2017.
 */

open class IdeaCompilerConfiguration {
  var resourcePatterns: String? = null
  var processHeapSize: Int? = null
  var autoShowFirstErrorInEditor : Boolean? = null
  var displayNotificationPopup : Boolean? = null
  var clearOutputDirectory : Boolean? = null
  var addNotNullAssertions : Boolean? = null
  var enableAutomake : Boolean? = null
  var parallelCompilation : Boolean? = null
  var rebuildModuleOnDependencyChange : Boolean? = null
  var additionalVmOptions: String? = null

  fun toMap(): Map<String, *> = HashMap<String, Any>().apply {
    resourcePatterns?.let { put("resourcePatterns", it) }
    processHeapSize?.let { put("processHeapSize", it) }
    autoShowFirstErrorInEditor?.let { put("autoShowFirstErrorInEditor", it) }
    displayNotificationPopup?.let { put("displayNotificationPopup", it) }
    clearOutputDirectory?.let { put("clearOutputDirectory", it) }
    addNotNullAssertions?.let { put("addNotNullAssertions", it) }
    enableAutomake?.let { put("enableAutomake", it) }
    parallelCompilation?.let { put("parallelCompilation", it) }
    rebuildModuleOnDependencyChange?.let { put("rebuildModuleOnDependencyChange", it) }
    additionalVmOptions?.let { put("additionalVmOptions", it) }
  }
}