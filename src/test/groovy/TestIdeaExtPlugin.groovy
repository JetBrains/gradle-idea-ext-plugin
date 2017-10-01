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
    buildFile << """
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
              spring {
                  descriptorXml 'file.xml'
                  priority 1
                  flag false
              }
            }
          }
        }
      }
      
      idea.module.settings.facets.spring.priority = 2
      
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
    result.output.contains('{"facets":{"spring":{"descriptorXml":"file.xml","priority":2,"flag":false}}}')
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
                  facets {
                      SomeFacet {
                        type 'unknown'
                        moduleName "\$idea.module.name"
                      }
                  }
                  runConfigurations {
                      create('App', Application) { o ->
                          o.mainClass = 'foo.App'
                          o.workingDirectory = "\$projectDir" 
                      }
                      create('DoTest', JUnit) { o ->
                          o.className = 'my.test.className'
                      }
                  }
              }
            }
          }
          
          task printSettings {
            doLast {
              println project.projectDir
              println project.idea.module.name
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
        def moduleName = lines[1]
        lines[2] == '{"facets":{"SomeFacet":{"type":"unknown","moduleName":' + JsonOutput.toJson(moduleName)+ '}},' +
                '"runConfigurations":{' +
                '"App":{"type":"Application",' +
                '"workingDirectory":' + JsonOutput.toJson(projectDir) + ',"mainClass":"foo.App","name":"App"},' +
                '"DoTest":{"type":"JUnit","className":"my.test.className","name":"DoTest"}' +
                '}}'
        result.task(":printSettings").outcome == TaskOutcome.SUCCESS
    }
}