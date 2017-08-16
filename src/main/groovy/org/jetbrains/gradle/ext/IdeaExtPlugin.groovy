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
      def projectSettings = (ideaModel.project as ExtensionAware).extensions.create("settings", ProjectSettings) as ProjectSettings
      projectSettings.runConfigurations = project.container(NamedSettings)
    }

    def moduleSettings = (ideaModel.module as ExtensionAware).extensions.create("settings", ModuleSettings) as ModuleSettings
    moduleSettings.facets = project.container(NamedSettings)
  }
}

class ProjectSettings {
  NestedExpando compiler
  NamedDomainObjectContainer<NamedSettings> runConfigurations

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
  Object getProperty(String property) {
    Object result = super.getProperty(property)
    if (result != null) {
      return result
    } else {
      throw new MissingPropertyException(property, NestedExpando)
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
