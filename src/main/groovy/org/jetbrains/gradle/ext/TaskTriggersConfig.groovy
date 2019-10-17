package org.jetbrains.gradle.ext

import com.google.common.collect.ListMultimap
import com.google.common.collect.Multimaps
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Provider

@CompileStatic
class TaskTriggersConfig implements MapConvertible {

  Project project
  ListMultimap<String, Object> phaseMap = Multimaps
          .newListMultimap(new LinkedHashMap<String, Collection<Object>>(),
          { new ArrayList<Object>()})

  TaskTriggersConfig(Project project) {
    this.project = project
  }

  void beforeSync(Object... tasks) {
    phaseMap.putAll("beforeSync", Arrays.asList(tasks))
  }
  void afterSync(Object... tasks) {
    phaseMap.putAll("afterSync", Arrays.asList(tasks))
  }
  void beforeBuild(Object... tasks) {
    phaseMap.putAll("beforeBuild", Arrays.asList(tasks))
  }
  void afterBuild(Object... tasks) {
    phaseMap.putAll("afterBuild", Arrays.asList(tasks))
  }
  void beforeRebuild(Object... tasks) {
    phaseMap.putAll("beforeRebuild", Arrays.asList(tasks))
  }
  void afterRebuild(Object... tasks) {
    phaseMap.putAll("afterRebuild", Arrays.asList(tasks))
  }

  @Override
  Map<String, ?> toMap() {
    def result = new LinkedHashMap<String, Object>()
    phaseMap.keySet().each { String phase ->
      List<Object> tasksObjects = phaseMap.get(phase)
      List<Task> tasks = tasksObjects.collect { resolveTask(it) }.findAll { it != null }
      def taskInfos = tasks.collect { task -> ["taskPath" : task.name, "projectPath" : task.project.projectDir.path.replaceAll("\\\\", "/")] }
      result.put(phase, taskInfos)
    }
    return result
  }

  Task resolveTask(Object taskObject) {
    if (taskObject instanceof Task) {
      return (Task)taskObject
    } else if (taskObject instanceof Provider) {
      return resolveTask(((Provider)taskObject).get())
    } else if (taskObject instanceof String) {
      return project.tasks.findByPath(taskObject)
    } else {
      return null
    }
  }
}