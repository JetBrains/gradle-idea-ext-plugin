package org.jetbrains.gradle.ext

import com.google.common.collect.ListMultimap
import com.google.common.collect.Multimaps
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskCollection

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
      List<Task> tasks = (List<Task>) tasksObjects.collect { resolveTasks(it) }.findAll { !it.isEmpty() }.flatten()
      def taskInfos = tasks.collect { task -> ["taskPath" : task.path, "projectPath" : task.project.rootProject.projectDir.path.replaceAll("\\\\", "/")] }
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