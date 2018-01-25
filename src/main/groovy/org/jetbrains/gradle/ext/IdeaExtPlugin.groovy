package org.jetbrains.gradle.ext

import groovy.json.JsonOutput
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.*
import org.gradle.api.internal.DefaultPolymorphicDomainObjectContainer
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.ExtensionAware
import org.gradle.internal.reflect.Instantiator
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.gradle.util.ConfigureUtil
import org.jetbrains.gradle.ext.facets.Facet
import org.jetbrains.gradle.ext.facets.SpringFacet
import org.jetbrains.gradle.ext.runConfigurations.Application
import org.jetbrains.gradle.ext.runConfigurations.JUnit
import org.jetbrains.gradle.ext.runConfigurations.Remote
import org.jetbrains.gradle.ext.runConfigurations.RunConfiguration

@CompileStatic
class IdeaExtPlugin implements Plugin<Project> {
  void apply(Project p) {
    p.apply plugin: 'idea'
    extend(p)
  }

  def extend(Project project) {
    def ideaModel = project.extensions.findByName('idea') as IdeaModel
    if (!ideaModel) { return }

    if (ideaModel.project) {
      (ideaModel.project as ExtensionAware).extensions.create("settings", ProjectSettings, project)
    }

    (ideaModel.module as ExtensionAware).extensions.create("settings", ModuleSettings, project)
  }
}

@CompileStatic
class ProjectSettings {
  IdeaCompilerConfiguration compilerConfig
  GroovyCompilerConfiguration groovyCompilerConfig
  CopyrightConfiguration copyrightConfig
  PolymorphicDomainObjectContainer<RunConfiguration> runConfigurations
  Project project
  CodeStyleConfig codeStyle
  GradleIDESettings gradleSettings
  FrameworkDetectionExclusionSettings detectExclusions
  NamedDomainObjectContainer<Inspection> inspections

  private Instantiator instantiator

  ProjectSettings(Project project) {
    this.instantiator = (project as ProjectInternal).services.get(Instantiator.class)
    def runConfigurations = instantiator.newInstance(DefaultPolymorphicDomainObjectContainer, RunConfiguration.class, this.instantiator,  new Namer<RunConfiguration>() {
      @Override
      String determineName(RunConfiguration runConfiguration) {
        return runConfiguration.name
      }
    })

    runConfigurations.registerFactory(Application) { String name -> new Application(name, project) }
    runConfigurations.registerFactory(JUnit) { String name -> new JUnit(name) }
    runConfigurations.registerFactory(Remote) { String name -> new Remote(name) }

    installDefaultsExtraProperty(runConfigurations)
    this.runConfigurations = runConfigurations
    this.project = project
  }

  @CompileStatic(TypeCheckingMode.SKIP)
  private void installDefaultsExtraProperty(PolymorphicDomainObjectContainer<RunConfiguration> runConfigurations) {
    // TODO:pm Problematic magic, this won't play nice with the Kotlin DSL
    runConfigurations.ext.defaults = { Class clazz, Closure configuration ->
      def aDefault = runConfigurations.maybeCreate("default_$clazz.name", clazz)
      aDefault.defaults = true
      ConfigureUtil.configure(configuration, aDefault)
    }
  }

  void compiler(Action<IdeaCompilerConfiguration> action) {
    if (compilerConfig == null) {
      compilerConfig = project.objects.newInstance(IdeaCompilerConfiguration)
    }
    action.execute(compilerConfig)
  }

  void groovyCompiler(final Closure configureClosure) {
    if (groovyCompilerConfig == null) {
      groovyCompilerConfig = new GroovyCompilerConfiguration()
    }
    ConfigureUtil.configure(configureClosure, groovyCompilerConfig)
  }

  def codeStyle(final Closure configureClosure) {
    if (codeStyle == null) {
      codeStyle = new CodeStyleConfig()
    }
    ConfigureUtil.configure(configureClosure, codeStyle)
  }

  def inspections(final Closure configureClosure) {
    if (inspections == null) {
      inspections = project.container(Inspection)
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

  def gradleSettings(final Closure configureClosure) {
    if (gradleSettings == null) {
      gradleSettings = new GradleIDESettings()
    }
    ConfigureUtil.configure(configureClosure, gradleSettings)
  }

  def doNotDetectFrameworks(String... ids) {
    if (detectExclusions == null) {
      detectExclusions = new FrameworkDetectionExclusionSettings()
    }
    detectExclusions.excludes.addAll(ids)
  }

  String toString() {
    def map  = [:]

    if (compilerConfig != null) {
      map["compiler"] = compilerConfig.toMap()
    }


    if (groovyCompilerConfig != null) {
      map["groovyCompiler"] = groovyCompilerConfig.toMap()
    }

    if (codeStyle != null) {
      map["codeStyle"] = codeStyle
    }

    if (inspections != null) {
      map["inspections"] = inspections.asList()
    }

    if (copyrightConfig != null) {
      map["copyright"] = copyrightConfig.toMap()
    }

    if (!runConfigurations.isEmpty()) {
      map["runConfigurations"] = runConfigurations.asList()
    }

    if (gradleSettings != null) {
      map["gradleSettings"] = gradleSettings
    }

    if (detectExclusions != null) {
      map["frameworkDetectionExcludes"] = detectExclusions.excludes
    }

    return JsonOutput.toJson(map)
  }
}

@CompileStatic
class ModuleSettings {
  PolymorphicDomainObjectContainer<Facet> facets

  ModuleSettings(Project project) {
    Instantiator instantiator = (project as ProjectInternal).services.get(Instantiator.class);

    def facets = instantiator.newInstance(DefaultPolymorphicDomainObjectContainer, Facet.class, instantiator, new Namer<Facet>() {
      @Override
      String determineName(Facet facet) {
        return facet.name
      }
    })

    facets.registerFactory(SpringFacet) { String name -> new SpringFacet(name, project) }
    this.facets = facets
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
