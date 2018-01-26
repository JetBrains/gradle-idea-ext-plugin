package org.jetbrains.gradle.ext

import groovy.transform.CompileStatic

@CompileStatic
class FrameworkDetectionExclusionSettings {
    List<String> excludes = []
}