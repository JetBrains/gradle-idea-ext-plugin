package org.jetbrains.gradle.ext

import groovy.transform.CompileStatic
import org.gradle.api.Action

/**
 * @deprecated Use .editorconfig to defined codestyle
 */
@CompileStatic
enum ForceBraces {
    DO_NOT_FORCE, FORCE_BRACES_IF_MULTILINE, FORCE_BRACES_ALWAYS
}

/**
 * @deprecated Use .editorconfig to defined codestyle
 */
@CompileStatic
class CommonCodeStyleConfig implements MapConvertible {

    public Integer hardWrapAt
    public Boolean wrapCommentsAtRightMargin
    public ForceBraces ifForceBraces
    public ForceBraces doWhileForceBraces
    public ForceBraces whileForceBraces
    public ForceBraces forForceBraces
    public Boolean keepControlStatementInOneLine

    @CompileStatic
    Map<String, ?> toMap() {
        return [
                "RIGHT_MARGIN"                       : hardWrapAt,
                "WRAP_COMMENTS"                      : wrapCommentsAtRightMargin,
                "IF_BRACE_FORCE"                     : ifForceBraces,
                "DOWHILE_BRACE_FORCE"                : doWhileForceBraces,
                "WHILE_BRACE_FORCE"                  : whileForceBraces,
                "FOR_BRACE_FORCE"                    : forForceBraces,
                "KEEP_CONTROL_STATEMENT_IN_ONE_LINE" : keepControlStatementInOneLine
        ]
    }
}

/**
 * @deprecated Use .editorconfig to defined codestyle
 */
class JavaCodeStyleConfig extends CommonCodeStyleConfig {
    public Integer classCountToUseImportOnDemand
    public Boolean alignParameterDescriptions
    public Boolean alignThrownExceptionDescriptions
    public Boolean generatePTagOnEmptyLines
    public Boolean keepEmptyParamTags
    public Boolean keepEmptyThrowsTags
    public Boolean keepEmptyReturnTags

    @CompileStatic
    @Override
    Map<String, ?> toMap() {
        def result = super.toMap()
        return result << ([
                "CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND" : classCountToUseImportOnDemand,
                "JD_ALIGN_PARAM_COMMENTS"             : alignParameterDescriptions,
                "JD_ALIGN_EXCEPTION_COMMENTS"         : alignThrownExceptionDescriptions,
                "JD_P_AT_EMPTY_LINES"                 : generatePTagOnEmptyLines,
                "JD_KEEP_EMPTY_PARAMETER"             : keepEmptyParamTags,
                "JD_KEEP_EMPTY_EXCEPTION"             : keepEmptyThrowsTags,
                "JD_KEEP_EMPTY_RETURN"                : keepEmptyReturnTags
        ] as Map<String, ?>)
    }
}

/**
 * @deprecated Use .editorconfig to defined codestyle
 */
class GroovyCodeStyleConfig extends CommonCodeStyleConfig {
    public Integer classCountToUseImportOnDemand
    public Boolean alignMultilineNamedArguments

    @CompileStatic
    @Override
    Map<String, ?> toMap() {
        def result = super.toMap()
        return result << ([
                "CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND" : classCountToUseImportOnDemand,
                "ALIGN_NAMED_ARGS_IN_MAP"             : alignMultilineNamedArguments
        ] as Map<String, ?>)
    }
}

/**
 * @deprecated Use .editorconfig to defined codestyle
 */
@CompileStatic
class CodeStyleConfig implements MapConvertible {

    @Deprecated
    public Boolean USE_SAME_INDENTS
    public Integer hardWrapAt
    public Boolean keepControlStatementInOneLine

    Map<String, CommonCodeStyleConfig> languages = [:]

    def java(Action<JavaCodeStyleConfig> action) {
        if (!languages["java"]) {
            languages["java"] = new JavaCodeStyleConfig()
        }
        action.execute(languages["java"] as JavaCodeStyleConfig)
    }

    def groovy(Action<GroovyCodeStyleConfig> action) {
        if (!languages["groovy"]) {
            languages["groovy"] = new GroovyCodeStyleConfig()
        }
        action.execute(languages["groovy"] as GroovyCodeStyleConfig)
    }

    @CompileStatic
    Map<String, ?> toMap() {
        def map = [
                "USE_SAME_INDENTS"                   : USE_SAME_INDENTS,
                "RIGHT_MARGIN"                       : hardWrapAt,
                "KEEP_CONTROL_STATEMENT_IN_ONE_LINE" : keepControlStatementInOneLine
        ] as Map<String, Object>

        def languages = this.languages.collectEntries { key, value ->
            [(key): value.toMap()].findAll { !it.value.isEmpty() }
        }
        if (!languages.isEmpty()) {
            map["languages"] = languages
        }
        return map
    }
}
