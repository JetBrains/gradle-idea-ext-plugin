package org.jetbrains.gradle.ext

import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.the
import org.gradle.plugins.ide.idea.model.IdeaModel

val IdeaModel.settings: ProjectSettings
    get() = (this.project as ExtensionAware).the()

fun IdeaModel.settings(configure: ProjectSettings.() -> Unit) {
    configure(this.settings)
}

val ProjectSettings.compiler: IdeaCompilerConfiguration
    get() = (this as ExtensionAware).the()

fun ProjectSettings.compiler(configure: IdeaCompilerConfiguration.() -> Unit) {
    configure(this.compiler)
}
