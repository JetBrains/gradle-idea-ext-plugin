import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.gradle.ext.SerializationUtil
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class IdeaModelExtensionOnKotlinBuildFileFunctionalTest extends Specification {
    @Rule
    TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile
    File settingsFile

    static List<String> gradleVersionList = ["5.0", "5.6.4", "6.0", "6.8.3", "7.0", "7.2", "7.3"]

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle.kts')
        settingsFile = testProjectDir.newFile('settings.gradle.kts')
    }

    def "test compiler settings"() {
        given:
        settingsFile << """
rootProject.name = "ProjectName"
"""
        // language=kotlin
        buildFile << """
      import org.jetbrains.gradle.ext.*

      plugins {
        id ("org.jetbrains.gradle.plugin.idea-ext")
      }
      idea {
        settings {
          compiler {
            processHeapSize = 8192
            parallelCompilation = false
            javac {
              javacAdditionalOptions = listOf(
                "-Werror",
                "-Xlint:all",
                "-Xlint:-options",
                "-Xlint:-serial",
                "-proc:none",
                "-Xdiags:verbose",
                "-parameters"
              ).joinToString(separator = " ")
            }
          }
        }
      }

      tasks.register("printSettings") {
        doLast {
          println(project.idea.settings.toString())
        }
      }
    """
        when:
        def result = GradleRunner.create()
            .withGradleVersion(gradleVersion)
            .withProjectDir(testProjectDir.root)
            .withArguments("printSettings", "-q")
            .withPluginClasspath()
            .build()
        then:

        def lines = result.output.readLines()
        def prettyOutput = SerializationUtil.prettyPrintJsonStr(lines[0])
        prettyOutput.contains(
            """"compiler": {
        "processHeapSize": 8192,
        "parallelCompilation": false,
        "javacOptions": {
            "javacAdditionalOptions": "-Werror -Xlint:all -Xlint:-options -Xlint:-serial -proc:none -Xdiags:verbose -parameters"
        }
    }""")

        result.task(":printSettings").outcome == TaskOutcome.SUCCESS
        where:
        gradleVersion << gradleVersionList
    }

}
