package org.jetbrains.gradle.ext

import groovy.transform.CompileStatic

import javax.inject.Inject

@CompileStatic
class Inspection implements MapConvertible {
    final String name
    Boolean enabled

    @Inject
    Inspection(String name) {
        this.name = name
    }

    @Override
    Map<String, ?> toMap() {
        return [
                "enabled": enabled,
                "name"   : name
        ]
    }
}
