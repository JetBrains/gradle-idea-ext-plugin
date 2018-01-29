package org.jetbrains.gradle.ext

import groovy.transform.CompileStatic

@CompileStatic
interface MapConvertible {
  Map<String, ?> toMap()
}