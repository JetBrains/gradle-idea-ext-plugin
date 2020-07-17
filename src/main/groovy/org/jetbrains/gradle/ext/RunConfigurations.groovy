package org.jetbrains.gradle.ext

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer
import org.gradle.api.Named
import org.gradle.api.PolymorphicDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Task

import javax.inject.Inject

@CompileStatic
interface RunConfigurationContainer extends ExtensiblePolymorphicDomainObjectContainer<RunConfiguration> {
    public <T extends RunConfiguration> void defaults(Class<T> type, Action<T> action)
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
    protected String name
    protected String type

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
                "defaults" : defaults,
                "type": type,
                "name": name
        ]
    }
}

@CompileStatic
abstract class JavaRunConfiguration extends BaseRunConfiguration {
    String workingDirectory
    String jvmArgs
    String programParameters
    Map<String, String> envs

    final PolymorphicDomainObjectContainer<BeforeRunTask> beforeRun

    static PolymorphicDomainObjectContainer<BeforeRunTask> createBeforeRun(Project project) {
        def beforeRun = GradleUtils.polymorphicContainer(project, BeforeRunTask)
        beforeRun.registerFactory(Make) { String name -> project.objects.newInstance(Make, name) }
        beforeRun.registerFactory(GradleTask) { String name -> project.objects.newInstance(GradleTask, name) }
        beforeRun.registerFactory(BuildArtifact) { String name -> project.objects.newInstance(BuildArtifact, name) }
        return beforeRun
    }

    JavaRunConfiguration(Project project) {
        this.beforeRun = createBeforeRun(project)
    }

    def beforeRun(Action<PolymorphicDomainObjectContainer<BeforeRunTask>> action) {
        action.execute(beforeRun)
    }

    @Override
    Map<String, ?> toMap() {
        return super.toMap() << [
                "envs"             : envs,
                "workingDirectory" : workingDirectory,
                "beforeRun"        : beforeRun.toList().collect { it.toMap() },
                "jvmArgs"          : jvmArgs,
                "programParameters": programParameters
        ]
    }
}

@CompileStatic
class Application extends JavaRunConfiguration {
    String mainClass
    String moduleName
    ShortenCommandLine shortenCommandLine

    @Inject
    Application(String name, Project project) {
        super(project)
        this.@name = name
        this.@type = "application"
    }

    @Override
    Map<String, ?> toMap() {
        return super.toMap() << [
                "mainClass"        : mainClass,
                "moduleName"       : moduleName,
                "shortenCommandLine": shortenCommandLine
        ]
    }
}

@CompileStatic
class JarApplication extends JavaRunConfiguration {
    String jarPath
    String moduleName

    @Inject
    JarApplication(String name, Project project) {
        super(project)
        this.@name = name
        this.@type = "jarApplication"
    }

    @Override
    Map<String, ?> toMap() {
        return super.toMap() << [
                "jarPath": jarPath,
                "moduleName": moduleName
        ]
    }
}

@CompileStatic
abstract class BeforeRunTask implements Named, MapConvertible {
    protected String type
    protected String name

    @Override
    String getName() {
        return name
    }

    @Override
    Map<String, ?> toMap() {
        return ["type" : type]
    }
}

@CompileStatic
class Make extends BeforeRunTask {
    Boolean enabled = true

    @Inject
    Make(String name) {
        this.@type = "make"
        this.@name = name
    }

    @Override
    Map<String, ?> toMap() {
        return super.toMap() << ["enabled" : enabled]
    }
}

@CompileStatic
class GradleTask extends BeforeRunTask {
    Task task

    @Inject
    GradleTask(String name) {
        this.@type = "gradleTask"
        this.@name = name
    }

    @Override
    Map<String, ?> toMap() {
        return super.toMap() << [
                "projectPath": task.project.rootDir.absolutePath,
                "taskName": task.name
        ]
    }
}

@CompileStatic
class BuildArtifact extends BeforeRunTask {
    String artifactName

    @Inject
    BuildArtifact(String name) {
        this.@type = "buildArtifact"
        this.@name = name
    }

    @Override
    Map<String, ?> toMap() {
        return super.toMap() << ["artifactName" : artifactName]
    }
}

@CompileStatic
class TestNG extends BaseRunConfiguration {

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
    ShortenCommandLine shortenCommandLine

  @Inject
  TestNG(String name) {
    super.@name = name
    super.@type = "testng"
  }

  @Override
    Map<String, ?> toMap() {
        return super.toMap() << [
                "type": type,
                "name": name,

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
                "envs": envs,
                "shortenCommandLine": shortenCommandLine
        ]
    }
}

@CompileStatic
class JUnit extends BaseRunConfiguration {

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
    ShortenCommandLine shortenCommandLine

  @Inject
  JUnit(String name) {
    super.@name = name
    super.@type = "junit"
  }

  @Override
    Map<String, ?> toMap() {
        return super.toMap() << [
                "directory"       : directory,
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
                "method"          : method,
                "shortenCommandLine" : shortenCommandLine
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

    RemoteTransport transport = RemoteTransport.SOCKET
    RemoteMode mode = RemoteMode.ATTACH
    String host
    Integer port
    String sharedMemoryAddress

  @Inject
  Remote(String name) {
    super.@name = name
    super.@type = "remote"
  }

  @Override
    Map<String, ?> toMap() {
        return super.toMap() << [
                "mode"               : mode,
                "port"               : port,
                "transport"          : transport,
                "host"               : host,
                "sharedMemoryAddress": sharedMemoryAddress
        ]
    }
}

@CompileStatic
class Gradle extends BaseRunConfiguration {

    String projectPath
    Collection<String> taskNames
    Map<String, String> envs
    String jvmArgs
    String scriptParameters

  @Inject
  Gradle(String name) {
    super.@name = name
    super.@type = "gradle"
  }

    def setProject(Project project) {
        projectPath = project.projectDir.absolutePath
    }

    @Override
    Map<String, ?> toMap() {
        return super.toMap() << [
                "projectPath"     : projectPath,
                "taskNames"       : taskNames,
                "envs"            : envs,
                "jvmArgs"       : jvmArgs,
                "scriptParameters": scriptParameters
        ]
    }
}

@CompileStatic
enum ShortenCommandLine {
  NONE,
  MANIFEST,
  CLASSPATH_FILE,
  ARGS_FILE
}