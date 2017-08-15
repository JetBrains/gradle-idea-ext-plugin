package org.jetbrains.gradle.ext

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware

class IdeaExtPlugin implements Plugin<Project> {
  void apply(Project p) {
    p.apply plugin: 'idea'
    extend(p)
  }

  def extend(Project project) {
    def ideaModel = project.extensions.findByName('idea') as ExtensionAware
    if (!ideaModel) { return }

    if (ideaModel.project) {
      (ideaModel.project as ExtensionAware).extensions.create("settings", DynamicSettings)
    }
    (ideaModel.module as ExtensionAware).extensions.create("settings", DynamicSettings)
  }
}
