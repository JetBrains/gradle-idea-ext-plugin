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
  private IdeaCompilerConfiguration compilerConfig
  private GroovyCompilerConfiguration groovyCompilerConfig
  private CopyrightConfiguration copyrightConfig
  private PolymorphicDomainObjectContainer<RunConfiguration> runConfigurations
  private Project project
  private CodeStyleConfig codeStyle
  private GradleIDESettings gradleSettings
  private FrameworkDetectionExclusionSettings detectExclusions
  private NamedDomainObjectContainer<Inspection> inspections

  private Instantiator instantiator

  ProjectSettings(Project project) {
    this.instantiator = (project as ProjectInternal).services.get(Instantiator.class)
    def runConfigurations = instantiator.newInstance(DefaultPolymorphicDomainObjectContainer, RunConfiguration.class, this.instantiator,  new Namer<RunConfiguration>() {
      @Override
      String determineName(RunConfiguration runConfiguration) {
        return runConfiguration.name
      }
    })

    runConfigurations.registerFactory(Application) { String name -> project.objects.newInstance(Application, name, project) }
    runConfigurations.registerFactory(JUnit) { String name -> project.objects.newInstance(JUnit, name) }
    runConfigurations.registerFactory(Remote) { String name -> project.objects.newInstance(Remote, name) }

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

  IdeaCompilerConfiguration getCompiler() {
    if (compilerConfig == null) {
      compilerConfig = project.objects.newInstance(IdeaCompilerConfiguration)
    }
    return compilerConfig
  }

  void compiler(Action<IdeaCompilerConfiguration> action) {
    action.execute(getCompiler())
  }

  GroovyCompilerConfiguration getGroovyCompiler() {
    if (groovyCompilerConfig == null) {
      groovyCompilerConfig = project.objects.newInstance(GroovyCompilerConfiguration);
    }
    return groovyCompilerConfig
  }

  void groovyCompiler(Action<GroovyCompilerConfiguration> action) {
    action.execute(getGroovyCompiler())
  }

  CodeStyleConfig getCodeStyle() {
    if (codeStyle == null) {
      codeStyle = project.objects.newInstance(CodeStyleConfig)
    }
    return codeStyle
  }

  def codeStyle(Action<CodeStyleConfig> action) {
    action.execute(getCodeStyle())
  }

  NamedDomainObjectContainer<Inspection> getInspections() {
    if (inspections == null) {
      inspections = project.container(Inspection)
    }
    return inspections
  }

  def inspections(Action<NamedDomainObjectContainer<Inspection>> action) {
    action.execute(getInspections())
  }

  CopyrightConfiguration getCopyright() {
    if (copyrightConfig == null) {
      copyrightConfig = project.objects.newInstance(CopyrightConfiguration, project)
    }
    return copyrightConfig
  }

  def copyright(Action<CopyrightConfiguration> action) {
    action.execute(getCopyright())
  }

  PolymorphicDomainObjectContainer<RunConfiguration> getRunConfigurations() {
    return runConfigurations
  }

  def runConfigurations(Action<PolymorphicDomainObjectContainer<RunConfiguration>> action) {
    action.execute(runConfigurations)
  }

  GradleIDESettings getGradleSettings() {
    if (gradleSettings == null) {
      gradleSettings = project.objects.newInstance(GradleIDESettings)
    }
    return gradleSettings
  }

  def gradleSettings(Action<GradleIDESettings> action) {
    action.execute(getGradleSettings())
  }

  def doNotDetectFrameworks(String... ids) {
    if (detectExclusions == null) {
      detectExclusions = project.objects.newInstance(FrameworkDetectionExclusionSettings)
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
      map["codeStyle"] = codeStyle.toMap()
    }

    if (inspections != null) {
      map["inspections"] = inspections.asList().collect { it.toMap() }
    }

    if (copyrightConfig != null) {
      map["copyright"] = copyrightConfig.toMap()
    }

    if (!runConfigurations.isEmpty()) {
      map["runConfigurations"] = runConfigurations.asList().collect { it.toMap() }
    }

    if (gradleSettings != null) {
      map["gradleSettings"] = gradleSettings.toMap()
    }

    if (detectExclusions != null) {
      map["frameworkDetectionExcludes"] = detectExclusions.excludes
    }

    return JsonOutput.toJson(map)
  }
}

@CompileStatic
class ModuleSettings {
  final PolymorphicDomainObjectContainer<Facet> facets

  ModuleSettings(Project project) {
    Instantiator instantiator = (project as ProjectInternal).services.get(Instantiator.class);

    def facets = instantiator.newInstance(DefaultPolymorphicDomainObjectContainer, Facet.class, instantiator, new Namer<Facet>() {
      @Override
      String determineName(Facet facet) {
        return facet.name
      }
    })

    facets.registerFactory(SpringFacet) { String name -> project.objects.newInstance(SpringFacet, name, project) }
    this.facets = facets
  }

  def facets(Action<PolymorphicDomainObjectContainer<Facet>> action) {
    action.execute(facets)
  }

  @Override
  String toString() {
    def map = [:]
    if (!facets.isEmpty()) {
      map["facets"] = facets.asList().collect { it.toMap() }
    }
    return JsonOutput.toJson(map)
  }
}
