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
      import org.jetbrains.gradle.ext.*

      plugins {
          id 'org.jetbrains.gradle.plugin.idea-ext'
      }
      
      idea {
        project {
          settings {
            compiler.resourcePatterns '!*.java;!*.class'
            inspections {
                "some" { enabled = true }
            }
            runConfigurations {
                "Run my app"(Application) {
                    mainClass = 'foo.App'
                    workingDirectory = "\$projectDir" 
                    moduleName = getProject().idea.module.name
                }
                defaults(Application) {
                    jvmArgs = '-DmyKey=myVal'
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
    def prettyOutput = JsonOutput.prettyPrint(lines[1])
    prettyOutput.contains(
""""compiler": {
        "resourcePatterns": "!*.java;!*.class"
    }""")
    prettyOutput.contains(
""""inspections": [
        {
            "enabled": true,
            "name": "some"
        }
    ]""")

     prettyOutput.contains(
""""runConfigurations": [
        {
            "type": "application",
            "workingDirectory": ${JsonOutput.toJson(projectDir)},
            "mainClass": "foo.App",
            "moduleName": "ProjectName",
            "beforeRun": [
                
            ],
            "defaults": false,
            "name": "Run my app"
        },
        {
            "type": "application",
            "beforeRun": [
                
            ],
            "jvmArgs": "-DmyKey=myVal",
            "defaults": true,
            "name": "default_org.jetbrains.gradle.ext.Application"
        }
    ]
""")

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
    JsonOutput.prettyPrint(lines[0]) ==
"""{
    "codeStyle": {
        "JD_ALIGN_PARAM_COMMENTS": false,
        "RIGHT_MARGIN": 200,
        "languages": {
            "groovy": {
                "ALIGN_NAMED_ARGS_IN_MAP": false,
                "CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND": 999
            }
        }
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
          import org.jetbrains.gradle.ext.*
          
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


  def "test extending the DSL with simple class"() {
    given:
    buildFile << """
import org.jetbrains.gradle.ext.*
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.*

class TestExtSettings implements MapConvertible {
  def name = "value"
  Map<String, Object> toMap() { return [ "name": name ] } 
}

class TestPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.apply plugin: 'org.jetbrains.gradle.plugin.idea-ext'
        def ideaModel = project.extensions.findByName('idea') as IdeaModel
    
        def projectSettings = (ideaModel.project as ExtensionAware).extensions.findByName("settings") as ExtensionAware
        projectSettings.extensions.create("myTest", TestExtSettings)
        
        def moduleSettings = (ideaModel.module as ExtensionAware).extensions.findByName("settings") as ExtensionAware
        moduleSettings.extensions.create("myTest", TestExtSettings)
    }
}

plugins {
  id 'org.jetbrains.gradle.plugin.idea-ext'
}

// Apply the plugin
apply plugin: TestPlugin

idea {
  project {
    settings {
      myTest {
        name = "test_project_value"
      }
    }
  }
  module.settings.myTest.name = "test_module_value"
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
            .withArguments("printSettings", "-q")
            .withPluginClasspath()
            .build()
    then:

    def lines = result.output.readLines()
    lines[0] == '{"myTest":{"name":"test_project_value"}}'
    lines[1] == '{"myTest":{"name":"test_module_value"}}'

  }


  def "test extending the DSL with NameObjectContainer"() {
    given:
    buildFile << """
import org.jetbrains.gradle.ext.*
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.*

class TestExtSettings implements MapConvertible, Named {
  private String name = "value"
  def param = 0
  
  def TestExtSettings(String constructedName) {
    name = constructedName
  }
  
  String getName() { return name }
  
  Map<String, Object> toMap() { return [ "name": name, "param": param ] } 
}

class TestPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.apply plugin: 'org.jetbrains.gradle.plugin.idea-ext'
        def ideaModel = project.extensions.findByName('idea') as IdeaModel
    
        def projectSettings = (ideaModel.project as ExtensionAware).extensions.findByName("settings") as ExtensionAware
        projectSettings.extensions.add("myTest", project.container(TestExtSettings))
        
        def moduleSettings = (ideaModel.module as ExtensionAware).extensions.findByName("settings") as ExtensionAware
        moduleSettings.extensions.add("myTest", project.container(TestExtSettings))
    }
}

plugins {
  id 'org.jetbrains.gradle.plugin.idea-ext'
}

// Apply the plugin
apply plugin: TestPlugin

idea {
  project {
    settings {
      myTest {
        "projectName1" {
          param = 1
        }
        "projectName2" {
          param = 2
        }
      }
    }
  }
  module.settings.myTest {
    "moduleName1" {
      param = 1
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
            .withArguments("printSettings", "-q")
            .withPluginClasspath()
            .build()
    then:

    def lines = result.output.readLines()
    lines[0] == '{"myTest":[{"name":"projectName1","param":1},{"name":"projectName2","param":2}]}'
    lines[1] == '{"myTest":[{"name":"moduleName1","param":1}]}'

  }
}