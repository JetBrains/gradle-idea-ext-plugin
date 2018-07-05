package org.jetbrains.gradle.ext

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.ConfigurableFileCollection

class Artifacts implements MapConvertible {

  Project project
  List<RecursiveArtifact> artifacts = new ArrayList<>()

  Artifacts(Project project) {
    this.project = project
  }

  void artifact(String name, Action<RecursiveArtifact> action) {
    def newArtifact = new RecursiveArtifact(name, ArtifactType.ARTIFACT)
    action.execute(newArtifact)
    artifacts.add(newArtifact)
  }

  @Override
  Map<String, ?> toMap() {
    return ["artifacts": artifacts*.toMap()]
  }

  class Artifact extends RecursiveArtifact {
    Artifact(String name) {
      super(name, ArtifactType.ARTIFACT)
    }
  }

  class RecursiveArtifact extends TypedArtifact {
    String name
    List<TypedArtifact> children = new ArrayList<>()

    RecursiveArtifact(String name, ArtifactType type) {
      super(type)
      this.name = name
    }

    void directory(String name, Action<RecursiveArtifact> action) {
      RecursiveArtifact newDir = new RecursiveArtifact(name, ArtifactType.DIR)
      action.execute(newDir)
      children.add(newDir)
    }

    void archive(String name, Action<RecursiveArtifact> action) {
      RecursiveArtifact newArchive = new RecursiveArtifact(name, ArtifactType.ARCHIVE)
      action.execute(newArchive)
      children.add(newArchive)
    }

    void libraryFiles(Configuration configuration) {
      children.add(new LibraryFiles(configuration))
    }

    void moduleOutput(String moduleName) {
      children.add(new ModuleOutput(moduleName))
    }

    void moduleTestOutput(String moduleName) {
      children.add(new ModuleTestOutput(moduleName))
    }

    void moduleSrc(String moduleName) {
      children.add(new ModuleSrc(moduleName))
    }

    void artifact(String artifactName) {
      children.add(new ArtifactRef(artifactName))
    }

    void file(Object... files) {
      children.add(new FileCopy(project.files(files)))
    }

    void directoryContent(Object... dirs) {
      children.add(new DirCopy(project.files(dirs)))
    }

    void extractedDirectory(Object... archivePaths) {
      children.add(new ExtractedArchive(project.files(archivePaths)))
    }

    @Override
    Map<String, ?> toMap() {
      return super.toMap() << ["name": name, "children": children*.toMap()]
    }
  }
}

abstract class TypedArtifact implements MapConvertible {
  ArtifactType type
  TypedArtifact(ArtifactType type) {
    this.type = type
  }

  @Override
  Map<String, ?> toMap() {
    return ["type":type]
  }
}

class LibraryFiles extends TypedArtifact {
  Configuration configuration

  LibraryFiles(Configuration configuration) {
    super(ArtifactType.LIBRARY_FILES)
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
  ModuleBasedArtifact(String name, ArtifactType type) {
    super(type)
    moduleName = name
  }

  @Override
  Map<String, ?> toMap() {
    return super.toMap() << ["moduleName" : moduleName]
  }
}
class ModuleOutput extends ModuleBasedArtifact {
  ModuleOutput(String name) {
    super(name, ArtifactType.MODULE_OUTPUT)
  }
}

class ModuleTestOutput extends ModuleBasedArtifact {
  ModuleTestOutput(String name) {
    super(name, ArtifactType.MODULE_TEST_OUTPUT)
  }
}

class ModuleSrc extends ModuleBasedArtifact {
  ModuleSrc(String name) {
    super(name, ArtifactType.MODULE_SRC)
  }
}

class ArtifactRef extends TypedArtifact {
  String artifactName
  ArtifactRef(String name) {
    super(ArtifactType.ARTIFACT_REF)
    artifactName = name
  }
  @Override
  Map<String, ?> toMap() {
    return super.toMap() << ["artifactName" : artifactName]
  }
}

abstract class FileBasedArtifact extends TypedArtifact {
  ConfigurableFileCollection sources
  FileBasedArtifact(ConfigurableFileCollection sourceFiles, ArtifactType type) {
    super(type)
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
  FileCopy(ConfigurableFileCollection sourceFiles) {
    super(sourceFiles, ArtifactType.FILE)
  }

  @Override
  Set<File> filter(Set<File> sources) {
    return sources.findAll { it.isFile() }
  }
}

class DirCopy extends FileBasedArtifact {
  DirCopy(ConfigurableFileCollection sourceDirs) {
    super(sourceDirs, ArtifactType.DIR_CONTENT)
  }

  @Override
  Set<File> filter(Set<File> sources) {
    return sources.findAll { it.isDirectory() }
  }
}

class ExtractedArchive extends FileBasedArtifact {
  ExtractedArchive(ConfigurableFileCollection sourceArchives) {
    super(sourceArchives, ArtifactType.EXTRACTED_DIR)
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
