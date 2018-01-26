package org.jetbrains.gradle.ext

import groovy.transform.CompileStatic
import org.gradle.api.Action

@CompileStatic
class GroovyCompilerConfiguration {
    Integer heapSize
    final ExcludesConfig excludes = new ExcludesConfig()

    def excludes(Action<ExcludesConfig> action) {
        action.execute(excludes)
    }

    def toMap() {
        return ["heapSize": heapSize.toString(), "excludes": excludes.data]
    }
}

@CompileStatic
class ExcludesConfig {
    final List<Map<String, ?>> data = []

    def file(String path) {
        data.add([
                "url"                  : path,
                "includeSubdirectories": false,
                "isFile"               : true
        ])
    }

    def dir(String path, Boolean includeSubdirectories = false) {
        data.add([
                "url"                  : path,
                "includeSubdirectories": includeSubdirectories,
                "isFile"               : false
        ])
    }
}
