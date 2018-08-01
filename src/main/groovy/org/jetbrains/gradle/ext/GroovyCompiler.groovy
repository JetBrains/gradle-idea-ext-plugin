package org.jetbrains.gradle.ext

import groovy.transform.CompileStatic
import org.gradle.api.Action

@CompileStatic
class GroovyCompilerConfiguration implements MapConvertible {
    ExcludesConfig excludes

    def excludes(Action<ExcludesConfig> action) {
        if (excludes == null) {
            excludes = new ExcludesConfig()
        }
        action.execute(excludes)
    }

    @Override
    Map<String, ?> toMap() {
        return ["excludes": excludes?.data]
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
