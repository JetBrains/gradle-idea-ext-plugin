package org.jetbrains.gradle.ext

import groovy.transform.CompileStatic

@CompileStatic
class IdeaCompilerConfiguration {

    String resourcePatterns
    Integer processHeapSize
    Boolean autoShowFirstErrorInEditor
    Boolean displayNotificationPopup
    Boolean clearOutputDirectory
    Boolean addNotNullAssertions
    Boolean enableAutomake
    Boolean parallelCompilation
    Boolean rebuildModuleOnDependencyChange
    Boolean additionalVmOptions

    def toMap() {
        def map = [:]
        if (resourcePatterns) map.put("resourcePatterns", resourcePatterns)
        if (processHeapSize != null) map.put("processHeapSize", processHeapSize)
        if (autoShowFirstErrorInEditor != null) map.put("autoShowFirstErrorInEditor", autoShowFirstErrorInEditor)
        if (displayNotificationPopup != null) map.put("displayNotificationPopup", displayNotificationPopup)
        if (clearOutputDirectory != null) map.put("clearOutputDirectory", clearOutputDirectory)
        if (addNotNullAssertions != null) map.put("addNotNullAssertions", addNotNullAssertions)
        if (enableAutomake != null) map.put("enableAutomake", enableAutomake)
        if (parallelCompilation != null) map.put("parallelCompilation", parallelCompilation)
        if (rebuildModuleOnDependencyChange != null) map.put("rebuildModuleOnDependencyChange", rebuildModuleOnDependencyChange)
        if (additionalVmOptions != null) map.put("additionalVmOptions", additionalVmOptions)
        return map
    }
}
