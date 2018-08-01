package org.jetbrains.gradle.ext

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Project

import javax.inject.Inject

@CompileStatic
class IdeaCompilerConfiguration implements MapConvertible {
    private final Project project
    String resourcePatterns
    Integer processHeapSize
    Boolean autoShowFirstErrorInEditor
    Boolean displayNotificationPopup
    Boolean clearOutputDirectory
    Boolean addNotNullAssertions
    Boolean enableAutomake
    Boolean parallelCompilation
    Boolean rebuildModuleOnDependencyChange
    String additionalVmOptions
    Boolean useReleaseOption
    JavacConfiguration javacConfig

    @Inject
    IdeaCompilerConfiguration(Project project) {
        this.project = project
    }

    JavacConfiguration getJavac() {
        if (javacConfig == null) {
            javacConfig = project.objects.newInstance(JavacConfiguration)
        }
        return javacConfig
    }

    void javac(Action<JavacConfiguration> action) {
        action.execute(getJavac())
    }

    @Override
    Map<String, ?> toMap() {
        Map<String, ?> map = [:]
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
        if (useReleaseOption != null) map.put("useReleaseOption", useReleaseOption)
        if (javacConfig != null) map.put("javacOptions", javacConfig.toMap())
        return map
    }
}

@CompileStatic
class JavacConfiguration {
    Boolean preferTargetJDKCompiler
    String javacAdditionalOptions
    Boolean generateDebugInfo
    Boolean generateDeprecationWarnings
    Boolean generateNoWarnings

    def toMap() {
        def map = [:]
        if (preferTargetJDKCompiler != null) map.put("preferTargetJDKCompiler", preferTargetJDKCompiler)
        if (javacAdditionalOptions != null) map.put("javacAdditionalOptions", javacAdditionalOptions)
        if (generateDebugInfo != null) map.put("generateDebugInfo", generateDebugInfo)
        if (generateDeprecationWarnings != null) map.put("generateDeprecationWarnings", generateDeprecationWarnings)
        if (generateNoWarnings != null) map.put("generateNoWarnings", generateNoWarnings)
        return map
    }
}