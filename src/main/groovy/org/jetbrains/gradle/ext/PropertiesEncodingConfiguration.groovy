package org.jetbrains.gradle.ext

import groovy.transform.CompileStatic

@CompileStatic
class PropertiesEncodingConfiguration implements MapConvertible {

  String encoding = null
  Boolean transparentNativeToAsciiConversion = null

  @Override
  Map<String, ?> toMap() {
    return [
            'encoding'                          : encoding,
            'transparentNativeToAsciiConversion': transparentNativeToAsciiConversion
    ]
  }
}
