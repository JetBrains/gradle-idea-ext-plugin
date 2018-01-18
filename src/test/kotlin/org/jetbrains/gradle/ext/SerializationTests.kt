package org.jetbrains.gradle.ext

import groovy.json.JsonOutput
import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.gradle.ext.runConfigurations.Application
import org.jetbrains.gradle.ext.runConfigurations.Make
import org.jetbrains.gradle.ext.runConfigurations.Remote
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
    val make = application.beforeRun.create(Make.ID, Make::class.java)
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
    |            "id": "Make",
    |            "enabled": false
    |        }
    |    ],
    |    "jvmArgs": null,
    |    "defaults": false,
    |    "name": "test",
    |    "programParameters": null
    |}
    """.trimMargin(),
            JsonOutput.prettyPrint(JsonOutput.toJson(application)))
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
            JsonOutput.prettyPrint(JsonOutput.toJson(remote)))

  }

  @Test fun `test Groovy config output`() {
    val config = GroovyCompilerConfiguration()
    config.heapSize = 2049
    config.excludes(object: Closure<ExcludesConfig>(this) {
      override fun call(): ExcludesConfig {
        val excludeCfg = (delegate as ExcludesConfig)
        excludeCfg.file("C:/myFile.ext")
        excludeCfg.dir("C:/myDir")
        excludeCfg.dir("C:/recursiveDir", true)
        return excludeCfg
      }
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

    config.java(object: Closure<LanguageCodeStyleConfig>(this) {
      override fun call(): LanguageCodeStyleConfig {
        val javaCfg = (delegate as LanguageCodeStyleConfig)
        javaCfg.CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND = 42
        javaCfg.FOR_BRACE_FORCE = ForceBraces.FORCE_BRACES_ALWAYS
        return javaCfg
      }
    })

    config.groovy(object: Closure<LanguageCodeStyleConfig>(this) {
      override fun call(): LanguageCodeStyleConfig {
        val groovyCfg = (delegate as LanguageCodeStyleConfig)
        groovyCfg.ALIGN_NAMED_ARGS_IN_MAP = true
        groovyCfg.RIGHT_MARGIN = 99
        return groovyCfg
      }
    })

    assertEquals("""
      |{
      |    "WHILE_BRACE_FORCE": null,
      |    "JD_KEEP_EMPTY_RETURN": null,
      |    "WRAP_COMMENTS": null,
      |    "ALIGN_NAMED_ARGS_IN_MAP": null,
      |    "CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND": null,
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
      |    },
      |    "JD_ALIGN_EXCEPTION_COMMENTS": null,
      |    "FOR_BRACE_FORCE": null,
      |    "JD_KEEP_EMPTY_EXCEPTION": null,
      |    "JD_KEEP_EMPTY_PARAMETER": null,
      |    "JD_P_AT_EMPTY_LINES": null,
      |    "DOWHILE_BRACE_FORCE": null,
      |    "USE_SAME_IDENTS": null,
      |    "JD_ALIGN_PARAM_COMMENTS": null,
      |    "RIGHT_MARGIN": null,
      |    "KEEP_CONTROL_STATEMENT_IN_ONE_LINE": null,
      |    "IF_BRACE_FORCE": "FORCE_BRACES_IF_MULTILINE"
      |}
    """.trimMargin(),
            JsonOutput.prettyPrint(JsonOutput.toJson(config)))

  }

}

