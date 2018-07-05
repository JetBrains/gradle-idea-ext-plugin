package org.jetbrains.gradle.ext

import groovy.json.JsonOutput
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

class SerializationTests {

  lateinit var myProject: Project

  @Before fun setup() {
    myProject = ProjectBuilder.builder().build()
  }

  @Test fun `test application json output`() {
    val application = Application("test", myProject)
    val make = application.beforeRun.create("make", Make::class.java)
    make.enabled = false

    assertEquals("""
    |{
    |    "type": "application",
    |    "envs": null,
    |    "workingDirectory": null,
    |    "mainClass": null,
    |    "moduleName": null,
    |    "beforeRun": [
    |        {
    |            "type": "make",
    |            "enabled": false
    |        }
    |    ],
    |    "jvmArgs": null,
    |    "defaults": false,
    |    "name": "test",
    |    "programParameters": null
    |}
    """.trimMargin(),
            JsonOutput.prettyPrint(JsonOutput.toJson(application.toMap())))
  }

  @Test fun `test remote json output`() {
    val remote = Remote("remote debug")
    remote.host = "hostname"
    remote.port = 1234
    remote.sharedMemoryAddress = "jvmdebug"

    assertEquals("""
    |{
    |    "type": "remote",
    |    "mode": "ATTACH",
    |    "port": 1234,
    |    "transport": "SOCKET",
    |    "host": "hostname",
    |    "defaults": false,
    |    "name": "remote debug",
    |    "sharedMemoryAddress": "jvmdebug"
    |}
    """.trimMargin(),
            JsonOutput.prettyPrint(JsonOutput.toJson(remote.toMap())))

  }

  @Test fun `test Groovy config output`() {
    val config = GroovyCompilerConfiguration()
    config.heapSize = 2049
    config.excludes(Action {
        it.file("C:/myFile.ext")
        it.dir("C:/myDir")
        it.dir("C:/recursiveDir", true)
    })

    assertEquals("""
      |{
      |    "heapSize": "2049",
      |    "excludes": [
      |        {
      |            "url": "C:/myFile.ext",
      |            "includeSubdirectories": false,
      |            "isFile": true
      |        },
      |        {
      |            "url": "C:/myDir",
      |            "includeSubdirectories": false,
      |            "isFile": false
      |        },
      |        {
      |            "url": "C:/recursiveDir",
      |            "includeSubdirectories": true,
      |            "isFile": false
      |        }
      |    ]
      |}
    """.trimMargin(),
            JsonOutput.prettyPrint(JsonOutput.toJson(config.toMap())))
  }

  @Test fun `test code style output`() {
    val config = CodeStyleConfig()

    config.java {
      it.ifForceBraces = ForceBraces.FORCE_BRACES_IF_MULTILINE
      it.classCountToUseImportOnDemand = 42
      it.forForceBraces = ForceBraces.FORCE_BRACES_ALWAYS
    }

    config.groovy {
      it.alignMultilineNamedArguments = true
      it.hardWrapAt = 99
    }

    assertEquals("""
    |{
    |    "USE_SAME_INDENTS": null,
    |    "RIGHT_MARGIN": null,
    |    "KEEP_CONTROL_STATEMENT_IN_ONE_LINE": null,
    |    "languages": {
    |        "java": {
    |            "RIGHT_MARGIN": null,
    |            "WRAP_COMMENTS": null,
    |            "IF_BRACE_FORCE": "FORCE_BRACES_IF_MULTILINE",
    |            "DOWHILE_BRACE_FORCE": null,
    |            "WHILE_BRACE_FORCE": null,
    |            "FOR_BRACE_FORCE": "FORCE_BRACES_ALWAYS",
    |            "KEEP_CONTROL_STATEMENT_IN_ONE_LINE": null,
    |            "CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND": 42,
    |            "JD_ALIGN_PARAM_COMMENTS": null,
    |            "JD_ALIGN_EXCEPTION_COMMENTS": null,
    |            "JD_P_AT_EMPTY_LINES": null,
    |            "JD_KEEP_EMPTY_PARAMETER": null,
    |            "JD_KEEP_EMPTY_EXCEPTION": null,
    |            "JD_KEEP_EMPTY_RETURN": null
    |        },
    |        "groovy": {
    |            "RIGHT_MARGIN": 99,
    |            "WRAP_COMMENTS": null,
    |            "IF_BRACE_FORCE": null,
    |            "DOWHILE_BRACE_FORCE": null,
    |            "WHILE_BRACE_FORCE": null,
    |            "FOR_BRACE_FORCE": null,
    |            "KEEP_CONTROL_STATEMENT_IN_ONE_LINE": null,
    |            "CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND": null,
    |            "ALIGN_NAMED_ARGS_IN_MAP": true
    |        }
    |    }
    |}
    """.trimMargin(),
            JsonOutput.prettyPrint(JsonOutput.toJson(config.toMap())))

  }

  @Test fun `test compiler options output`() {
    val config = IdeaCompilerConfiguration(myProject)

    config.apply {
      processHeapSize = 234
      addNotNullAssertions = true
      javac {
        it.preferTargetJDKCompiler = false
        it.javacAdditionalOptions = "-Xmaxwarns 999"
      }
      additionalVmOptions = "-Xms120"
      useReleaseOption = false
    }

    assertEquals("""
      |{
      |    "processHeapSize": 234,
      |    "addNotNullAssertions": true,
      |    "additionalVmOptions": "-Xms120",
      |    "useReleaseOption": false,
      |    "javacOptions": {
      |        "preferTargetJDKCompiler": false,
      |        "javacAdditionalOptions": "-Xmaxwarns 999"
      |    }
      |}
      """.trimMargin(),
            JsonOutput.prettyPrint(JsonOutput.toJson(config.toMap()))
    )
  }

  @Test fun `test task triggers output`() {
    val config = TaskTriggersConfig()

    myProject.tasks.create("task1")
    myProject.tasks.create("task2")
    myProject.tasks.create("task3")

    config.apply {
      beforeBuild(myProject.tasks.getByName("task1"), myProject.tasks.getByName("task2"))
      afterSync(myProject.tasks.getByName("task3"))
      beforeRebuild(myProject.tasks.getByName("task1"))
    }

    val escapedRootProjectPath = myProject.rootProject.projectDir.path.replace("\\", "/")

    assertEquals("""
      |{
      |    "beforeRebuild": [
      |        {
      |            "taskPath": ":task1",
      |            "projectPath": "$escapedRootProjectPath"
      |        }
      |    ],
      |    "beforeBuild": [
      |        {
      |            "taskPath": ":task1",
      |            "projectPath": "$escapedRootProjectPath"
      |        },
      |        {
      |            "taskPath": ":task2",
      |            "projectPath": "$escapedRootProjectPath"
      |        }
      |    ],
      |    "afterSync": [
      |        {
      |            "taskPath": ":task3",
      |            "projectPath": "$escapedRootProjectPath"
      |        }
      |    ]
      |}
    """.trimMargin(),
            JsonOutput.prettyPrint(JsonOutput.toJson(config.toMap()))
      )
  }

  @Test fun `test artifacts tree`() {
    val artifacts = Artifacts(myProject)

    val rootDir = myProject.rootDir

    val filePath = File(rootDir, "file.txt").apply {
      createNewFile()
      writeText("Some text")
    }.path.replace('\\', '/')

    val dirPath = File(rootDir, "dir").apply {
      mkdirs()
      File(this, "dir_file.txt").apply {
        createNewFile()
        writeText("Some text in subdirectory")
      }
    }.path.replace('\\', '/')

    val archivePath = File(rootDir, "my.zip")
            .apply { createNewFile() }
            .path.replace('\\', '/')

    val configurationName = "testCfg"
    val testCfg = myProject.configurations.create(configurationName)

    // as long as this junit is used for build itself, it can be used safely
    myProject.repositories.mavenLocal()
    myProject.dependencies.add(configurationName, "junit:junit:4.12")


    artifacts.apply {
      artifact("art1") {
        it.directory("dir1") {
          it.file(File("file.txt"))
          it.archive("arch1") {
            it.directoryContent("dir")
            it.libraryFiles(testCfg)
          }
        }
      }
      artifact("art2") {
        it.artifact("art1")
        it.extractedDirectory("my.zip")
      }
    }

    assertEquals("""
      |{
      |    "artifacts": [
      |        {
      |            "type": "ARTIFACT",
      |            "name": "art1",
      |            "children": [
      |                {
      |                    "type": "DIR",
      |                    "name": "dir1",
      |                    "children": [
      |                        {
      |                            "type": "FILE",
      |                            "sourceFiles": [
      |                                "$filePath"
      |                            ]
      |                        },
      |                        {
      |                            "type": "ARCHIVE",
      |                            "name": "arch1",
      |                            "children": [
      |                                {
      |                                    "type": "DIR_CONTENT",
      |                                    "sourceFiles": [
      |                                        "$dirPath"
      |                                    ]
      |                                },
      |                                {
      |                                    "type": "LIBRARY_FILES",
      |                                    "libraries": [
      |                                        {
      |                                            "group": "junit",
      |                                            "artifact": "junit",
      |                                            "version": "4.12"
      |                                        },
      |                                        {
      |                                            "group": "org.hamcrest",
      |                                            "artifact": "hamcrest-core",
      |                                            "version": "1.3"
      |                                        }
      |                                    ]
      |                                }
      |                            ]
      |                        }
      |                    ]
      |                }
      |            ]
      |        },
      |        {
      |            "type": "ARTIFACT",
      |            "name": "art2",
      |            "children": [
      |                {
      |                    "type": "ARTIFACT_REF",
      |                    "artifactName": "art1"
      |                },
      |                {
      |                    "type": "EXTRACTED_DIR",
      |                    "sourceFiles": [
      |                        "$archivePath"
      |                    ]
      |                }
      |            ]
      |        }
      |    ]
      |}
    """.trimMargin(),
            JsonOutput.prettyPrint(JsonOutput.toJson(artifacts.toMap()))
    )

  }


}

