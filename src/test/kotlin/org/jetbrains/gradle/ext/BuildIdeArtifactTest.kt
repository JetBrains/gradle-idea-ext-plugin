package org.jetbrains.gradle.ext

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.internal.AbstractTask
import org.gradle.api.internal.plugins.DefaultConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.Zip
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.gradle.ext.BuildIdeArtifact.DEFAULT_DESTINATION
import org.junit.Before
import org.junit.Test
import java.io.File
import java.lang.IllegalStateException
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry
import java.io.BufferedOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.BufferedInputStream

const val artifactName = "myArt"

class BuildIdeArtifactTest {
  lateinit var myProject: Project
  lateinit var ideArtifact: BuildIdeArtifact

  @Before
  fun setup() {
    myProject = ProjectBuilder.builder().build()
    ideArtifact = myProject.tasks.create("testBuild", BuildIdeArtifact::class.java)
    ideArtifact.artifact = myProject.objects.newInstance(TopLevelArtifact::class.java, myProject, artifactName)
  }

  @Test fun `test empty task does nothing`() {
    ideArtifact.artifact = null // artifact was already set in setup
    ideArtifact.createArtifact()
    assertFalse(myProject.layout.buildDirectory.dir(DEFAULT_DESTINATION).get().asFile.exists())
  }

  @Test fun `test empty artifact creates default destination dir`() {
    ideArtifact.createArtifact()

    val target = myProject.layout.buildDirectory.dir(DEFAULT_DESTINATION).get().dir(artifactName).asFile

    assertThat(target)
            .exists()
            .isDirectory()
  }

  @Test fun `test single file copy`() {
    val fileName = "some.txt"

    ideArtifact.artifact.file(fileName)

    val file = myProject.layout.projectDirectory.file(fileName).asFile
    val payload = "Payload"
    file.writeText(payload)

    ideArtifact.createArtifact()

    val target = myProject.layout.buildDirectory
            .dir(DEFAULT_DESTINATION).get()
            .dir(artifactName)
            .file(fileName)
            .asFile

    assertThat(target)
            .exists()
            .hasContent(payload)
  }

  @Test fun `test custom destination directory`() {
    val fileName = "some.txt"
    val file = myProject.layout.projectDirectory.file(fileName).asFile
    val payload = "Payload"
    file.writeText(payload)

    val customOutput = createTempDir()
    try {
      ideArtifact.artifact.file(fileName)
      ideArtifact.outputDirectory = customOutput

      ideArtifact.createArtifact()

      assertThat(File(customOutput, fileName))
              .exists()
              .hasContent(payload)
    } finally {
      customOutput.deleteRecursively()
    }
  }

  @Test fun `test subdirectories copy`() {
    val fileName = "some.txt"
    val d1 = "dir1"
    val d2 = "dir2"
    ideArtifact.artifact.directory(d1) {
      it.directory(d2) { sub ->
        sub.file(fileName)
      }
    }

    val file = myProject.layout.projectDirectory.file(fileName).asFile
    val payload = "Payload"
    file.writeText(payload)

    ideArtifact.createArtifact()

    val target = myProject.layout.buildDirectory
            .dir(DEFAULT_DESTINATION).get()
            .dir(artifactName)
            .dir(d1)
            .dir(d2)
            .dir(fileName)
            .asFile

    assertThat(target)
            .exists()
            .hasContent(payload)
  }

  @Test fun `test archives copy`() {
    val fileName1 = "some1.txt"
    val fileName2 = "some2.txt"
    val topArchName = "arch.jar"
    val d1 = "dir1"
    val archName = "my.zip"
    val d2 = "dir2"

    ideArtifact.artifact.archive(topArchName) { topArch ->
      topArch.directory(d1) { sub ->
        sub.archive(archName) { arch ->
          arch.file(fileName1)
          arch.directory(d2) { sub1 ->
            sub1.file(fileName2)
          }
        }
      }
    }

    val payload = "Payload1"
    listOf(fileName1, fileName2).forEach {
      myProject.layout.projectDirectory.file(it).asFile.writeText(payload)
    }

    ideArtifact.createArtifact()

    val arch = myProject.layout.buildDirectory
            .dir(DEFAULT_DESTINATION).get()
            .dir(artifactName)
            .file(topArchName)
            .asFile

    assertThat(arch)
            .exists()
            .hasName(topArchName)

    val topContent = myProject.zipTree(arch).files
    assertEquals(1, topContent.size)

    val extracted = topContent.iterator().next()
    assertThat(extracted)
            .hasName(archName)
    assertThat(extracted.parentFile).hasName(d1)

    val innerContent = ArrayList(myProject.zipTree(extracted).files)
    innerContent.sortBy { it.name }
    assertEquals(2, innerContent.size)

    assertThat(innerContent[0])
            .hasName(fileName1)
            .hasContent(payload)

    assertThat(innerContent[1])
            .hasName(fileName2)
            .hasContent(payload)
    assertThat(innerContent[1].parentFile).hasName(d2)
  }

  @Test fun `test libraries are copied`() {
    myProject.repositories.mavenLocal()
    val myCfg = myProject.configurations.create("myCfg")
    myProject.dependencies.add(myCfg.name, "junit:junit:4.12")

    ideArtifact.artifact.libraryFiles(myCfg)
    ideArtifact.createArtifact()

    val target = myProject.layout.buildDirectory
            .dir(DEFAULT_DESTINATION).get()
            .dir(artifactName).asFile

    val fileNames = target.listFiles().map { it.name }
    assertThat(fileNames).containsExactlyInAnyOrder("hamcrest-core-1.3.jar", "junit-4.12.jar")
  }

  @Test fun `test directory content copy`() {
    val dirName = "myDir"
    val fileName = "some.txt"
    val payload = "payload"

    val srcFile = myProject.layout.projectDirectory
            .dir(dirName)
            .file(fileName).asFile

    srcFile.parentFile.mkdir()
    srcFile.writeText(payload)

    ideArtifact.artifact.directoryContent(dirName)
    ideArtifact.createArtifact()

    val target = myProject.layout.buildDirectory
            .dir(DEFAULT_DESTINATION).get()
            .dir(artifactName)
            .file(fileName).asFile

    assertThat(target).exists()
            .hasContent(payload)
  }

  @Test fun `test archive unpacking`() {
    val payload = "Payload ;)"
    val temp = createTempDir()
    val fileName = "some.text"

    try {
      val testFile = File(temp, fileName)
      testFile.writeText(payload)
      val testArchive = myProject.layout.projectDirectory.file("archive.zip").asFile
      createTestArchive(temp, testArchive)

      ideArtifact.artifact.extractedDirectory(testArchive)
      ideArtifact.createArtifact()

      val target = myProject.layout.buildDirectory
              .dir(DEFAULT_DESTINATION).get()
              .dir(artifactName)
              .file(fileName).asFile

      assertThat(target).exists()
              .hasContent(payload)

    } finally {
      temp.deleteRecursively()
    }
  }

  fun createTestArchive(sourceDir: File, destinationFile: File) {
    var out = ZipOutputStream(BufferedOutputStream(FileOutputStream(destinationFile)))

    sourceDir.walkBottomUp().forEach { file ->
      if(!file.isDirectory()) {
        var fi = FileInputStream(file)
        var origin = BufferedInputStream(fi)
        var entry = ZipEntry(file.absolutePath.removePrefix(sourceDir.absolutePath + "/"))
        out.putNextEntry(entry)
        origin.copyTo(out, 1024)
        origin.close()
      }
    }
    out.close()
  }

  @Test fun `test copy module sources`() {
    applyPluginIdea()
    createJavaSourceSetWithOutput()

    ideArtifact.artifact.moduleSrc(myProject.ideaModuleName)
    ideArtifact.createArtifact()

    val allSourceFiles = collectRelativePathsOfSourceFiles(myProject.sourceSets)

    val target = myProject.layout.buildDirectory
            .dir(DEFAULT_DESTINATION).get()
            .dir(artifactName).asFile

    assertThat(collectRelativeChildrenPaths(target).toList())
            .containsAll(allSourceFiles)
  }

  @Test fun `test copy module production classes`() {
    applyPluginIdea()
    createJavaSourceSetWithOutput()

    ideArtifact.artifact.moduleOutput(myProject.ideaModuleName)
    ideArtifact.createArtifact()

    val mainOutput = myProject.sourceSets.getByName("main").output
    val productionOutputRoots = mainOutput.classesDirs.files + mainOutput.dirs.files + mainOutput.resourcesDir
    val productionClassesPaths = productionOutputRoots.flatMap { collectRelativeChildrenPaths(it!!) }

    val target = myProject.layout.buildDirectory
            .dir(DEFAULT_DESTINATION).get()
            .dir(artifactName).asFile

    assertThat(collectRelativeChildrenPaths(target).toList())
            .hasSameSizeAs(productionClassesPaths)
            .containsAll(productionClassesPaths)
  }

  @Test fun `test copy module test classes`() {
    applyPluginIdea()
    createJavaSourceSetWithOutput()

    ideArtifact.artifact.moduleTestOutput(myProject.ideaModuleName)
    ideArtifact.createArtifact()

    val testOutput = myProject.sourceSets.getByName("test").output
    val testClassesRoots = testOutput.classesDirs.files + testOutput.dirs.files + testOutput.resourcesDir
    val testClassesPaths = testClassesRoots.flatMap { collectRelativeChildrenPaths(it!!) }

    val target = myProject.layout.buildDirectory
            .dir(DEFAULT_DESTINATION).get()
            .dir(artifactName).asFile

    assertThat(collectRelativeChildrenPaths(target).toList())
            .hasSameSizeAs(testClassesPaths)
            .containsAll(testClassesPaths)
  }

  private fun collectRelativePathsOfSourceFiles(sourceSets: NamedDomainObjectCollection<SourceSet>): List<String> {
    return sourceSets.flatMap { sourceSet ->
      sourceSet.allSource.srcDirs.flatMap { srcDir ->
        collectRelativeChildrenPaths(srcDir)
      }
    }.toList()
  }

  private fun collectRelativeChildrenPaths(srcDir: File) = srcDir.walk()
          .filter { it.isFile }
          .map {
            sourceFile -> sourceFile.toRelativeString(srcDir)
          }.asIterable()

  fun applyPluginIdea() {
    val param: MutableMap<String, String> = HashMap()
    param["plugin"] = "idea"
    myProject.apply(param)
  }

  fun createJavaSourceSetWithOutput() {
    val param: MutableMap<String, String> = HashMap()
    param["plugin"] = "java"
    myProject.apply(param)
    val sourceFile = myProject.layout.projectDirectory
            .dir("src")
            .dir("main")
            .dir("java")
            .dir("testRoot")
            .file("AClass.java")
            .asFile

    sourceFile.parentFile.mkdirs()
    sourceFile.writeText("""package testRoot;

      public class AClass {
        public static void main(String[] args) {
          System.out.println("Hello, World!");
        }
      }
      """)

    val resourceFile = myProject.layout.projectDirectory
            .dir("src")
            .dir("main")
            .dir("resources")
            .file("file.text")
            .asFile

    resourceFile.parentFile.mkdirs()
    resourceFile.writeText("payload")

    val testFile = myProject.layout.projectDirectory
            .dir("src")
            .dir("test")
            .dir("java")
            .dir("testRoot")
            .file("BClass.java")
            .asFile

    testFile.parentFile.mkdirs()
    testFile.writeText("""package testRoot;
      public class BClass {
        public static void main(String[] args) {
          System.out.println("Hello, Test World!");
        }
      }
      """)
  }
}

private val Project.ideaModuleName: String?
  get() {
    val ideaModel = this.extensions.getByName("idea") as IdeaModel
    return ideaModel.module.name
  }

private val Project.sourceSets: NamedDomainObjectCollection<SourceSet>
        get() {
  return (this.extensions as DefaultConvention)
          .extensionsAsDynamicObject.getProperty("sourceSets") as? NamedDomainObjectCollection<SourceSet>
          ?: throw IllegalStateException("Source sets are not available for this project. Check that 'java' plugin is applied")
}
