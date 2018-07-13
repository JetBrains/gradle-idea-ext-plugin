package org.jetbrains.gradle.ext

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Zip

import javax.inject.Inject

class IdeArtifacts implements MapConvertible {

  Project project
  List<RecursiveArtifact> artifacts = new ArrayList<>()

  @Inject
  IdeArtifacts(Project project) {
    this.project = project
  }

  void ideArtifact(String name, Action<RecursiveArtifact> action) {
    def newArtifact = project.objects.newInstance(RecursiveArtifact, project, name, ArtifactType.ARTIFACT)
    action.execute(newArtifact)
    artifacts.add(newArtifact)
  }

  RecursiveArtifact getAt(String name) {
    return artifacts.find { it.name == name }
  }

  @Override
  Map<String, ?> toMap() {
    return ["artifacts": artifacts*.toMap()]
  }
}

abstract class TypedArtifact implements MapConvertible {

  Project project
  ArtifactType type

  TypedArtifact(Project project, ArtifactType type) {
    this.type = type
    this.project = project
  }

  @Override
  Map<String, ?> toMap() {
    return ["type":type]
  }

  void buildTo(File destination) {}
}

class RecursiveArtifact extends TypedArtifact {
  String name
  public final List<TypedArtifact> children = new ArrayList<>()

  @Inject
  RecursiveArtifact(Project project, String name, ArtifactType type) {
    super(project, type)
    this.name = name
  }

  void directory(String name, Action<RecursiveArtifact> action) {
    RecursiveArtifact newDir = project.objects.newInstance(RecursiveArtifact, project, name, ArtifactType.DIR)
    action.execute(newDir)
    children.add(newDir)
  }

  void archive(String name, Action<RecursiveArtifact> action) {
    RecursiveArtifact newArchive = project.objects.newInstance(RecursiveArtifact, project, name, ArtifactType.ARCHIVE)
    action.execute(newArchive)
    children.add(newArchive)
  }

  void libraryFiles(Configuration configuration) {
    children.add(new LibraryFiles(project, configuration))
  }

  void moduleOutput(String moduleName) {
    children.add(new ModuleOutput(project, moduleName))
  }

  void moduleTestOutput(String moduleName) {
    children.add(new ModuleTestOutput(project, moduleName))
  }

  void moduleSrc(String moduleName) {
    children.add(new ModuleSrc(project, moduleName))
  }

  void artifact(String artifactName) {
    children.add(new ArtifactRef(project, artifactName))
  }

  void file(Object... files) {
    children.add(new FileCopy(project, project.files(files)))
  }

  void directoryContent(Object... dirs) {
    children.add(new DirCopy(project, project.files(dirs)))
  }

  void extractedDirectory(Object... archivePaths) {
    children.add(new ExtractedArchive(project, project.files(archivePaths)))
  }

  @Override
  Map<String, ?> toMap() {
    return super.toMap() << ["name": name, "children": children*.toMap()]
  }

  @Override
  void buildTo(File destination) {
    if (type == ArtifactType.DIR) {
      def newDir = new File(destination, name)
      newDir.mkdirs()
      children.forEach {
        it.buildTo(newDir)
      }
    }

    if (type == ArtifactType.ARCHIVE) {
      def randomName = "archive_" + (new Random().nextInt())
      def temp = project.layout.buildDirectory.dir("tmp").get().dir(randomName).asFile
      temp.mkdirs()

      children.forEach {
        it.buildTo(temp)
      }

      def customArchName = name
      project.tasks.create(randomName, Zip) {
        from temp
        destinationDir destination
        archiveName customArchName
      }.execute()
    }
  }
}

class LibraryFiles extends TypedArtifact {
  Configuration configuration

  LibraryFiles(Project project, Configuration configuration) {
    super(project, ArtifactType.LIBRARY_FILES)
    this.configuration = configuration
  }

  @Override
  Map<String, ?> toMap() {
    ArtifactCollection artifacts = configuration.getIncoming().artifactView({
      it.lenient(true)
      //it.componentFilter(Specs.SATISFIES_ALL)
    }).getArtifacts()

    def libraries = artifacts.artifacts
            .collect { it.id.componentIdentifier }
            .findAll { it instanceof ModuleComponentIdentifier }
            .collect { ["group":it.group, "artifact": it.module, "version": it.version] }
    return super.toMap() << [ "libraries": libraries ]
  }
}

abstract class ModuleBasedArtifact extends TypedArtifact {
  String moduleName
  ModuleBasedArtifact(Project project, String name, ArtifactType type) {
    super(project, type)
    moduleName = name
  }

  @Override
  Map<String, ?> toMap() {
    return super.toMap() << ["moduleName" : moduleName]
  }
}
class ModuleOutput extends ModuleBasedArtifact {
  ModuleOutput(Project project, String name) {
    super(project, name, ArtifactType.MODULE_OUTPUT)
  }
}

class ModuleTestOutput extends ModuleBasedArtifact {
  ModuleTestOutput(Project project, String name) {
    super(project, name, ArtifactType.MODULE_TEST_OUTPUT)
  }
}

class ModuleSrc extends ModuleBasedArtifact {
  ModuleSrc(Project project, String name) {
    super(project, name, ArtifactType.MODULE_SRC)
  }
}

class ArtifactRef extends TypedArtifact {
  String artifactName
  ArtifactRef(Project project, String name) {
    super(project, ArtifactType.ARTIFACT_REF)
    artifactName = name
  }
  @Override
  Map<String, ?> toMap() {
    return super.toMap() << ["artifactName" : artifactName]
  }
}

abstract class FileBasedArtifact extends TypedArtifact {
  ConfigurableFileCollection sources
  FileBasedArtifact(Project project, ConfigurableFileCollection sourceFiles, ArtifactType type) {
    super(project, type)
    sources = sourceFiles
  }

  protected abstract Set<File> filter(Set<File> sources)

  @Override
  Map<String, ?> toMap() {
    def filteredFiles = filter(sources.files)
    return super.toMap() << ["sourceFiles": filteredFiles.collect { it.absolutePath.replace('\\' as char, '/' as char) }]
  }
}

class FileCopy extends FileBasedArtifact {
  FileCopy(Project project, ConfigurableFileCollection sourceFiles) {
    super(project, sourceFiles, ArtifactType.FILE)
  }

  @Override
  Set<File> filter(Set<File> sources) {
    return sources.findAll { it.isFile() }
  }

  @Override
  void buildTo(File destination) {
    if (!destination.isDirectory()) {
      return
    }

    def filtered = filter(sources.files)
    project.copy {
      from filtered
      into destination
    }
  }
}

class DirCopy extends FileBasedArtifact {
  DirCopy(Project project, ConfigurableFileCollection sourceDirs) {
    super(project, sourceDirs, ArtifactType.DIR_CONTENT)
  }

  @Override
  Set<File> filter(Set<File> sources) {
    return sources.findAll { it.isDirectory() }
  }
}

class ExtractedArchive extends FileBasedArtifact {
  ExtractedArchive(Project project, ConfigurableFileCollection sourceArchives) {
    super(project, sourceArchives, ArtifactType.EXTRACTED_DIR)
  }

  @Override
  Set<File> filter(Set<File> sources) {
    return sources.findAll { it.isFile() }
  }
}

enum ArtifactType {
  ARTIFACT,
  DIR, ARCHIVE,
  LIBRARY_FILES, MODULE_OUTPUT, MODULE_TEST_OUTPUT, MODULE_SRC, FILE, DIR_CONTENT, EXTRACTED_DIR,
  ARTIFACT_REF
}

class BuildIdeArtifact extends DefaultTask {
  public static final String DEFAULT_DESTINATION = "idea-artifacts"
  RecursiveArtifact artifact
  String outputDirectory

  @TaskAction
  void createArtifact() {
    if (artifact == null) {
      project.logger.warn("artifact not specified for task ${this.name}")
      return
    }

    File destination = createDestinationDir()

    artifact.children.forEach { child ->
      child.buildTo(destination)
    }
  }

  private File createDestinationDir() {
    def destination = project.layout.buildDirectory
            .dir(DEFAULT_DESTINATION).get()
            .dir(artifact.name)
            .asFile
    destination
            .mkdirs()
    destination
  }
}