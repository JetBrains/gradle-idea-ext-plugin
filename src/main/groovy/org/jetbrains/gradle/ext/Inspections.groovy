package org.jetbrains.gradle.ext

class Inspection {
    final String name
    boolean enabled = false

    Inspection(String name) {
        this.name = name
    }

    def toMap() {
        return [
                "enabled": enabled,
                "name"   : name
        ]
    }
}
