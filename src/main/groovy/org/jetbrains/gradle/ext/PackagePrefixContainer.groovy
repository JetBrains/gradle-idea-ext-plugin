package org.jetbrains.gradle.ext

import groovy.transform.CompileStatic
import org.gradle.plugins.ide.idea.model.IdeaModule

import javax.inject.Inject

@CompileStatic
class PackagePrefixContainer extends LinkedHashMap<String, String> implements MapConvertible {

  private final IdeaModule module

  @Inject
  PackagePrefixContainer(IdeaModule module) {
    this.module = module
  }

  @Override
  Map<String, ?> toMap() {
    def contentRoot = module.contentRoot.absolutePath
    def result = new LinkedHashMap<String, String>()
    for (def entry : this) {
      def path = entry.key
      def prefix = entry.value
      def file = new File(contentRoot, path)
      def sourceDir = file.path.replace(File.separator, '/')
      result.put(sourceDir, prefix)
    }
    return result
  }
}
