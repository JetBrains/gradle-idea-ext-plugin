package org.jetbrains.gradle.ext

import com.google.gson.Gson
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.xml.XmlTransformer

import javax.inject.Inject

@CompileStatic
class IdeaFilesProcessor {
    Project myProject
    List<Action<File>> ideaDirCallbacks = new ArrayList<>()
    Map<String, Action<XmlProvider>> ideaFileXmlCallbacks = new HashMap<>()
    Map<String, Action<File>> imlsCallbacks = new HashMap<>()
    Map<String, Action<XmlProvider>> xmlCallbacks = new HashMap<>()

    IdeaFilesProcessor(Project project) {
        myProject = project
    }
    def withIDEAdir(Action<File> callback) {
        ideaDirCallbacks.add(callback)
    }

    def withIDEAFileXml(String relativeFilePath, Action<XmlProvider> callback) {
        ideaFileXmlCallbacks.put(relativeFilePath, callback)
    }

    def withModuleFile(SourceSet s, Action<File> callback) {
        imlsCallbacks.put(getName(s), callback)
    }

    def withModuleXml(SourceSet s, Action<XmlProvider> callback) {
        xmlCallbacks.put(getName(s), callback)
    }

    def String getName(SourceSet s) {
        def projectPath = myProject.path == ":" ? myProject.name : myProject.path
        return s == null ? projectPath : "$projectPath:$s.name"
    }

    def process() {
        def gson = new Gson()
        def file = myProject.file("layout.json")
        if (!file.exists()) {
            myProject.logger.lifecycle("IDEA layout file 'layout.json' was not found, terminating.")
            return
        }
        try {
            def layout = gson.fromJson(file.text, IdeaLayoutJson)
            def ideaDir = new File(layout.ideaDirPath)
            ideaDirCallbacks.each { it.execute(ideaDir) }

            ideaFileXmlCallbacks.each { entry ->
                def ideaFile = new File(ideaDir, entry.getKey())
                def transformer = new XmlTransformer()
                transformer.addAction(entry.value)
                def result = transformer.transform(ideaFile.text)
                ideaFile.write(result)
            }

            imlsCallbacks.each { entry ->
                def moduleFile = new File(layout.modulesMap.get(entry.key))
                entry.value.execute(moduleFile)
            }

            xmlCallbacks.each { entry ->
                def moduleFile = new File(layout.modulesMap.get(entry.key))
                def transformer = new XmlTransformer()
                transformer.addAction(entry.value)
                def result = transformer.transform(moduleFile.text)
                moduleFile.write(result)
            }
        } finally {
            file.delete()
        }
    }

    boolean hasPostprocessors() {
        return !ideaDirCallbacks.isEmpty() || !imlsCallbacks.isEmpty()
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


class IdeaLayoutJson {
    public String ideaDirPath
    public Map<String, String> modulesMap
}