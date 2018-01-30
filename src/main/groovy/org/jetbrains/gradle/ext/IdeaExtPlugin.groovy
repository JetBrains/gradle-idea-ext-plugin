package org.jetbrains.gradle.ext

import com.google.gson.Gson
import groovy.json.JsonOutput
import groovy.transform.CompileStatic
import org.gradle.api.*
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.reflect.TypeOf
import org.gradle.plugins.ide.idea.model.IdeaModel

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
abstract class AbstractExtensibleSettings {
  TypeOf mapConvertibleType = TypeOf.typeOf(MapConvertible)
  TypeOf iterableType = TypeOf.typeOf(Iterable)

  Map<String, ?> collectExtensionsMap() {
    def result = [:]
    if (this instanceof ExtensionAware) {
      def extContainer = (this as ExtensionAware).extensions
      extContainer.extensionsSchema.each { schema ->
        def name = schema.name
        def typeOfExt = schema.publicType

        def converted = convertToMapOrList(typeOfExt, extContainer.findByName(name))
        if (converted != null) {
          result.put(name, converted)
        }
      }
    }
    return result
  }

  def convertToMapOrList(TypeOf<?> typeOfExt, def extension) {
    if (extension == null) {
      return null
    }

    if (mapConvertibleType.isAssignableFrom(typeOfExt)) {
      return (extension as MapConvertible).toMap()
    }

    if (iterableType.isAssignableFrom(typeOfExt)) {
      def converted = (extension as Iterable)
              .findAll { it instanceof MapConvertible }
              .collect { (it as MapConvertible).toMap() }
      if (converted.size() > 0) {
        return converted
      } else {
        return null
      }
    }
  }
}

@CompileStatic
class ProjectSettings extends AbstractExtensibleSettings {
  private IdeaCompilerConfiguration compilerConfig
  private GroovyCompilerConfiguration groovyCompilerConfig
  private CopyrightConfiguration copyrightConfig
  private RunConfigurationContainer runConfigurations
  private Project project
  private CodeStyleConfig codeStyle
  private FrameworkDetectionExclusionSettings detectExclusions
  private NamedDomainObjectContainer<Inspection> inspections

  private Gson gson = new Gson()

  ProjectSettings(Project project) {
    def runConfigurations = GradleUtils.customPolymorphicContainer(project, DefaultRunConfigurationContainer)

    runConfigurations.registerFactory(Application) { String name -> project.objects.newInstance(Application, name, project) }
    runConfigurations.registerFactory(JUnit) { String name -> project.objects.newInstance(JUnit, name) }
    runConfigurations.registerFactory(Remote) { String name -> project.objects.newInstance(Remote, name) }

    this.runConfigurations = runConfigurations
    this.project = project
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

  def doNotDetectFrameworks(String... ids) {
    if (detectExclusions == null) {
      detectExclusions = project.objects.newInstance(FrameworkDetectionExclusionSettings)
    }
    detectExclusions.excludes.addAll(ids)
  }

  String toString() {
    def map = collectExtensionsMap()

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
      map["inspections"] = inspections.collect { it.toMap() }
    }

    if (copyrightConfig != null) {
      map["copyright"] = copyrightConfig.toMap()
    }

    if (!runConfigurations.isEmpty()) {
      map["runConfigurations"] = runConfigurations.collect { (it as RunConfiguration).toMap() }
    }

    if (detectExclusions != null) {
      map["frameworkDetectionExcludes"] = detectExclusions.excludes
    }

    return gson.toJson(map)
  }
}

@CompileStatic
class ModuleSettings extends AbstractExtensibleSettings {
  final PolymorphicDomainObjectContainer<Facet> facets

  ModuleSettings(Project project) {
    def facets = GradleUtils.polymorphicContainer(project, Facet)

    facets.registerFactory(SpringFacet) { String name -> project.objects.newInstance(SpringFacet, name, project) }
    this.facets = facets
  }

  def facets(Action<PolymorphicDomainObjectContainer<Facet>> action) {
    action.execute(facets)
  }

  @Override
  String toString() {
    def map = collectExtensionsMap()
    if (!facets.isEmpty()) {
      map["facets"] = facets.asList().collect { it.toMap() }
    }
    return JsonOutput.toJson(map)
  }
}
