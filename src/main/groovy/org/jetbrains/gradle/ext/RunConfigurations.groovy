package org.jetbrains.gradle.ext

import groovy.transform.CompileStatic
import org.codehaus.groovy.runtime.DefaultGroovyMethods
import org.gradle.api.Action
import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer
import org.gradle.api.Named
import org.gradle.api.PolymorphicDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer

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
abstract class ModuleRunConfiguration extends BaseRunConfiguration {
    String moduleName
    ModuleRef moduleRef

    void setModuleName(String name) {
        moduleName = name
        moduleRef = null
    }

    void setModuleRef(ModuleRef ref) {
        moduleRef = ref
        moduleName = null
    }

    def moduleRef(Project project, SourceSet sourceSet = null) {
        setModuleRef(new ModuleRef(project, sourceSet))
    }

    @Override
    Map<String, ?> toMap() {
        String resultingModuleName = null

        if (moduleName != null) {
            resultingModuleName = moduleName
        } else if (moduleRef != null) {
            resultingModuleName = moduleRef.toModuleName()
        }

        return super.toMap() << [
                "moduleName"        : resultingModuleName,
        ]
    }
}

@CompileStatic
abstract class JavaRunConfiguration extends ModuleRunConfiguration {
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
                "beforeRun"        : DefaultGroovyMethods.collect(beforeRun.toList() as Collection<BeforeRunTask>) { it.toMap() },
                "jvmArgs"          : jvmArgs,
                "programParameters": programParameters
        ]
    }
}

@CompileStatic
class Application extends JavaRunConfiguration {
    String mainClass
    ShortenCommandLine shortenCommandLine
    Boolean includeProvidedDependencies = false

    @Inject
    Application(String nameParam, Project project) {
        super(project)
        name = nameParam
        type = "application"
    }

    @Override
    Map<String, ?> toMap() {
        return super.toMap() << [
                "mainClass"         : mainClass,
                "shortenCommandLine": shortenCommandLine,
                "includeProvidedDependencies": includeProvidedDependencies
        ]
    }
}

@CompileStatic
class JarApplication extends JavaRunConfiguration {
    String jarPath

    @Inject
    JarApplication(String nameParam, Project project) {
        super(project)
        name = nameParam
        type = "jarApplication"
    }

    @Override
    Map<String, ?> toMap() {
        return super.toMap() << [
                "jarPath": jarPath
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
    Make(String nameParam) {
        type = "make"
        name = nameParam
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
    GradleTask(String nameParam) {
        type = "gradleTask"
        name = nameParam
    }

    @Override
    Map<String, ?> toMap() {
        return super.toMap() << [
                "projectPath": task.project.projectDir.absolutePath.replaceAll("\\\\", "/"),
                "taskName": task.name
        ]
    }
}

@CompileStatic
class BuildArtifact extends BeforeRunTask {
    String artifactName

    @Inject
    BuildArtifact(String nameParam) {
        type = "buildArtifact"
        name = nameParam
    }

    @Override
    Map<String, ?> toMap() {
        return super.toMap() << ["artifactName" : artifactName]
    }
}

@CompileStatic
class TestNG extends ModuleRunConfiguration {

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
    Map<String, String> envs
    ShortenCommandLine shortenCommandLine

  @Inject
  TestNG(String nameParam) {
    name = nameParam
    type = "testng"
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
                "envs": envs,
                "shortenCommandLine": shortenCommandLine
        ]
    }
}

@CompileStatic
class JUnit extends ModuleRunConfiguration {

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
    Map<String, String> envs
    ShortenCommandLine shortenCommandLine

  @Inject
  JUnit(String nameParam) {
    name = nameParam
    type = "junit"
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
    Boolean autoRestart = false

  @Inject
  Remote(String nameParam) {
    name = nameParam
    type = "remote"
  }

  @Override
    Map<String, ?> toMap() {
        return super.toMap() << [
                "mode"               : mode,
                "port"               : port,
                "transport"          : transport,
                "host"               : host,
                "sharedMemoryAddress": sharedMemoryAddress,
                "autoRestart"        : autoRestart
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
  Gradle(String nameParam) {
    name = nameParam
    type = "gradle"
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
class ModuleRef {

    Project project
    String sourceSetName = ""

    /**
     * Creates a reference to a module based on project and a source set
     * If a source set is not null, it must belong to the project
     * @param project Gradle project
     * @param sourceSet Source set within project, optional.
     * @throw IllegalArgumentException if source set does not belong to project
     */
    ModuleRef(Project project, SourceSet sourceSet) {
        this.project = project
        if (sourceSet != null && project.hasProperty("sourceSets")) {
            SourceSetContainer sourceSets = project.property("sourceSets") as SourceSetContainer
            if (sourceSets.contains(sourceSet)) {
                this.sourceSetName = sourceSet.name
            } else {
                throw new IllegalArgumentException("Source set $sourceSet does not belong to $project")
            }
        }
    }

    ModuleRef(Project project) {
        this(project, null)
    }

    String toModuleName() {
        def name = project.rootProject.name
        if (project.path == ":") {
            return addSourceSetName(name)
        }
        return addSourceSetName(name + project.path.replaceAll(":", "."))
    }

    String addSourceSetName(String moduleName) {
        return moduleName + (sourceSetName.isEmpty() ? "" : "." + sourceSetName)
    }
}

@CompileStatic
enum ShortenCommandLine {
  NONE,
  MANIFEST,
  CLASSPATH_FILE,
  ARGS_FILE
}