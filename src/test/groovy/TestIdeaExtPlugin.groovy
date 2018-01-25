import groovy.json.JsonOutput
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.gradle.ext.runConfigurations.Application
import org.jetbrains.gradle.ext.runConfigurations.JUnit
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class IdeaModelExtensionFunctionalTest extends Specification {
  @Rule
  final TemporaryFolder testProjectDir = new TemporaryFolder()
  File buildFile
  File settingsFile

  def setup() {
    buildFile = testProjectDir.newFile('build.gradle')
    settingsFile = testProjectDir.newFile('settings.gradle')
  }

  def "test project settings"() {
    given:
    settingsFile << """
rootProject.name = "ProjectName"
"""
    // language=groovy
    buildFile << """
      import org.jetbrains.gradle.ext.runConfigurations.*

      plugins {
          id 'org.jetbrains.gradle.plugin.idea-ext'
      }
      
      idea {
        project {
          settings {
            compiler {
              resourcePatterns '!*.java;!*.class'
            }
            inspections {
                "some" { enabled = true }
            }
            runConfigurations {
                "Run my app"(Application) {
                    mainClass = 'foo.App'
                    workingDirectory = "\$projectDir" 
                    moduleName = getProject().idea.module.name
                }
                "Run my test"(JUnit) {
                    className = 'my.test.className'
                }

                defaults(Application) {
                    jvmArgs = '-DmyKey=myVal'
                }

                defaults(JUnit) {
                    className = 'MyDefaultClass'
                }
            }
          }
        }
      }
      
      
      task printSettings {
        doLast {
          println(projectDir)
          println(project.idea.project.settings)
        }
      }
    """
    when:
    def result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("printSettings", "-q")
            .withPluginClasspath()
            .build()
    then:

    def lines = result.output.readLines()
    def projectDir = lines[0]
    def output = lines[1]
    output.contains('{"compiler":{"resourcePatterns":"!*.java;!*.class"}')
    output.contains('"inspections":[{"enabled":true,"name":"some"}]')

    output.contains(
            '"runConfigurations":[{"type":"application","envs":null,' +
                    '"workingDirectory":' + JsonOutput.toJson(projectDir) + ',"mainClass":"foo.App","moduleName":"ProjectName","beforeRun":[],"jvmArgs":null,"defaults":false,"name":"Run my app","programParameters":null},' +
                    '{"directory":null,"type":"junit","repeat":null,"envs":null,"vmParameters":null,"category":null,"workingDirectory":null,' +
                        '"className":"my.test.className","moduleName":null,"passParentEnvs":null,"packageName":null,"defaults":false,"pattern":null,"name":"Run my test","method":null},' +
                    '{"type":"application","envs":null,"workingDirectory":null,"mainClass":null,"moduleName":null,"beforeRun":[],"jvmArgs":"-DmyKey=myVal","defaults":true,"name":"default_'+ Application.name +'","programParameters":null},' +
                    '{"directory":null,"type":"junit","repeat":null,"envs":null,"vmParameters":null,"category":null,"workingDirectory":null,' +
                        '"className":"MyDefaultClass","moduleName":null,"passParentEnvs":null,"packageName":null,"defaults":true,"pattern":null,"name":"default_'+ JUnit.name +'","method":null}]'
    )

    result.task(":printSettings").outcome == TaskOutcome.SUCCESS
  }

  def "test groovy compiler settings"() {
    given:
    buildFile << """
plugins {
  id 'org.jetbrains.gradle.plugin.idea-ext'
}

idea {
  project {
    settings {
      groovyCompiler {
        heapSize = 2000
        excludes {
          file("/some/myFile")
          dir("/a/dir", true)
        }
      }
    }
  }
}

task printSettings {
  doLast {
    println(project.idea.project.settings)
  }
}
"""

    when:
    def result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("printSettings", "-q")
            .withPluginClasspath()
            .build()
    then:

    def lines = result.output.readLines()
    lines[0] == '{"groovyCompiler":{"heapSize":"2000","excludes":[{"url":"/some/myFile","includeSubdirectories":false,"isFile":true},' +
            '{"url":"/a/dir","includeSubdirectories":true,"isFile":false}]}}'
    
  }

  def "test code style settings"() {
    given:
    buildFile << """
plugins {
  id 'org.jetbrains.gradle.plugin.idea-ext'
}

idea {
  project {
    settings {
      codeStyle {
        RIGHT_MARGIN = 200
        JD_ALIGN_PARAM_COMMENTS = false
        groovy {
          ALIGN_NAMED_ARGS_IN_MAP = false
          CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND = 999
        }
      }
    }
  }
}

task printSettings {
  doLast {
    println(project.idea.project.settings)
  }
}
"""

    when:
    def result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("printSettings", "-q")
            .withPluginClasspath()
            .build()
    then:

    def lines = result.output.readLines()
    JsonOutput.prettyPrint(lines[0]) == """{
    "codeStyle": {
        "WHILE_BRACE_FORCE": null,
        "JD_KEEP_EMPTY_RETURN": null,
        "WRAP_COMMENTS": null,
        "ALIGN_NAMED_ARGS_IN_MAP": null,
        "CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND": null,
        "languages": {
            "groovy": {
                "WHILE_BRACE_FORCE": null,
                "JD_KEEP_EMPTY_RETURN": null,
                "WRAP_COMMENTS": null,
                "ALIGN_NAMED_ARGS_IN_MAP": false,
                "CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND": 999,
                "JD_ALIGN_EXCEPTION_COMMENTS": null,
                "FOR_BRACE_FORCE": null,
                "JD_KEEP_EMPTY_EXCEPTION": null,
                "JD_KEEP_EMPTY_PARAMETER": null,
                "JD_P_AT_EMPTY_LINES": null,
                "DOWHILE_BRACE_FORCE": null,
                "USE_SAME_IDENTS": null,
                "JD_ALIGN_PARAM_COMMENTS": null,
                "KEEP_CONTROL_STATEMENT_IN_ONE_LINE": null,
                "RIGHT_MARGIN": null,
                "IF_BRACE_FORCE": null
            }
        },
        "JD_ALIGN_EXCEPTION_COMMENTS": null,
        "FOR_BRACE_FORCE": null,
        "JD_KEEP_EMPTY_EXCEPTION": null,
        "JD_KEEP_EMPTY_PARAMETER": null,
        "JD_P_AT_EMPTY_LINES": null,
        "DOWHILE_BRACE_FORCE": null,
        "USE_SAME_IDENTS": null,
        "JD_ALIGN_PARAM_COMMENTS": false,
        "RIGHT_MARGIN": 200,
        "KEEP_CONTROL_STATEMENT_IN_ONE_LINE": null,
        "IF_BRACE_FORCE": null
    }
}"""

  }

  def "idea extension plugin can be applied in multiproject build"() {
    given:
    settingsFile << """
    include 'p1', 'p2', 'p3'
"""

    buildFile << """
    plugins {
       id 'org.jetbrains.gradle.plugin.idea-ext' apply false
    }
      
    allprojects {
      apply plugin: 'org.jetbrains.gradle.plugin.idea-ext'
    }
    
"""
    when:
    def result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("projects", "--stacktrace")
            .withPluginClasspath()
            .build()
    then:
    // result.output.contains("p1")
    result.task(":projects").outcome == TaskOutcome.SUCCESS
  }

    def "test module settings"() {
        given:
        buildFile << """
          import org.jetbrains.gradle.ext.facets.*
          
          plugins {
              id 'org.jetbrains.gradle.plugin.idea-ext'
          }
          
          idea {
            module {
              settings {
                  facets {
                      spring(SpringFacet) {
                        contexts {
                          p1 {
                            file = 'spring_parent.xml'
                          }
                          
                          p2 {
                            file = 'spring_child.xml'
                            parent = 'p1'
                          }
                        }
                      }
                    }
              }
            }
          }

          idea.module.settings.facets.spring.contexts.p2.file = 'spring_new_child.xml'
          
          task printSettings {
            doLast {
              println project.projectDir
              println project.idea.module.settings
            }
          }
        """
        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("printSettings", "-q")
                .withPluginClasspath()
                .build()
        then:
        def lines = result.output.readLines()
        lines[1] == '{"facets":[{"type":"spring","contexts":' +
                          '[{"file":"spring_parent.xml","name":"p1","parent":null},' +
                          '{"file":"spring_new_child.xml","name":"p2","parent":"p1"}],"name":"spring"}]}'

        result.task(":printSettings").outcome == TaskOutcome.SUCCESS
    }
}