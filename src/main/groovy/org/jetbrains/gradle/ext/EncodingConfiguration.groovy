package org.jetbrains.gradle.ext

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Project

import javax.inject.Inject

@CompileStatic
class EncodingConfiguration implements MapConvertible {
  enum BomPolicy {
    WITH_BOM, WITH_NO_BOM, WITH_BOM_ON_WINDOWS
  }

  private final Project project

  String encoding = null
  BomPolicy bomPolicy = null
  Map<String, String> mapping = [:]
  final PropertiesEncodingConfiguration properties = new PropertiesEncodingConfiguration()

  @Inject
  EncodingConfiguration(Project project) {
    this.project = project
  }

  void properties(Action<? super PropertiesEncodingConfiguration> action) {
    action.execute(properties)
  }

  @Override
  Map<String, ?> toMap() {
    def contentRoot = project.projectDir.absolutePath
    def absoluteMapping = new LinkedHashMap<String, String>()
    for (def entry : mapping) {
      def path = entry.key
      def prefix = entry.value
      def file = new File(contentRoot, path)
      def sourceDir = file.path.replace(File.separator, '/')
      absoluteMapping.put(sourceDir, prefix)
    }
    return [
            'encoding'  : encoding,
            'bomPolicy' : bomPolicy?.toString(),
            'properties': nullIfEmpty(properties.toMap()),
            'mapping'   : nullIfEmpty(absoluteMapping)
    ]
  }

  static <K, V> Map<K, V> nullIfEmpty(Map<K, V> map) {
    def result = map.findAll { it.value != null }
    return result.isEmpty() ? null : result
  }
}
