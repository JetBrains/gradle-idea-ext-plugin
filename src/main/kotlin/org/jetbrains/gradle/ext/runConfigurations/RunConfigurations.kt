package org.jetbrains.gradle.ext.runConfigurations

import groovy.lang.Closure
import org.gradle.api.Namer
import org.gradle.api.Project
import org.gradle.api.internal.DefaultPolymorphicDomainObjectContainer
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.reflect.Instantiator

/**
 * Created by Nikita.Skvortsov
 * date: 01.10.2017.
 */

open class RunConfiguration(val name: String, val type: String) {
  var defaults: Boolean = false
}


open class Application(name: String, project: Project) : RunConfiguration(name, "application") {
  var mainClass: String? = null
  var workingDirectory: String? = null
  var jvmArgs: String? = null
  var moduleName: String? = null
  var programParameters: String? = null
  var envs: Map<String, String>? = null

  val beforeRun: DefaultPolymorphicDomainObjectContainer<BeforeRunTask>
  fun beforeRun(action: Closure<*>) = beforeRun.configure(action)

  init {
    val instantiator = (project as ProjectInternal).services.get(Instantiator::class.java)
    beforeRun = instantiator.newInstance(DefaultPolymorphicDomainObjectContainer::class.java,
            BeforeRunTask::class.java,
            instantiator,
            Namer<BeforeRunTask> { it.id }) as DefaultPolymorphicDomainObjectContainer<BeforeRunTask>

    beforeRun.registerFactory(Make::class.java) { Make() }
  }
}

open class BeforeRunTask(val id: String) {
  var enabled = true
}

open class Make: BeforeRunTask(ID) {
  companion object {
    const val ID = "Make"
  }
}


open class JUnit(name: String): RunConfiguration(name, "junit") {

  // only one (first not null) type will be used
  var packageName : String? = null
  var directory : String? = null
  var pattern : String? = null
  var className : String? = null
  var method : String? = null
  var category : String? = null
  // end of type list

  var workingDirectory: String? = null
  var vmParameters: String? = null
  var passParentEnvs = true
  var moduleName: String? = null
  var envs: Map<String, String>? = null
}

open class Remote(name: String): RunConfiguration(name, "remote") {
  enum class RemoteMode { ATTACH, LISTEN }
  enum class RemoteTransport { SOCKET, SHARED_MEM }

  var transport: RemoteTransport = RemoteTransport.SOCKET
  var mode: RemoteMode = RemoteMode.ATTACH
  var host: String? = null
  var port: Int = 0
  var sharedMemoryAddress: String? = null
}

