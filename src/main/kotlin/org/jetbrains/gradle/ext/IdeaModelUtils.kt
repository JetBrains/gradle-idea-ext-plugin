package org.jetbrains.gradle.ext

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.the
import org.gradle.plugins.ide.idea.model.IdeaModule
import org.gradle.plugins.ide.idea.model.IdeaProject

val IdeaProject.settings: ProjectSettings
    get() = (this as ExtensionAware).the()

fun IdeaProject.settings(configure: ProjectSettings.() -> Unit) {
    configure(this.settings)
}

// compiler
val ProjectSettings.compiler: IdeaCompilerConfiguration
    get() = (this as ExtensionAware).the()

fun ProjectSettings.compiler(configure: IdeaCompilerConfiguration.() -> Unit) {
    configure(this.compiler)
}

//delegateActions
val ProjectSettings.delegateActions: ActionDelegationConfig
    get() = (this as ExtensionAware).the()

fun ProjectSettings.delegateActions(configure: ActionDelegationConfig.() -> Unit) {
    configure(this.delegateActions)
}
//taskTriggers
val ProjectSettings.taskTriggers: TaskTriggersConfig
    get() = (this as ExtensionAware).the()

fun ProjectSettings.taskTriggers(configure: TaskTriggersConfig.() -> Unit) {
    configure(this.taskTriggers)
}

//groovyCompiler
val ProjectSettings.groovyCompiler: GroovyCompilerConfiguration
    get() = (this as ExtensionAware).the()

fun ProjectSettings.groovyCompiler(configure: GroovyCompilerConfiguration.() -> Unit) {
    configure(this.groovyCompiler)
}

//copyright
val ProjectSettings.copyright: CopyrightConfiguration
    get() = (this as ExtensionAware).the()

fun ProjectSettings.copyright(configure: CopyrightConfiguration.() -> Unit) {
    configure(this.copyright)
}

//encodings
val ProjectSettings.encodings: EncodingConfiguration
    get() = (this as ExtensionAware).the()

fun ProjectSettings.encodings(configure: EncodingConfiguration.() -> Unit) {
    configure(this.encodings)
}

//runConfigurations
val ProjectSettings.runConfigurations: RunConfigurationContainer
    get() = (this as ExtensionAware).the()

fun ProjectSettings.runConfigurations(configure: RunConfigurationContainer.() -> Unit) {
    configure(this.runConfigurations)
}

//inspections
val ProjectSettings.inspections: NamedDomainObjectContainer<Inspection>
    get() = (this as ExtensionAware).the()

fun ProjectSettings.inspections(configure: NamedDomainObjectContainer<Inspection>.() -> Unit) {
    configure(this.inspections)
}

//ideArtifacts
val ProjectSettings.ideArtifacts: NamedDomainObjectContainer<TopLevelArtifact>
    get() = (this as ExtensionAware).the()

fun ProjectSettings.ideArtifacts(configure: NamedDomainObjectContainer<TopLevelArtifact>.() -> Unit) {
    configure(this.ideArtifacts)
}



val IdeaModule.settings: ModuleSettings
    get() = (this as ExtensionAware).the()
fun IdeaModule.settings(configure: ModuleSettings.() -> Unit) {
    configure(this.settings)
}

//packagePrefix
val ModuleSettings.packagePrefix: PackagePrefixContainer
    get() = (this as ExtensionAware).the()

fun ModuleSettings.packagePrefix(configure: PackagePrefixContainer.() -> Unit) {
    configure(this.packagePrefix)
}

//moduleType
val ModuleSettings.moduleType: ModuleTypesConfig
    get() = (this as ExtensionAware).the()

fun ModuleSettings.moduleType(configure: ModuleTypesConfig.() -> Unit) {
    configure(this.moduleType)
}

operator fun ModuleTypesConfig.set(sourceSet: SourceSet, typeName: String?) {
    this.putAt(sourceSet, typeName)
}

