import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Ignore
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
}