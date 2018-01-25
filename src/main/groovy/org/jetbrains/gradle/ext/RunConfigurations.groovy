package org.jetbrains.gradle.ext

import org.gradle.api.Action
import org.gradle.api.Namer
import org.gradle.api.PolymorphicDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.internal.DefaultPolymorphicDomainObjectContainer
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.reflect.Instantiator

import javax.inject.Inject

interface RunConfiguration {
    String getName()

    String getType()

    boolean isDefaults()

    void setDefaults(boolean defaults)

    Map<String, ?> toMap()
}

class Application implements RunConfiguration {

    final String name
    final String type = "application"
    boolean defaults

    String mainClass
    String workingDirectory
    String jvmArgs
    String moduleName
    String programParameters
    Map<String, String> envs

    final PolymorphicDomainObjectContainer<BeforeRunTask> beforeRun

    @Inject
    Application(String name, Project project) {
        this.name = name
        def instantiator = (project as ProjectInternal).services.get(Instantiator)
        def beforeRun = instantiator.newInstance(
                DefaultPolymorphicDomainObjectContainer,
                BeforeRunTask,
                instantiator,
                { BeforeRunTask beforeRunTask -> beforeRunTask.id } as Namer<BeforeRunTask>)
        beforeRun.registerFactory(Make, { project.objects.newInstance(Make) })
        this.beforeRun = beforeRun
    }

    def beforeRun(Action<PolymorphicDomainObjectContainer<BeforeRunTask>> action) {
        action.execute(beforeRun)
    }

    @Override
    Map<String, ?> toMap() {
        return [
                "type"             : type,
                "envs"             : envs,
                "workingDirectory" : workingDirectory,
                "mainClass"        : mainClass,
                "moduleName"       : moduleName,
                "beforeRun"        : beforeRun.toList().collect { ["id": it.id, "enabled": it.enabled] },
                "jvmArgs"          : jvmArgs,
                "defaults"         : defaults,
                "name"             : name,
                "programParameters": programParameters
        ]
    }
}

class BeforeRunTask {
    final String id
    Boolean enabled = true

    BeforeRunTask(String id) {
        this.id = id
    }
}

class Make extends BeforeRunTask {
    public static final String ID = "Make"

    Make() {
        super(ID)
    }
}

class JUnit implements RunConfiguration {

    final String name
    final String type = "junit"
    boolean defaults

    // only one (first not null) type will be used
    String packageName
    String directory
    String pattern
    String className
    String method
    String category
    // end of type list

    String repeat
    String workingDirectory
    String vmParameters
    Boolean passParentEnvs
    String moduleName
    Map<String, String> envs

    @Inject
    JUnit(String name) {
        this.name = name
    }

    @Override
    Map<String, ?> toMap() {
        return [
                "directory"       : directory,
                "type"            : type,
                "repeat"          : repeat,
                "envs"            : envs,
                "vmParameters"    : vmParameters,
                "category"        : category,
                "workingDirectory": workingDirectory,
                "className"       : className,
                "moduleName"      : moduleName,
                "passParentEnvs"  : passParentEnvs,
                "packageName"     : packageName,
                "defaults"        : defaults,
                "pattern"         : pattern,
                "name"            : name,
                "method"          : method
        ]
    }
}

class Remote implements RunConfiguration {
    static enum RemoteMode {
        ATTACH, LISTEN
    }

    static enum RemoteTransport {
        SOCKET, SHARED_MEM
    }

    final String name
    final String type = "remote"
    boolean defaults
    RemoteTransport transport = RemoteTransport.SOCKET
    RemoteMode mode = RemoteMode.ATTACH
    String host
    Integer port
    String sharedMemoryAddress

    Remote(String name) {
        this.name = name
    }

    @Override
    Map<String, ?> toMap() {
        return [
                "type"               : type,
                "mode"               : mode,
                "port"               : port,
                "transport"          : transport,
                "host"               : host,
                "defaults"           : defaults,
                "name"               : name,
                "sharedMemoryAddress": sharedMemoryAddress
        ]
    }
}