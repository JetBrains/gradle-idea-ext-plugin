package org.jetbrains.gradle.ext

import groovy.json.JsonOutput
import junit.framework.Assert.assertEquals
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.gradle.ext.runConfigurations.Application
import org.jetbrains.gradle.ext.runConfigurations.Make
import org.junit.Test

class RunConfigurationTests {
  @Test fun `test json output`() {
    val project = ProjectBuilder.builder().build()
    val application = Application("test", project)
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
}