package org.jetbrains.gradle.ext

import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.tasks.SourceSet

import javax.inject.Inject

class ModuleTypesConfig implements MapConvertible {

    Project project
    Map<SourceSet, String> typesMap = new LinkedHashMap<>()
    String rootType
    private propertiesExtension

    @Inject
    ModuleTypesConfig(Project project, ExtensionContainer extensionContainer) {
        this.project = project
        propertiesExtension = extensionContainer.getByName("ext") as ExtraPropertiesExtension
        propertiesExtension.set("rootModuleType", "")
    }

    void putAt(SourceSet targetSourceSet, String moduleType) {
        if (!project.hasProperty('sourceSets')) {
            project.logger.warn("Can not register a module type [$moduleType] for a source set [$targetSourceSet] as no source sets are configured")
            return
        }
        if (!project.sourceSets.contains(targetSourceSet)) {
            project.logger.warn("Attempt to set a module type [$moduleType] for a source set [$targetSourceSet] that does not belong to current project")
            return
        }
        typesMap[targetSourceSet] = moduleType
    }

    String getAt(SourceSet s) {
        return typesMap[s]
    }

    @Override
    Map<String, ?> toMap() {

        Map<String, ?> result = [:]
        def rootType = propertiesExtension.get("rootModuleType")
        if (rootType != null && "" != rootType) {
            result << ["": rootType]
        }
        result << typesMap.collectEntries {
            [it.key.name, it.value]
        }

        return result
    }
}
