package org.jetbrains.gradle.ext

import groovy.json.JsonOutput
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

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

    config.IF_BRACE_FORCE = ForceBraces.FORCE_BRACES_IF_MULTILINE

    config.java {
      it.CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND = 42
      it.FOR_BRACE_FORCE = ForceBraces.FORCE_BRACES_ALWAYS
    }

    config.groovy {
      it.ALIGN_NAMED_ARGS_IN_MAP = true
      it.RIGHT_MARGIN = 99
    }

    assertEquals("""
      |{
      |    "WHILE_BRACE_FORCE": null,
      |    "JD_KEEP_EMPTY_RETURN": null,
      |    "WRAP_COMMENTS": null,
      |    "ALIGN_NAMED_ARGS_IN_MAP": null,
      |    "CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND": null,
      |    "JD_ALIGN_EXCEPTION_COMMENTS": null,
      |    "FOR_BRACE_FORCE": null,
      |    "JD_KEEP_EMPTY_EXCEPTION": null,
      |    "JD_KEEP_EMPTY_PARAMETER": null,
      |    "JD_P_AT_EMPTY_LINES": null,
      |    "DOWHILE_BRACE_FORCE": null,
      |    "USE_SAME_IDENTS": null,
      |    "JD_ALIGN_PARAM_COMMENTS": null,
      |    "KEEP_CONTROL_STATEMENT_IN_ONE_LINE": null,
      |    "RIGHT_MARGIN": null,
      |    "IF_BRACE_FORCE": "FORCE_BRACES_IF_MULTILINE",
      |    "languages": {
      |        "java": {
      |            "WHILE_BRACE_FORCE": null,
      |            "JD_KEEP_EMPTY_RETURN": null,
      |            "WRAP_COMMENTS": null,
      |            "ALIGN_NAMED_ARGS_IN_MAP": null,
      |            "CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND": 42,
      |            "JD_ALIGN_EXCEPTION_COMMENTS": null,
      |            "FOR_BRACE_FORCE": "FORCE_BRACES_ALWAYS",
      |            "JD_KEEP_EMPTY_EXCEPTION": null,
      |            "JD_KEEP_EMPTY_PARAMETER": null,
      |            "JD_P_AT_EMPTY_LINES": null,
      |            "DOWHILE_BRACE_FORCE": null,
      |            "USE_SAME_IDENTS": null,
      |            "JD_ALIGN_PARAM_COMMENTS": null,
      |            "KEEP_CONTROL_STATEMENT_IN_ONE_LINE": null,
      |            "RIGHT_MARGIN": null,
      |            "IF_BRACE_FORCE": null
      |        },
      |        "groovy": {
      |            "WHILE_BRACE_FORCE": null,
      |            "JD_KEEP_EMPTY_RETURN": null,
      |            "WRAP_COMMENTS": null,
      |            "ALIGN_NAMED_ARGS_IN_MAP": true,
      |            "CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND": null,
      |            "JD_ALIGN_EXCEPTION_COMMENTS": null,
      |            "FOR_BRACE_FORCE": null,
      |            "JD_KEEP_EMPTY_EXCEPTION": null,
      |            "JD_KEEP_EMPTY_PARAMETER": null,
      |            "JD_P_AT_EMPTY_LINES": null,
      |            "DOWHILE_BRACE_FORCE": null,
      |            "USE_SAME_IDENTS": null,
      |            "JD_ALIGN_PARAM_COMMENTS": null,
      |            "KEEP_CONTROL_STATEMENT_IN_ONE_LINE": null,
      |            "RIGHT_MARGIN": 99,
      |            "IF_BRACE_FORCE": null
      |        }
      |    }
      |}
    """.trimMargin(),
            JsonOutput.prettyPrint(JsonOutput.toJson(config.toMap())))

  }

}

