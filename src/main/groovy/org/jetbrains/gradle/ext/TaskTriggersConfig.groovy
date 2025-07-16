package org.jetbrains.gradle.ext

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskCollection

class TaskTriggersConfig implements MapConvertible {

  Project project
  Map<String, List<Object>> phaseMap = new LinkedHashMap<>()

  TaskTriggersConfig(Project project) {
    this.project = project
  }

  void beforeSync(Object... tasks) {
    phaseMap.computeIfAbsent("beforeSync", {new ArrayList<Object>() }).addAll(tasks)
  }
  void afterSync(Object... tasks) {
    phaseMap.computeIfAbsent("afterSync", {new ArrayList<Object>() }).addAll(tasks)
  }
  void beforeBuild(Object... tasks) {
    phaseMap.computeIfAbsent("beforeBuild", {new ArrayList<Object>() }).addAll(tasks)
  }
  void afterBuild(Object... tasks) {
    phaseMap.computeIfAbsent("afterBuild", {new ArrayList<Object>() }).addAll(tasks)
  }
  void beforeRebuild(Object... tasks) {
    phaseMap.computeIfAbsent("beforeRebuild", {new ArrayList<Object>() }).addAll(tasks)
  }
  void afterRebuild(Object... tasks) {
    phaseMap.computeIfAbsent("afterRebuild", {new ArrayList<Object>() }).addAll(tasks)
  }

  @Override
  Map<String, ?> toMap() {
    def result = new LinkedHashMap<String, Object>()
    phaseMap.keySet().each { String phase ->
      List<Object> tasksObjects = phaseMap.get(phase)
      List<Task> tasks = (List<Task>) tasksObjects.collect { resolveTasks(it) }.findAll { !it.isEmpty() }.flatten()
      def taskInfos = tasks.collect { task -> ["taskPath" : task.name, "projectPath" : task.project.projectDir.path.replaceAll("\\\\", "/")] }
      result.put(phase, taskInfos)
    }
    return result
  }

  List<? extends Task> resolveTasks(Object taskObject) {
    if (taskObject instanceof Task) {
      return Collections.singletonList((Task)taskObject)
    } else if (taskObject instanceof Provider) {
      return resolveTasks(((Provider)taskObject).get())
    } else if (taskObject instanceof String) {
      return Collections.singletonList(project.tasks.findByPath(taskObject));
    } else if (taskObject instanceof TaskCollection) {
      return ((TaskCollection)taskObject).asList()
    } else {
      return Collections.emptyList()
    }
  }
}