package org.jetbrains.gradle.ext

import groovy.transform.CompileStatic

import javax.inject.Inject

@CompileStatic
class Inspection {
    final String name
    boolean enabled = false

    @Inject
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
