package org.jetbrains.gradle.ext

import groovy.json.JsonOutput
import junit.framework.Assert.assertEquals
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.gradle.ext.runConfigurations.Application
import org.jetbrains.gradle.ext.runConfigurations.Make
import org.jetbrains.gradle.ext.runConfigurations.Remote
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
    config.excludes(object: Action<ExcludesConfig> {
      override fun execute(excludeCfg: ExcludesConfig?) {
        if (excludeCfg == null) return
        excludeCfg.file("C:/myFile.ext")
        excludeCfg.dir("C:/myDir")
        excludeCfg.dir("C:/recursiveDir", true)
      }
    })

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
      |    ],
      |    "heapSize": 2049
      |}
    """.trimMargin(),
            JsonOutput.prettyPrint(JsonOutput.toJson(config)))
  }
}

