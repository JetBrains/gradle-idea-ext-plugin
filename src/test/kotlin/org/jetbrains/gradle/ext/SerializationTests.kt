package org.jetbrains.gradle.ext

import com.google.gson.JsonParser
import groovy.json.JsonOutput
import org.gradle.api.Project
import org.gradle.api.internal.provider.DefaultProvider
import org.gradle.testfixtures.ProjectBuilder
import org.intellij.lang.annotations.Language
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
    val application = Application("test", myProject).apply {
      beforeRun.create("make", Make::class.java).apply {
        enabled = false
      }
      beforeRun.create("myArtifact", BuildArtifact::class.java).apply {
        artifactName = "myName"
      }
      shortenCommandLine = ShortenCommandLine.MANIFEST
    }

    assertEquals("""
    |{
    |    "defaults": false,
    |    "type": "application",
    |    "name": "test",
    |    "envs": null,
    |    "workingDirectory": null,
    |    "beforeRun": [
    |        {
    |            "type": "buildArtifact",
    |            "artifactName": "myName"
    |        },
    |        {
    |            "type": "make",
    |            "enabled": false
    |        }
    |    ],
    |    "jvmArgs": null,
    |    "programParameters": null,
    |    "mainClass": null,
    |    "moduleName": null,
    |    "shortenCommandLine": "MANIFEST"
    |}
    """.trimMargin(),
            JsonOutput.prettyPrint(JsonOutput.toJson(application.toMap())))
  }
  @Test fun `test JarApplication json output`() {
    val jarApplication = JarApplication("test", myProject).apply {
      beforeRun.create("make", Make::class.java).apply {
        enabled = false
      }
      beforeRun.create("myArtifact", BuildArtifact::class.java).apply {
        artifactName = "myName"
      }
      jarPath = "myJarPath"
    }

    assertEquals("""
    |{
    |    "defaults": false,
    |    "type": "jarApplication",
    |    "name": "test",
    |    "envs": null,
    |    "workingDirectory": null,
    |    "beforeRun": [
    |        {
    |            "type": "buildArtifact",
    |            "artifactName": "myName"
    |        },
    |        {
    |            "type": "make",
    |            "enabled": false
    |        }
    |    ],
    |    "jvmArgs": null,
    |    "programParameters": null,
    |    "jarPath": "myJarPath",
    |    "moduleName": null
    |}
    """.trimMargin(),
            JsonOutput.prettyPrint(JsonOutput.toJson(jarApplication.toMap())))
  }

  @Test
  fun `test remote json output`() {
    val remote = Remote("remote debug").apply {
      host = "hostname"
      port = 1234
      sharedMemoryAddress = "jvmdebug"
    }

    @Language("JSON")
    val expected = JsonParser.parseString("""
    {
        "defaults": false,
        "type": "remote",
        "name": "remote debug",
        "mode": "ATTACH",
        "port": 1234,
        "transport": "SOCKET",
        "host": "hostname",
        "sharedMemoryAddress": "jvmdebug"
    }
    """)
    assertEquals(expected, JsonParser.parseString(JsonOutput.toJson(remote.toMap())))
  }

  @Test
  fun `test JUnit config output`() {
    val config = JUnit("myName").apply {
      className = "my.TestClass"
      repeat = "untilFailure"
      workingDirectory = "myWorkDir"
      vmParameters = "-Dkey=Value"
      passParentEnvs = true
      moduleName = "myModule"
      envs = mapOf("env1" to "envVal1", "env2" to "envVal2")
      defaults = true
      shortenCommandLine = ShortenCommandLine.CLASSPATH_FILE
    }

    @Language("JSON")
    val expected = JsonParser.parseString("""
                {
                    "defaults": true,
                    "type": "junit",
                    "name": "myName",
                    "directory": null,
                    "repeat": "untilFailure",
                    "envs": {
                        "env1": "envVal1",
                        "env2": "envVal2"
                    },
                    "vmParameters": "-Dkey=Value",
                    "category": null,
                    "workingDirectory": "myWorkDir",
                    "className": "my.TestClass",
                    "moduleName": "myModule",
                    "passParentEnvs": true,
                    "packageName": null,
                    "pattern": null,
                    "method": null,
                    "shortenCommandLine": "CLASSPATH_FILE"
                }
      """)
    assertEquals(expected, JsonParser.parseString(JsonOutput.toJson(config.toMap())))
  }

  @Test fun `test TestNG config output`() {
    val config = TestNG("myName").apply {
      className = "my.TestClass"

      workingDirectory = "myWorkDir"
      vmParameters = "-Dkey=Value"
      passParentEnvs = true
      moduleName = "myModule"
      envs = mapOf("env1" to "envVal1", "env2" to "envVal2")
      defaults = true
      shortenCommandLine = ShortenCommandLine.ARGS_FILE
    }

    @Language("JSON")
    val expected = JsonParser.parseString("""
      {
          "defaults": true,
          "type": "testng",
          "name": "myName",
          "package": null,
          "class": "my.TestClass",
          "method": null,
          "group": null,
          "suite": null,
          "pattern": null,
          "workingDirectory": "myWorkDir",
          "vmParameters": "-Dkey=Value",
          "passParentEnvs": true,
          "moduleName": "myModule",
          "envs": {
              "env1": "envVal1",
              "env2": "envVal2"
          },
          "shortenCommandLine": "ARGS_FILE"
      }
    """)
    assertEquals(expected, JsonParser.parseString(JsonOutput.toJson(config.toMap())))
  }

  @Test
  fun `test Gradle run configuration output`() {
    val absolutePath = File("").absolutePath.replace("\\", "/")
    val config = Gradle("gradleName").apply {
      projectPath = absolutePath
      taskNames = listOf(":cleanTest", ":test")
      jvmArgs = "-Dkey=val"
      scriptParameters = "-PscriptParam"
      envs = mapOf("env1" to "envVal1", "env2" to "envVal2")
      defaults = true
    }

    @Language("JSON")
    val expected = JsonParser.parseString("""
      {
          "defaults": true,
          "type": "gradle",
          "name": "gradleName",
          "projectPath": "$absolutePath",
          "taskNames": [
              ":cleanTest",
              ":test"
          ],
          "envs": {
              "env1": "envVal1",
              "env2": "envVal2"
          },
          "jvmArgs": "-Dkey=val",
          "scriptParameters": "-PscriptParam"
      }
    """)
    assertEquals(expected, JsonParser.parseString(JsonOutput.toJson(config.toMap())))
  }

  @Test fun `test Groovy config output`() {
    val config = GroovyCompilerConfiguration()
    config.excludes {
      it.file("C:/myFile.ext")
      it.dir("C:/myDir")
      it.dir("C:/recursiveDir", true)
    }

    assertEquals("""
      |{
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
    val config = TaskTriggersConfig(myProject)
    val subProject = ProjectBuilder.builder()
            .withProjectDir(File(myProject.projectDir, "subProject"))
            .withParent(myProject)
            .build()

    val tasks = myProject.tasks
    tasks.apply {
      create("task1")
      create("task2")
      create("task3")
      create("task4")
      create("task5")
    }

    val subTasks = subProject.tasks
    subTasks.apply {
      create("subtask")
    }

    config.apply {
      beforeBuild(tasks.getByName("task1"), tasks.getByName("task2"))
      beforeBuild(tasks.matching { it.name.endsWith("3")})
      afterSync(tasks.getByName("task3"))
      beforeRebuild(tasks.getByName("task1"), tasks.named("task4"), DefaultProvider<String> { "task5" } )
      afterRebuild(subTasks.getByName("subtask"))
    }

    val escapedRootProjectPath = myProject.projectDir.path.replace("\\", "/")
    val subProjectPath = subProject.projectDir.path.replace("\\", "/")

    assertEquals("""
      |{
      |    "beforeBuild": [
      |        {
      |            "taskPath": ":task1",
      |            "projectPath": "$escapedRootProjectPath"
      |        },
      |        {
      |            "taskPath": ":task2",
      |            "projectPath": "$escapedRootProjectPath"
      |        },
      |        {
      |            "taskPath": ":task3",
      |            "projectPath": "$escapedRootProjectPath"
      |        }
      |    ],
      |    "afterSync": [
      |        {
      |            "taskPath": ":task3",
      |            "projectPath": "$escapedRootProjectPath"
      |        }
      |    ],
      |    "beforeRebuild": [
      |        {
      |            "taskPath": ":task1",
      |            "projectPath": "$escapedRootProjectPath"
      |        },
      |        {
      |            "taskPath": ":task4",
      |            "projectPath": "$escapedRootProjectPath"
      |        },
      |        {
      |            "taskPath": ":task5",
      |            "projectPath": "$escapedRootProjectPath"
      |        }
      |    ],
      |    "afterRebuild": [
      |        {
      |            "taskPath": ":test:subtask",
      |            "projectPath": "$escapedRootProjectPath"
      |        }
      |    ]
      |}
    """.trimMargin(),
            JsonOutput.prettyPrint(JsonOutput.toJson(config.toMap()))
      )
  }

  @Test fun `test artifacts tree`() {
    val artifacts = myProject.container(TopLevelArtifact::class.java, TopLevelArtifactFactory(myProject))

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
    myProject.repositories.jcenter()
    myProject.dependencies.add(configurationName, "junit:junit:4.12")


    artifacts.apply {
      create("art1") {
        it.directory("dir1") {
          it.file(File("file.txt"))
          it.archive("arch1") {
            it.directoryContent("dir")
            it.libraryFiles(testCfg)
          }
          it.moduleOutput("moduleName")
        }
      }
      create("art2") {
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
      |                        },
      |                        {
      |                            "type": "MODULE_OUTPUT",
      |                            "moduleName": "moduleName"
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
            JsonOutput.prettyPrint(JsonOutput.toJson(mapOf("artifacts" to artifacts.map { it.toMap() })))
    )

  }


}

