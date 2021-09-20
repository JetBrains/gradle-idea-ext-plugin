package org.jetbrains.gradle.ext


import com.google.gson.Gson
import groovy.json.JsonOutput
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.PolymorphicDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.reflect.TypeOf
import org.gradle.api.tasks.SourceSet
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.gradle.util.GradleVersion

@CompileStatic
class IdeaExtPlugin implements Plugin<Project> {
  void apply(Project p) {
    p.apply plugin: 'idea'
    extend(p)
  }

  def extend(Project project) {
    def ideaModel = project.extensions.findByName('idea') as IdeaModel
    if (!ideaModel) { return }

    IdeaFilesProcessor ideaFilesProcessor = new IdeaFilesProcessor(project)
    project.tasks.create("processIdeaSettings", ProcessIdeaFilesTask, ideaFilesProcessor)
    if (ideaModel.project) {
      def projectSettings = (ideaModel.project as ExtensionAware).extensions.create("settings", ProjectSettings, project, ideaFilesProcessor)

      def settingsExt = (projectSettings as ExtensionAware).extensions

      settingsExt.create("delegateActions", ActionDelegationConfig)
      settingsExt.create("taskTriggers", TaskTriggersConfig, project)
      settingsExt.create("compiler", IdeaCompilerConfiguration, project)
      settingsExt.create("groovyCompiler", GroovyCompilerConfiguration)
      settingsExt.create("codeStyle", CodeStyleConfig)
      settingsExt.create("copyright", CopyrightConfiguration, project)
      settingsExt.create("encodings", EncodingConfiguration, project)
      addRunConfigurations(settingsExt, project)
      addInspections(settingsExt, project)
      addArtifacts(settingsExt, project)
    }

    def ideaModule = ideaModel.module
    if (ideaModule) {
      def moduleSettings = (ideaModel.module as ExtensionAware).extensions.create("settings", ModuleSettings, project, ideaFilesProcessor)

      def settingsExt = (moduleSettings as ExtensionAware).extensions

      settingsExt.create("packagePrefix", PackagePrefixContainer, ideaModule)
      settingsExt.create("moduleType", ModuleTypesConfig, project, settingsExt)
    }
  }

  static void addRunConfigurations(ExtensionContainer container, Project project) {
    RunConfigurationContainer runConfigurations = GradleUtils.runConfigurationsContainer(project)

    runConfigurations.registerFactory(Application) { String name -> project.objects.newInstance(Application, name, project) }
    runConfigurations.registerFactory(JUnit) { String name -> project.objects.newInstance(JUnit, name) }
    runConfigurations.registerFactory(Remote) { String name -> project.objects.newInstance(Remote, name) }
    runConfigurations.registerFactory(TestNG) { String name -> project.objects.newInstance(TestNG, name) }
    runConfigurations.registerFactory(Gradle) { String name -> project.objects.newInstance(Gradle, name) }
    runConfigurations.registerFactory(JarApplication) { String name -> project.objects.newInstance(JarApplication, name, project) }

    container.add("runConfigurations", runConfigurations)
  }
  static void addInspections(ExtensionContainer container, Project project) {
    def inspections = project.container(Inspection)
    container.add("inspections", inspections)
  }

  static void addArtifacts(ExtensionContainer container, Project project) {
    def artifacts = project.container(TopLevelArtifact, new TopLevelArtifactFactory(project))
    container.add("ideArtifacts", artifacts)
  }
}

abstract class AbstractExtensibleSettings {
  TypeOf mapConvertibleType = TypeOf.typeOf(MapConvertible)
  TypeOf iterableType = TypeOf.typeOf(Iterable)

  Map<String, Object> collectExtensionsMap() {
    def result = [:]
    if (this instanceof ExtensionAware) {
      def extContainer = (this as ExtensionAware).extensions
      if (GradleVersion.current() >= GradleVersion.version("4.5")) {
        extContainer.extensionsSchema.each { schema ->
          def name = schema.name
          def typeOfExt = schema.publicType

          def converted = convertToMapOrList(typeOfExt, extContainer.findByName(name))
          if (converted != null) {
            result.put(name, converted)
          }
        }
      } else {
        try {
          def schemaMethod = extContainer.class.getMethod("getSchema")
          Map<String, TypeOf> schema = schemaMethod.invoke(extContainer) as Map<String, TypeOf>
          schema.each { name, typeOfExt ->
            def converted = convertToMapOrList(typeOfExt, extContainer.findByName(name))
            if (converted != null) {
              result.put(name, converted)
            }
          }
        } catch (NoSuchMethodException e) {
          throw new GradleException("Can not collect extensions information in IDEA settings." +
                  " Please, use Gradle 4.2 or later.", e)
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
      def map = (extension as MapConvertible).toMap().findAll { it.value != null }
      return map.isEmpty() ? null : map
    }

    if (iterableType.isAssignableFrom(typeOfExt)) {
      def converted = (extension as Iterable)
              .findAll { it instanceof MapConvertible }
              .collect { (it as MapConvertible).toMap().findAll { it.value != null }}
              .findAll { !it.isEmpty() }

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
  private Project project
  private FrameworkDetectionExclusionSettings detectExclusions
  private IdeaFilesProcessor ideaFilesProcessor

  private Gson gson = new Gson()

  ProjectSettings(Project project, IdeaFilesProcessor processor) {
    this.project = project
    ideaFilesProcessor = processor
  }

  def withIDEADir(Action<File> action) {
    ideaFilesProcessor.withIDEAdir(action)
  }

  def withIDEAFileXml(String relativeFilePath, Action<XmlProvider> action) {
    ideaFilesProcessor.withIDEAFileXml(relativeFilePath, action)
  }

  def doNotDetectFrameworks(String... ids) {
    if (detectExclusions == null) {
      detectExclusions = project.objects.newInstance(FrameworkDetectionExclusionSettings)
    }
    detectExclusions.excludes.addAll(ids)
  }

  String toString() {
    def map = collectExtensionsMap()

    if (detectExclusions != null) {
      map.put("frameworkDetectionExcludes", detectExclusions.excludes)
    }

    if (ideaFilesProcessor.hasPostprocessors()) {
      map.put("requiresPostprocessing", true)
    }

    return gson.toJson(map)
  }
}


class ModuleSettings extends AbstractExtensibleSettings {
  final PolymorphicDomainObjectContainer<Facet> facets
  final IdeaFilesProcessor ideaFilesProcessor

  ModuleSettings(Project project, IdeaFilesProcessor processor) {
    def facets = GradleUtils.polymorphicContainer(project, Facet)

    facets.registerFactory(SpringFacet) { String name -> project.objects.newInstance(SpringFacet, name, project) }
    this.facets = facets
    ideaFilesProcessor = processor
  }

  def facets(Action<PolymorphicDomainObjectContainer<Facet>> action) {
    action.execute(facets)
  }

  def withModuleFile(SourceSet s, Action<File> action) {
    ideaFilesProcessor.withModuleFile(s, action)
  }

  def withModuleFile(Action<File> action) {
    ideaFilesProcessor.withModuleFile(null, action)
  }

  def withModuleXml(SourceSet s, Action<XmlProvider> action) {
    ideaFilesProcessor.withModuleXml(s, action)
  }

  def withModuleXml(Action<XmlProvider> action) {
    ideaFilesProcessor.withModuleXml(null, action)
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
