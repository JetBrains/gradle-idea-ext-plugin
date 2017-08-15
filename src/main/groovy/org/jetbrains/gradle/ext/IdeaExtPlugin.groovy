package org.jetbrains.gradle.ext

import groovy.json.JsonOutput
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.util.Configurable
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
      (ideaModel.project as ExtensionAware).extensions.create("settings", ProjectSettings)
    }
    def moduleSettings = (ideaModel.module as ExtensionAware).extensions.create("settings", ModuleSettings) as ModuleSettings
    moduleSettings.facets = project.container(NamedSettings)
  }
}

class ProjectSettings {

  NestedExpando compiler = new NestedExpando()

  def compiler(final Closure configureClosure) {
    ConfigureUtil.configure(configureClosure, compiler)
  }

  String toString() {
    def map  = ["compiler" : compiler]
    return JsonOutput.toJson(map)
  }
}

class ModuleSettings {
  NamedDomainObjectContainer<NamedSettings> facets

  def facets(final Closure configureClosure) {
    facets.configure(configureClosure)
  }

  @Override
  String toString() {
    def map = ["facets": facets.asMap]
    return JsonOutput.toJson(map)
  }
}


class NestedExpando extends Expando implements Configurable<NestedExpando>  {

  @Override
  NestedExpando configure(Closure closure) {
    def cloned = closure.clone()
    cloned.setDelegate(this)
    cloned.setResolveStrategy(Closure.DELEGATE_FIRST)
    cloned.call()
    return this
  }

  @Override
  Object invokeMethod(String name, Object args) {
    if (args instanceof Object[] && args.length == 1 && args[0] instanceof Closure) {
      def nested = new NestedExpando()
      ConfigureUtil.configure(args[0] as Closure, nested)
      setProperty(name, nested)
      return nested
    } else if (args instanceof Object[] && args.length == 1) {
      setProperty(name, args[0])
      return args[0]
    } else {
      setProperty(name, args)
      return args
    }
  }

  @Override
  protected Map createMap() {
    return new LinkedHashMap()
  }
}

class NamedSettings extends NestedExpando {
  def name

  NamedSettings(String name) {
   this.name = name
  }
}
