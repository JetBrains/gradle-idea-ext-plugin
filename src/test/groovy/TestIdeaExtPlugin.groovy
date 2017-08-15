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
          }
        }
        module {
          settings {
           facetManager {
              facet(type: "Spring", name: "Spring") {
                  descriptorXml 'file.xml'
              }
            }
          }
        }
      }
      
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
    result.output.contains('{"compiler":{"resourcePatterns":"!*.java;!*.class"}}')
    result.output.contains('{"facetManager":{"facet":[{"type":"Spring","name":"Spring"},{"descriptorXml":"file.xml"}]}}')
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
}