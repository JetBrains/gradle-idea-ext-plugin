package org.jetbrains.gradle.ext

import groovy.json.JsonOutput
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.util.ConfigureUtil

class IdeaExtPlugin implements Plugin<Project> {
  void apply(Project p) {
    p.apply plugin: 'idea'
    extend(p)
  }

  def extend(Project project) {
    def ideaModel = project.extensions.findByName('idea') as ExtensionAware
    if (!ideaModel) { return }

    if (ideaModel.project) {
      (ideaModel.project as ExtensionAware).extensions.create("settings", ProjectSettings, project)
    }

    (ideaModel.module as ExtensionAware).extensions.create("settings", ModuleSettings, project)
  }
}

class ProjectSettings {
  NestedExpando compiler
  NamedDomainObjectContainer<NamedSettings> runConfigurations

  ProjectSettings(Project project) {
    runConfigurations = project.container(NamedSettings)
  }

  def compiler(final Closure configureClosure) {
    if (compiler == null) {
      compiler = new NestedExpando()
    }
    ConfigureUtil.configure(configureClosure, compiler)
  }

  def runConfigurations(final Closure configureClosure) {
    runConfigurations.configure(configureClosure)
  }

  String toString() {
    def map  = [:]
    if (compiler != null) {
      map["compiler"] = compiler
    }
    if (!runConfigurations.isEmpty()) {
      map["runConfigurations"] = runConfigurations.asMap
    }
    return JsonOutput.toJson(map)
  }
}

class ModuleSettings {
  NamedDomainObjectContainer<NamedSettings> facets

  ModuleSettings(Project project) {
    facets = project.container(NamedSettings)
  }

  def facets(final Closure configureClosure) {
    facets.configure(configureClosure)
  }

  @Override
  String toString() {
    def map = ["facets": facets.asMap]
    return JsonOutput.toJson(map)
  }
}


class NamedSettings extends NestedExpando {
  def name

  NamedSettings(String name) {
   this.name = name
  }
}
