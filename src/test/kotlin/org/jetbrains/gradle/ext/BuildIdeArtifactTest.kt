package org.jetbrains.gradle.ext

import junit.framework.Assert.*
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
    assertTrue(target.exists())
    assertTrue(target.isDirectory)
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

    assertTrue(target.exists())
    assertEquals(payload, target.readText())
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

    assertTrue(target.exists())
    assertEquals(payload, target.readText())
  }
}