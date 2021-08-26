package org.jetbrains.gradle.ext

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction

import javax.inject.Inject

class IdeaFilesProcessor {
    Project myProject
    List<File> myCallbacks = new ArrayList<>()

    IdeaFilesProcessor(Project project) {
        myProject = project
    }

    def ideaDirCallback(Action callback) {}
    def withModuleFile(SourceSet s = null, Action callback) {}
    def process() {}
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
