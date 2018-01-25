package org.jetbrains.gradle.ext

import groovy.transform.CompileStatic
import org.gradle.api.Action

@CompileStatic
enum ForceBraces {
    DO_NOT_FORCE, FORCE_BRACES_IF_MULTILINE, FORCE_BRACES_ALWAYS
}

@CompileStatic
class LanguageCodeStyleConfig {

    public Boolean USE_SAME_IDENTS
    public Integer CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND
    public Integer RIGHT_MARGIN
    public Boolean JD_ALIGN_PARAM_COMMENTS
    public Boolean JD_ALIGN_EXCEPTION_COMMENTS
    public Boolean JD_P_AT_EMPTY_LINES
    public Boolean JD_KEEP_EMPTY_PARAMETER
    public Boolean JD_KEEP_EMPTY_EXCEPTION
    public Boolean JD_KEEP_EMPTY_RETURN
    public Boolean WRAP_COMMENTS

    public ForceBraces IF_BRACE_FORCE
    public ForceBraces DOWHILE_BRACE_FORCE
    public ForceBraces WHILE_BRACE_FORCE
    public ForceBraces FOR_BRACE_FORCE

    public Boolean KEEP_CONTROL_STATEMENT_IN_ONE_LINE
    public Boolean ALIGN_NAMED_ARGS_IN_MAP

    protected Map<String, ?> asMap() {
        return [
                "WHILE_BRACE_FORCE"                  : WHILE_BRACE_FORCE,
                "JD_KEEP_EMPTY_RETURN"               : JD_KEEP_EMPTY_RETURN,
                "WRAP_COMMENTS"                      : WRAP_COMMENTS,
                "ALIGN_NAMED_ARGS_IN_MAP"            : ALIGN_NAMED_ARGS_IN_MAP,
                "CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND": CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND,
                "JD_ALIGN_EXCEPTION_COMMENTS"        : JD_ALIGN_EXCEPTION_COMMENTS,
                "FOR_BRACE_FORCE"                    : FOR_BRACE_FORCE,
                "JD_KEEP_EMPTY_EXCEPTION"            : JD_KEEP_EMPTY_EXCEPTION,
                "JD_KEEP_EMPTY_PARAMETER"            : JD_KEEP_EMPTY_PARAMETER,
                "JD_P_AT_EMPTY_LINES"                : JD_P_AT_EMPTY_LINES,
                "DOWHILE_BRACE_FORCE"                : DOWHILE_BRACE_FORCE,
                "USE_SAME_IDENTS"                    : USE_SAME_IDENTS,
                "JD_ALIGN_PARAM_COMMENTS"            : JD_ALIGN_PARAM_COMMENTS,
                "KEEP_CONTROL_STATEMENT_IN_ONE_LINE" : KEEP_CONTROL_STATEMENT_IN_ONE_LINE,
                "RIGHT_MARGIN"                       : RIGHT_MARGIN,
                "IF_BRACE_FORCE"                     : IF_BRACE_FORCE
        ]
    }
}

@CompileStatic
class CodeStyleConfig extends LanguageCodeStyleConfig {

    Map<String, LanguageCodeStyleConfig> languages = [:]

    def java(Action<LanguageCodeStyleConfig> action) {
        if (!languages["java"]) {
            languages["java"] = new LanguageCodeStyleConfig()
        }
        action.execute(languages["java"])
    }

    def groovy(Action<LanguageCodeStyleConfig> action) {
        if (!languages["groovy"]) {
            languages["groovy"] = new LanguageCodeStyleConfig()
        }
        action.execute(languages["groovy"])
    }

    Map<String, ?> toMap() {
        def map = asMap()
        map["languages"] = languages.collectEntries { key, value -> [(key): value.asMap()] }
        return map
    }
}
