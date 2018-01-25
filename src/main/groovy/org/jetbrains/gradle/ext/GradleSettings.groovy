package org.jetbrains.gradle.ext

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
