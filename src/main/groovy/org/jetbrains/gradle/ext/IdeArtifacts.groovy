package org.jetbrains.gradle.ext

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Project
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Zip

import javax.inject.Inject

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

  abstract void buildTo(File destination)
}

abstract class RecursiveArtifact extends TypedArtifact {
  String name
  public final List<TypedArtifact> children = new ArrayList<>()

  @Inject
  RecursiveArtifact(Project project, String name, ArtifactType type) {
    super(project, type)
    this.name = name
  }

  void directory(String name, Action<RecursiveArtifact> action) {
    RecursiveArtifact newDir = project.objects.newInstance(DirectoryArtifact, project, name)
    action.execute(newDir)
    children.add(newDir)
  }

  void archive(String name, Action<RecursiveArtifact> action) {
    RecursiveArtifact newArchive = project.objects.newInstance(ArchiveArtifact, project, name)
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
    return super.toMap() << ["name": name, "children": children*.toMap() ]
  }
}

class TopLevelArtifact extends RecursiveArtifact {
  @Inject
  TopLevelArtifact(Project project, String name) {
    super(project, name, ArtifactType.ARTIFACT)
  }

  @Override
  void buildTo(File destination) {
    children.forEach {
      it.buildTo(destination)
    }
  }
}

class TopLevelArtifactFactory implements NamedDomainObjectFactory<TopLevelArtifact> {
  Project myProject

  TopLevelArtifactFactory(Project project) {
    myProject = project
  }

  @Override
  TopLevelArtifact create(String name) {
    return myProject.objects.newInstance(TopLevelArtifact, myProject, name)
  }
}

class DirectoryArtifact extends RecursiveArtifact {
  @Inject
  DirectoryArtifact(Project project, String name) {
    super(project, name, ArtifactType.DIR)
  }

  @Override
  void buildTo(File destination) {
    def newDir = new File(destination, name)
    newDir.mkdirs()
    children.forEach {
      it.buildTo(newDir)
    }
  }
}

class ArchiveArtifact extends RecursiveArtifact {
  @Inject
  ArchiveArtifact(Project project, String name) {
    super(project, name, ArtifactType.ARCHIVE)
  }

  @Override
  void buildTo(File destination) {
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

  @Override
  void buildTo(File destination) {
    ArtifactCollection artifacts = configuration.getIncoming().artifactView({
      it.lenient(true)
      //it.componentFilter(Specs.SATISFIES_ALL)
    }).getArtifacts()

    def libraries = artifacts.artifacts
            .findAll { it.id.componentIdentifier instanceof ModuleComponentIdentifier }
            .collect { it.file }

    project.copy {
      from libraries
      into destination
    }
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

  protected Project findProject() {
    def find = project.rootProject.allprojects.find { it.idea.module.name == moduleName }
    if (find == null) {
      project.logger.warn("Failed to find gradle project for with idea module name ${moduleName}")
    }
    return find
  }
}

class ModuleOutput extends ModuleBasedArtifact {
  ModuleOutput(Project project, String name) {
    super(project, name, ArtifactType.MODULE_OUTPUT)
  }

  @Override
  void buildTo(File destination) {
    def project = findProject()
    if (project == null) {
      return
    }
    def mainOutput = project.sourceSets["main"].output
    project.copy {
      from mainOutput.classesDirs + mainOutput.resourcesDir + mainOutput.dirs
      into destination
    }
  }
}

class ModuleTestOutput extends ModuleBasedArtifact {
  ModuleTestOutput(Project project, String name) {
    super(project, name, ArtifactType.MODULE_TEST_OUTPUT)
  }

  @Override
  void buildTo(File destination) {
    def project = findProject()
    if (project == null) {
      return
    }
    def testOutput = project.sourceSets["test"].output
    project.copy {
      from testOutput.classesDirs + testOutput.resourcesDir + testOutput.dirs
      into destination
    }
  }
}

class ModuleSrc extends ModuleBasedArtifact {
  ModuleSrc(Project project, String name) {
    super(project, name, ArtifactType.MODULE_SRC)
  }

  @Override
  void buildTo(File destination) {
    def project = findProject()
    if (project == null) {
      return
    }
    project.copy {
      from { project.sourceSets.collect { it.allSource } }
      into destination
    }
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

  @Override
  void buildTo(File destination) {
    def referredArtifact = project.idea.project.settings.ideArtifacts[artifactName]
    if (referredArtifact != null) {
      referredArtifact.buildTo(destination)
    }
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

class ExtractedArchive extends FileBasedArtifact {
  ExtractedArchive(Project project, ConfigurableFileCollection sourceArchives) {
    super(project, sourceArchives, ArtifactType.EXTRACTED_DIR)
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
      from filtered.collect { project.zipTree(it) }
      into destination
    }
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
  File outputDirectory = null

  @TaskAction
  void createArtifact() {
    if (artifact == null) {
      project.logger.warn("artifact not specified for task ${this.name}")
      return
    }

    File destination = createDestinationDir()
    artifact.buildTo(destination)
  }

  private File createDestinationDir() {
    File destination
    if (outputDirectory == null) {
      destination = project.layout.buildDirectory
              .dir(DEFAULT_DESTINATION).get()
              .dir(artifact.name)
              .asFile
    } else {
      destination = outputDirectory
    }
    destination.mkdirs()
    return destination
  }
}