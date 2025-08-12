package org.jetbrains.gradle.ext

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.PolymorphicDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.reflect.TypeOf
import org.gradle.api.tasks.SourceSet
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.gradle.util.GradleVersion

import javax.inject.Inject

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
    if (ideaModel.project) {
      def projectSettings = (ideaModel.project as ExtensionAware).extensions.create("settings", ProjectSettings, project.objects, ideaFilesProcessor)

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

    container.add(RunConfigurationContainer, "runConfigurations", runConfigurations)
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

    if (TypeOf.typeOf(MapConvertible).isAssignableFrom(typeOfExt)) {
      def map = (extension as MapConvertible).toMap().findAll { it.value != null }
      return map.isEmpty() ? null : map
    }

    if (TypeOf.typeOf(Iterable).isAssignableFrom(typeOfExt)) {
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
  private ObjectFactory objectFactory
  private FrameworkDetectionExclusionSettings detectExclusions
  private IdeaFilesProcessor ideaFilesProcessor
  private Boolean generateImlFiles;
  @Inject
  ProjectSettings(ObjectFactory objectFactory, IdeaFilesProcessor processor) {
    this.objectFactory = objectFactory
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
      detectExclusions = objectFactory.newInstance(FrameworkDetectionExclusionSettings)
    }
    detectExclusions.excludes.addAll(ids)
  }

  boolean getGenerateImlFiles() {
    return generateImlFiles
  }

  void setGenerateImlFiles(boolean generateImlFiles) {
    this.generateImlFiles = generateImlFiles
  }

  String toString() {
    def map = collectExtensionsMap()

    if (detectExclusions != null) {
      map.put("frameworkDetectionExcludes", detectExclusions.excludes)
    }

    if (ideaFilesProcessor.hasPostprocessors()) {
      map.put("requiresPostprocessing", true)
    }

    boolean hasNestedPostprocessors = ideaFilesProcessor.anyHasProcessors()

    if (hasNestedPostprocessors) {
      map.put("requiresPostprocessing", true)
    }

    if (generateImlFiles != null) {
      map.put("generateImlFiles", generateImlFiles);
    }

    return new Gson().toJson(map)
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
    return new GsonBuilder()
            .disableHtmlEscaping()
            .serializeNulls()
            .create()
            .toJson(map)
  }
}
