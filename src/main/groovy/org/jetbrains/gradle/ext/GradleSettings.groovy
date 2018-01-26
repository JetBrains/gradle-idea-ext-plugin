package org.jetbrains.gradle.ext

import groovy.transform.CompileStatic

@CompileStatic
class GradleIDESettings {
    String sdkHome

    def toMap() {
        def map = [:]
        if (sdkHome) {
            map.put("sdkHome", sdkHome)
        }
        return map
    }
}
