package org.jetbrains.gradle.ext

import groovy.lang.Closure
import org.gradle.util.ConfigureUtil

enum class ForceBraces { DO_NOT_FORCE, FORCE_BRACES_IF_MULTILINE, FORCE_BRACES_ALWAYS }

open class LanguageCodeStyleConfig {

  var USE_SAME_IDENTS: Boolean? = null
  var CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND: Int? = null
  var RIGHT_MARGIN: Int? = null
  var JD_ALIGN_PARAM_COMMENTS: Boolean? = null
  var JD_ALIGN_EXCEPTION_COMMENTS: Boolean? = null
  var JD_P_AT_EMPTY_LINES: Boolean? = null
  var JD_KEEP_EMPTY_PARAMETER: Boolean? = null
  var JD_KEEP_EMPTY_EXCEPTION: Boolean? = null
  var JD_KEEP_EMPTY_RETURN: Boolean? = null
  var WRAP_COMMENTS: Boolean? = null

  var IF_BRACE_FORCE: ForceBraces? = null
  var DOWHILE_BRACE_FORCE: ForceBraces? = null
  var WHILE_BRACE_FORCE: ForceBraces? = null
  var FOR_BRACE_FORCE: ForceBraces? = null

  var KEEP_CONTROL_STATEMENT_IN_ONE_LINE: Boolean? = null
  var ALIGN_NAMED_ARGS_IN_MAP: Boolean? = null
}


class CodeStyleConfig: LanguageCodeStyleConfig() {
  val languages: MutableMap<String, LanguageCodeStyleConfig> = mutableMapOf()

  fun java(closure: Closure<*>) {
    ConfigureUtil.configure(closure,
            languages.getOrPut("java", { LanguageCodeStyleConfig() }))
  }

  fun groovy(closure: Closure<*>) {
    ConfigureUtil.configure(closure,
            languages.getOrPut("groovy", { LanguageCodeStyleConfig() }))
  }
}