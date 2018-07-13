package org.jetbrains.gradle.ext

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.gradle.ext.BuildIdeArtifact.DEFAULT_DESTINATION
import org.junit.Before
import org.junit.Test

class BuildIdeArtifactTest {

  lateinit var myProject: Project
  lateinit var ideArtifact: BuildIdeArtifact

  @Before
  fun setup() {
    myProject = ProjectBuilder.builder().build()
    ideArtifact = myProject.tasks.create("testBuild", BuildIdeArtifact::class.java)
  }

  @Test fun `test empty task does nothing`() {
    ideArtifact.createArtifact()
    assertFalse(myProject.layout.buildDirectory.dir(DEFAULT_DESTINATION).get().asFile.exists())
  }

  @Test fun `test empty artifact creates destination dir`() {
    val name = "myArt"
    ideArtifact.artifact = myProject.objects.newInstance(RecursiveArtifact::class.java, myProject, name, ArtifactType.ARTIFACT)

    ideArtifact.createArtifact()

    val target = myProject.layout.buildDirectory.dir(DEFAULT_DESTINATION).get().dir(name).asFile

    assertThat(target)
            .exists()
            .isDirectory()
  }

  @Test fun `test single file copy`() {
    val name = "myArt"
    val fileName = "some.txt"
    val root = myProject.objects.newInstance(RecursiveArtifact::class.java, myProject, name, ArtifactType.ARTIFACT)
    root.file(fileName)
    ideArtifact.artifact = root

    val file = myProject.layout.projectDirectory.file(fileName).asFile
    val payload = "Payload"
    file.writeText(payload)

    ideArtifact.createArtifact()

    val target = myProject.layout.buildDirectory
            .dir(DEFAULT_DESTINATION).get()
            .dir(name)
            .dir(fileName)
            .asFile

    assertThat(target)
            .exists()
            .hasContent(payload)
  }

  @Test fun `test subdirectories copy`() {
    val name = "myArt"
    val fileName = "some.txt"
    val d1 = "dir1"
    val d2 = "dir2"
    val root = myProject.objects.newInstance(RecursiveArtifact::class.java, myProject, name, ArtifactType.ARTIFACT)
    root.directory(d1) {
      it.directory(d2) { sub ->
        sub.file(fileName)
      }
    }

    ideArtifact.artifact = root

    val file = myProject.layout.projectDirectory.file(fileName).asFile
    val payload = "Payload"
    file.writeText(payload)

    ideArtifact.createArtifact()

    val target = myProject.layout.buildDirectory
            .dir(DEFAULT_DESTINATION).get()
            .dir(name)
            .dir(d1)
            .dir(d2)
            .dir(fileName)
            .asFile

    assertThat(target)
            .exists()
            .hasContent(payload)
  }

  @Test fun `test archives copy`() {
    val name = "myArt"
    val fileName1 = "some1.txt"
    val fileName2 = "some2.txt"
    val topArchName = "arch.jar"
    val d1 = "dir1"
    val archName = "my.zip"
    val d2 = "dir2"
    val root = myProject.objects.newInstance(RecursiveArtifact::class.java, myProject, name, ArtifactType.ARTIFACT)

    root.archive(topArchName) { topArch ->
      topArch.directory(d1) { sub ->
        sub.archive(archName) { arch ->
          arch.file(fileName1)
          arch.directory(d2) { sub1 ->
            sub1.file(fileName2)
          }
        }
      }
    }

    ideArtifact.artifact = root

    val payload = "Payload1"
    listOf(fileName1, fileName2).forEach {
      myProject.layout.projectDirectory.file(it).asFile.writeText(payload)
    }

    ideArtifact.createArtifact()

    val arch = myProject.layout.buildDirectory
            .dir(DEFAULT_DESTINATION).get()
            .dir(name)
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
    val artifactName = "myArt"

    val root = myProject.objects.newInstance(RecursiveArtifact::class.java, myProject, artifactName, ArtifactType.ARTIFACT)
    root.libraryFiles(myCfg)
    ideArtifact.artifact = root

    ideArtifact.createArtifact()

    val target = myProject.layout.buildDirectory
            .dir(DEFAULT_DESTINATION).get()
            .dir(artifactName).asFile

    val fileNames = target.listFiles().map { it.name }
    assertThat(fileNames).containsExactlyInAnyOrder("hamcrest-core-1.3.jar", "junit-4.12.jar")
  }
}