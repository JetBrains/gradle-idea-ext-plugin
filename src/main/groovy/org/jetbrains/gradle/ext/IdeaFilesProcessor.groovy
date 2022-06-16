package org.jetbrains.gradle.ext

import com.google.gson.Gson
import groovy.transform.CompileStatic
import groovy.transform.Synchronized
import org.gradle.BuildListener
import org.gradle.BuildResult
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.XmlProvider
import org.gradle.api.initialization.Settings
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildServiceSpec
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.internal.xml.XmlTransformer
import org.gradle.util.GradleVersion

import javax.inject.Inject
import java.util.concurrent.atomic.AtomicBoolean

@CompileStatic
class IdeaFilesProcessor {
    Project myProject
    List<Action<File>> ideaDirCallbacks = new ArrayList<>()
    Map<String, List<Action<XmlProvider>>> ideaFileXmlCallbacks = new HashMap<String, List<Action<XmlProvider>>>()
    Map<String, List<Action<File>>> imlsCallbacks = new HashMap<String, List<Action<File>>>()
    Map<String, List<Action<XmlProvider>>> xmlCallbacks = new HashMap<String, List<Action<XmlProvider>>>()

    IdeaFilesProcessor(Project project) {
        myProject = project
        if (GradleVersion.current() > GradleVersion.version("6.1")) {
            def serviceProvider = project.getGradle().getSharedServices()
                    .registerIfAbsent("layoutFile", LayoutFileBuildService.class,
                            new Action<BuildServiceSpec<LayoutFileBuildService.Params>>() {
                                @Override
                                void execute(BuildServiceSpec<LayoutFileBuildService.Params> spec) {
                                    spec.getParameters().getLayoutFile().set(myProject.rootProject.file("layout.json"))
                                }
                            })
            def that = this

            if (findExistingTask(myProject) != null) {
                myProject.logger.warn("Using more than one ${this.class.name} for project [$myProject.path] is not expected")
            } else {
                myProject.tasks.register("processIdeaSettings", ProcessIdeaFilesWithServiceTask,
                        new Action<ProcessIdeaFilesWithServiceTask>() {
                            @Override
                            void execute(ProcessIdeaFilesWithServiceTask task) {
                                task.service.set(serviceProvider)
                                task.myProcessor = that
                            }
                        }
                )
            }
        } else {
            myProject.tasks.create("processIdeaSettings", ProcessIdeaFilesTask, this)
            installBuildFinishedListener()
        }
    }

    private static TaskProvider<Task> findExistingTask(Project myProject) {
        try {
            return myProject.tasks.named("processIdeaSettings")
        } catch (UnknownTaskException ignore) { }
        return null
    }

    def withIDEAdir(Action<File> callback) {
        ideaDirCallbacks.add(callback)
    }

    def withIDEAFileXml(String relativeFilePath, Action<XmlProvider> callback) {
        put(ideaFileXmlCallbacks, relativeFilePath, callback)
    }

    def withModuleFile(SourceSet s, Action<File> callback) {
        put(imlsCallbacks, getName(s), callback)
    }

    def withModuleXml(SourceSet s, Action<XmlProvider> callback) {
        put(xmlCallbacks, getName(s), callback)
    }

    static <V> void put(Map<String, List<V>> collection, String key, V value) {
        collection.computeIfAbsent(key, { new ArrayList<V>() }).add(value)
    }

    static String getName(SourceSet s) {
        return s == null ? "" : "$s.name"
    }

    def process() {
        process(myProject.rootProject.file("layout.json"))
    }

    def process(File layoutFile) {
        def gson = new Gson()
        def file = layoutFile
        if (!file.exists()) {
            myProject.logger.lifecycle("IDEA layout file 'layout.json' was not found, terminating.")
            return
        }

        final IdeaLayoutJson layout = gson.fromJson(file.text, IdeaLayoutJson)
        def ideaDir = new File(layout.ideaDirPath)
        ideaDirCallbacks.each { it.execute(ideaDir) }

        ideaFileXmlCallbacks.each { entry ->
            def ideaFile = new File(ideaDir, entry.getKey())
            def transformer = new XmlTransformer()
            entry.value.each {transformer.addAction(it) }
            def result = transformer.transform(ideaFile.text)
            ideaFile.write(result)
        }

        imlsCallbacks.each { entry ->
            def imlPath = lookUpImlPath(layout, entry.key)
            if (imlPath == null) {
                myProject.logger.warn("No path to iml is present for key [${entry.key}].\nLayout: $layout")
                return
            }
            def moduleFile = new File(imlPath)
            entry.value.each { it.execute(moduleFile) }
        }

        xmlCallbacks.each { entry ->
            String imlPath = lookUpImlPath(layout, entry.key)
            if (imlPath == null) {
                myProject.logger.warn("No path to iml is present for key [${entry.key}].\\nLayout: $layout")
                return
            }
            def moduleFile = new File(imlPath)
            def transformer = new XmlTransformer()
            entry.value.each { transformer.addAction(it) }
            def result = transformer.transform(moduleFile.text)
            moduleFile.write(result)
        }
    }

    private String lookUpImlPath(IdeaLayoutJson layout, String entryKey) {
        def result = searchByProjectName(layout, entryKey)
        if (result != null) {
            return result
        }
        return searchByProjectPath(layout, entryKey)
    }

    private String searchByProjectName(IdeaLayoutJson layout, String entryKey) {
        def key = myProject.name
        if (entryKey != "") {
            key = key + ":${entryKey}"
        }
        return layout.modulesMap.get(key)
    }

    private String searchByProjectPath(IdeaLayoutJson layout, String entryKey) {
        def key = myProject.path == ":" ? myProject.name : myProject.path
        if (entryKey != "") {
            key = key + ":${entryKey}"
        }
        return layout.modulesMap.get(key)
    }

    boolean hasPostprocessors() {
        return !ideaDirCallbacks.isEmpty() ||
                !imlsCallbacks.isEmpty() ||
                !ideaFileXmlCallbacks.isEmpty() ||
                !xmlCallbacks.isEmpty()
    }

    @Synchronized
    void installBuildFinishedListener() {
        def listener = new FilesProcessorBuildListener()
        myProject.gradle.addBuildListener(listener)
    }
}

class ProcessIdeaFilesTask extends DefaultTask {
    @Internal
    IdeaFilesProcessor myProcessor

    @Inject
    ProcessIdeaFilesTask(IdeaFilesProcessor processor) {
        myProcessor = processor
    }
    @TaskAction
    def process() {
        myProcessor.process()
    }
}

abstract class ProcessIdeaFilesWithServiceTask extends DefaultTask {
    @Internal
    IdeaFilesProcessor myProcessor

    @Internal
    abstract Property<LayoutFileBuildService> getService();

    @TaskAction
    def process() {
        myProcessor.process(getService().get().getFilePath())
    }
}


class IdeaLayoutJson {
    public String ideaDirPath
    public Map<String, String> modulesMap
}

class FilesProcessorBuildListener implements  BuildListener {
    @Override
    void buildStarted(org.gradle.api.invocation.Gradle gradle) {
    }

    @Override
    void settingsEvaluated(Settings settings) {
    }

    @Override
    void projectsLoaded(org.gradle.api.invocation.Gradle gradle) {
    }

    @Override
    void projectsEvaluated(org.gradle.api.invocation.Gradle gradle) {
    }

    @Override
    void buildFinished(BuildResult buildResult) {
        def file = buildResult.getGradle().getRootProject().file("layout.json")
        if (file.exists()) {
            file.delete()
        }
    }
}

