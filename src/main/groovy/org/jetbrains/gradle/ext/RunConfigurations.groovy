package org.jetbrains.gradle.ext

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Action
import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer
import org.gradle.api.Named
import org.gradle.api.PolymorphicDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.DefaultPolymorphicDomainObjectContainer
import org.gradle.internal.reflect.Instantiator

import javax.inject.Inject

@CompileStatic
interface RunConfigurationContainer extends ExtensiblePolymorphicDomainObjectContainer<RunConfiguration> {
    public <T extends RunConfiguration> void defaults(Class<T> type, Action<T> action)
}

@CompileStatic
class DefaultRunConfigurationContainer extends DefaultPolymorphicDomainObjectContainer<RunConfiguration> implements RunConfigurationContainer {

    @Inject
    DefaultRunConfigurationContainer(Instantiator instantiator) {
        super(RunConfiguration, instantiator)
    }

    @Override
    public <T extends RunConfiguration> void defaults(Class<T> type, Action<T> action) {
        def defaults = maybeCreate("default_$type.name", type)
        defaults.defaults = true
        action.execute(defaults)
    }
}

@CompileStatic
interface RunConfiguration extends Named, MapConvertible {
    String getType()
    boolean isDefaults()
    void setDefaults(boolean defaults)
}

@CompileStatic
abstract class BaseRunConfiguration implements RunConfiguration {
    boolean defaults
}

@CompileStatic
class Application extends BaseRunConfiguration {

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
    @CompileStatic(TypeCheckingMode.SKIP)
    Application(String name, Project project) {
        this.name = name
        def beforeRun = GradleUtils.polymorphicContainer(project, BeforeRunTask)
        beforeRun.registerFactory(Make, { project.objects.newInstance(Make) })
        beforeRun.registerFactory(GradleTask, { project.objects.newInstance(GradleTask) })
        beforeRun.registerFactory(BuildArtifact, { project.objects.newInstance(BuildArtifact) })
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
                "beforeRun"        : beforeRun.toList().collect { it.toMap() },
                "jvmArgs"          : jvmArgs,
                "defaults"         : defaults,
                "name"             : name,
                "programParameters": programParameters
        ]
    }
}

@CompileStatic
abstract class BeforeRunTask implements Named, MapConvertible {
    @Override
    String getName() {
        return getType()
    }

    abstract String getType()
}

@CompileStatic
class Make extends BeforeRunTask {
    String type = "make"
    Boolean enabled = true

    @Inject
    Make() { }

    @Override
    Map<String, ?> toMap() {
        return [
                "type"    : getType(),
                "enabled" : enabled
        ]
    }
}

@CompileStatic
class GradleTask extends BeforeRunTask {
    String type = "gradleTask"
    Task task

    @Inject
    GradleTask() {}

    @Override
    Map<String, ?> toMap() {
        return [
                "type": getType(),
                "projectPath": task.project.path,
                "taskName": task.name
        ]
    }
}

@CompileStatic
class BuildArtifact extends BeforeRunTask {
    String type = "buildArtifact"
    String artifactName

    @Inject
    BuildArtifact() {}

    @Override
    Map<String, ?> toMap() {
        return [
                "type"    : getType(),
                "artifactName" : artifactName
        ]
    }
}

@CompileStatic
class TestNG extends BaseRunConfiguration {
    final String name
    final String type = "testng"

    // only one type will be used
    String packageName
    String className
    String method
    String group
    String suite
    String pattern
    //

    String workingDirectory
    String vmParameters
    Boolean passParentEnvs
    String moduleName
    Map<String, String> envs

    @Inject
    TestNG(String name) {
        this.name = name
    }

    @Override
    String getType() {
        return type
    }

    @Override
    String getName() {
        return name
    }

    @Override
    Map<String, ?> toMap() {
        return [
                "type": type,
                "name": name,
                "defaults": defaults,

                "package": packageName,
                "class": className,
                "method": method,
                "group": group,
                "suite": suite,
                "pattern": pattern,
                "workingDirectory": workingDirectory,
                "vmParameters": vmParameters,
                "passParentEnvs": passParentEnvs,
                "moduleName": moduleName,
                "envs": envs
        ]
    }
}

@CompileStatic
class JUnit extends BaseRunConfiguration {

    final String name
    final String type = "junit"

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

@CompileStatic
class Remote extends BaseRunConfiguration {
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

    @Inject
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