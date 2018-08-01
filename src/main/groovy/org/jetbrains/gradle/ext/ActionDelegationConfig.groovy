package org.jetbrains.gradle.ext

import groovy.transform.CompileStatic

@CompileStatic
class ActionDelegationConfig implements MapConvertible {
  enum TestRunner { PLATFORM, GRADLE, CHOOSE_PER_TEST }
  Boolean delegateBuildRunToGradle
  TestRunner testRunner

  Map<String, ?> toMap() {
    return ["delegateBuildRunToGradle": delegateBuildRunToGradle,  "testRunner": testRunner]
  }
}