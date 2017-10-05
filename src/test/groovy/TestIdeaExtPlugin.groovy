import groovy.json.JsonOutput
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class IdeaModelExtensionFunctionalTest extends Specification {
  @Rule
  final TemporaryFolder testProjectDir = new TemporaryFolder()
  File buildFile

  def setup() {
    buildFile = testProjectDir.newFile('build.gradle')
  }

  def "idea extension plugin exposes settings as Json"() {
    given:
    // language=groovy
    buildFile << """import org.jetbrains.gradle.ext.facets.SpringFacet
      plugins {
          id 'org.jetbrains.gradle.plugin.idea-ext'
      }
      
      idea {
        project {
          settings {
            compiler {
              resourcePatterns '!*.java;!*.class'
            }
            codeStyle {
              indent 'tabs'
            }
            inspections {
              name 'value'
            }
          }
        }
        module {
          settings {
           facets {
              create('spring', SpringFacet) {
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
          println(project.idea.project.settings)
          println(project.idea.module.settings)
        }
      }
    """
    when:
    def result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("printSettings")
            .withPluginClasspath()
            .build()
    then:
    result.output.contains('{"compiler":{"resourcePatterns":"!*.java;!*.class"},' +
            '"codeStyle":{"indent":"tabs"},' +
            '"inspections":{"name":"value"}}')
    result.output.contains('{"facets":{"spring":{"type":"spring","contexts":' +
            '[{"file":"spring_parent.xml","name":"p1","parent":null},' +
            '{"file":"spring_new_child.xml","name":"p2","parent":"p1"}],"name":"spring"}}}')
    result.task(":printSettings").outcome == TaskOutcome.SUCCESS
  }

  def "idea extension plugin can be applied in multiproject build"() {
    given:
    File settingsFile = testProjectDir.newFile('settings.gradle')
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

    def "gradle project api can be used for the settings construction"() {
        given:
        buildFile << """
          import org.jetbrains.gradle.ext.runConfigurations.*
          
          plugins {
              id 'org.jetbrains.gradle.plugin.idea-ext'
          }
          
          idea {
            module {
              settings {
                  runConfigurations {
                      create('App', Application) {
                          mainClass = 'foo.App'
                          workingDirectory = "\$projectDir" 
                      }
                      create('DoTest', JUnit) {
                          className = 'my.test.className'
                      }
                  }
              }
            }
          }
          
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
        def projectDir = lines[0]
        lines[1] == '{"runConfigurations":{' +
                '"App":{"type":"application",' +
                '"workingDirectory":' + JsonOutput.toJson(projectDir) + ',"mainClass":"foo.App","name":"App"},' +
                '"DoTest":{"type":"junit","className":"my.test.className","name":"DoTest"}' +
                '}}'
        result.task(":printSettings").outcome == TaskOutcome.SUCCESS
    }
}