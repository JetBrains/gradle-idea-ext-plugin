package org.jetbrains.gradle.ext

import groovy.json.JsonOutput
import org.gradle.api.*
import org.gradle.api.internal.DefaultPolymorphicDomainObjectContainer
import org.gradle.api.plugins.ExtensionAware
import org.gradle.internal.reflect.Instantiator
import org.gradle.util.ConfigureUtil
import org.jetbrains.gradle.ext.facets.Facet
import org.jetbrains.gradle.ext.facets.SpringFacet
import org.jetbrains.gradle.ext.runConfigurations.Application
import org.jetbrains.gradle.ext.runConfigurations.JUnit
import org.jetbrains.gradle.ext.runConfigurations.Remote
import org.jetbrains.gradle.ext.runConfigurations.RunConfiguration

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
  IdeaCompilerConfiguration compilerConfig
  GroovyCompilerConfiguration groovyCompilerConfig
  CopyrightConfiguration copyrightConfig
  PolymorphicDomainObjectContainer<RunConfiguration> runConfigurations
  Project project

  NestedExpando codeStyle
  NestedExpando inspections

  ProjectSettings(Project project) {
    Instantiator instantiator = project.getServices().get(Instantiator.class)
    runConfigurations = instantiator.newInstance(DefaultPolymorphicDomainObjectContainer, RunConfiguration.class, instantiator,  new Namer<RunConfiguration>() {
      @Override
      String determineName(RunConfiguration runConfiguration) {
        return runConfiguration.name
      }
    })

    runConfigurations.registerFactory(Application) { new Application(it, project) }
    runConfigurations.registerFactory(JUnit) { new JUnit(it) }
    runConfigurations.registerFactory(Remote) { new Remote(it) }

    runConfigurations.ext.defaults = { Class clazz, Closure configuration ->
      def aDefault = runConfigurations.maybeCreate("default_$clazz.name", clazz)
      aDefault.defaults = true
      ConfigureUtil.configure(configuration, aDefault)
    }
    this.project = project
  }

  void compiler(Action<IdeaCompilerConfiguration> action) {
    if (compilerConfig == null) {
      compilerConfig = project.objects.newInstance(IdeaCompilerConfiguration)
    }
    action.execute(compilerConfig)
  }

  void groovyCompiler(Action<GroovyCompilerConfiguration> action) {
    if (groovyCompilerConfig == null) {
      groovyCompilerConfig = project.objects.newInstance(GroovyCompilerConfiguration)
    }
    action.execute(groovyCompilerConfig)
  }

  def codeStyle(final Closure configureClosure) {
    if (codeStyle == null) {
      codeStyle = new NestedExpando()
    }
    ConfigureUtil.configure(configureClosure, codeStyle)
  }

  def inspections(final Closure configureClosure) {
    if (inspections == null) {
      inspections = new NestedExpando()
    }
    ConfigureUtil.configure(configureClosure, inspections)
  }

  def copyright(final Closure copyrightClosure) {
    if (copyrightConfig == null) {
      copyrightConfig = new CopyrightConfiguration(project)
    }
    ConfigureUtil.configure(copyrightClosure, copyrightConfig)
  }

  def runConfigurations(final Closure configureClosure) {
    runConfigurations.configure(configureClosure)
  }

  String toString() {
    def map  = [:]

    if (compilerConfig != null) {
      map["compiler"] = compilerConfig.toMap()
    }

    if (codeStyle != null) {
      map["codeStyle"] = codeStyle
    }

    if (inspections != null) {
      map["inspections"] = inspections
    }

    if (copyrightConfig != null) {
      map["copyright"] = copyrightConfig.toMap()
    }

    if (!runConfigurations.isEmpty()) {
      map["runConfigurations"] = runConfigurations.asList()
    }

    return JsonOutput.toJson(map)
  }
}

class ModuleSettings {
  PolymorphicDomainObjectContainer<Facet> facets

  ModuleSettings(Project project) {
    Instantiator instantiator = project.getServices().get(Instantiator.class);

    facets = instantiator.newInstance(DefaultPolymorphicDomainObjectContainer, Facet.class, instantiator, new Namer<Facet>() {
      @Override
      String determineName(Facet facet) {
        return facet.name
      }
    })

    facets.registerFactory(SpringFacet) { new SpringFacet(it, project) }
  }

  def facets(final Closure configureClosure) {
    facets.configure(configureClosure)
  }

  @Override
  String toString() {
    def map = [:]
    if (!facets.isEmpty()) {
      map["facets"] = facets.asList()
    }
    return JsonOutput.toJson(map)
  }
}


class NamedSettings extends NestedExpando {
  def name

  NamedSettings(String name) {
   this.name = name
  }
}
