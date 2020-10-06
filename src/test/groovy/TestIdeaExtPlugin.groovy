import groovy.json.JsonOutput
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class IdeaModelExtensionFunctionalTest extends Specification {
  @Rule
  TemporaryFolder testProjectDir = new TemporaryFolder()
  File buildFile
  File settingsFile

  static List<String> gradleVersionList = ["5.0", "5.6", "5.6.4", "6.1.1", "6.6"]

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
            copyright {
               profiles {
                 myProfile {
                   notice = "My private license text placeholder"
                 }
               }
               useDefault = "myProfile"
            }
            compiler.resourcePatterns '!*.java;!*.class'
            inspections {
                "some" { enabled = true }
            }
            runConfigurations {
                "Run my app"(Application) {
                    mainClass = 'foo.App'
                    workingDirectory = "\$projectDir" 
                    moduleName = getProject().idea.module.name
                    includeProvidedDependencies = true 
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
                             .withGradleVersion(gradleVersion)
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
            "defaults": false,
            "type": "application",
            "name": "Run my app",
            "workingDirectory": ${JsonOutput.toJson(projectDir)},
            "beforeRun": [
                
            ],
            "mainClass": "foo.App",
            "moduleName": "ProjectName",
            "includeProvidedDependencies": true
        },
        {
            "defaults": true,
            "type": "application",
            "name": "default_org.jetbrains.gradle.ext.Application",
            "beforeRun": [
                
            ],
            "jvmArgs": "-DmyKey=myVal",
            "includeProvidedDependencies": false
        }
    ]""")
    prettyOutput.contains(
""""copyright": {
        "useDefault": "myProfile",
        "profiles": {
            "myProfile": {
                "name": "myProfile",
                "notice": "My private license text placeholder"
            }
        }
    }"""
    )

    result.task(":printSettings").outcome == TaskOutcome.SUCCESS
    where:
    gradleVersion << gradleVersionList
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
            .withGradleVersion(gradleVersion)
            .withProjectDir(testProjectDir.root)
            .withArguments("printSettings", "-q")
            .withPluginClasspath()
            .build()
    then:

    def lines = result.output.readLines()
    lines[0] == '{"groovyCompiler":{"excludes":[{"url":"/some/myFile","includeSubdirectories":false,"isFile":true},' +
            '{"url":"/a/dir","includeSubdirectories":true,"isFile":false}]}}'
    where:
    gradleVersion << gradleVersionList
  }

  def "test artifacts settings"() {
    given:
    buildFile << """
plugins {
  id 'org.jetbrains.gradle.plugin.idea-ext'
}

idea {
  project {
    settings {
      ideArtifacts {
        myArt {
          directory("dir1") {
            archive("my.zip") {
              file("build.gradle")
            }
          }
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
            .withGradleVersion(gradleVersion)
            .withProjectDir(testProjectDir.root)
            .withArguments("printSettings", "-q")
            .withPluginClasspath()
            .build()
    then:

    def lines = result.output.readLines()
    JsonOutput.prettyPrint(lines[0]) == """{
    "ideArtifacts": [
        {
            "type": "ARTIFACT",
            "name": "myArt",
            "children": [
                {
                    "type": "DIR",
                    "name": "dir1",
                    "children": [
                        {
                            "type": "ARCHIVE",
                            "name": "my.zip",
                            "children": [
                                {
                                    "type": "FILE",
                                    "sourceFiles": [
                                        "${buildFile.canonicalPath.replace('\\\\' as char, '/' as char)}"
                                    ]
                                }
                            ]
                        }
                    ]
                }
            ]
        }
    ]
}"""

    where:
    gradleVersion << gradleVersionList
  }

  def "test build ide artifact reference"() {
    given:
    buildFile << """
plugins {
  id 'org.jetbrains.gradle.plugin.idea-ext'
}

idea {
  project {
    settings {
      ideArtifacts {
        ref {
          directory("dir1") {
            file("build.gradle")
          }
        }
        root {
          artifact("ref")
        }
      }
    }
  }
}

task buildIdeArtifact(type: org.jetbrains.gradle.ext.BuildIdeArtifact) {
  artifact = idea.project.settings.ideArtifacts["root"]
}
"""

    when:
    GradleRunner.create()
            .withGradleVersion(gradleVersion)
            .withProjectDir(testProjectDir.root)
            .withArguments("buildIdeArtifact")
            .withPluginClasspath()
            .build()
    then:
    new File(testProjectDir.root, "build/idea-artifacts/root/dir1/build.gradle").exists()

    where:
    gradleVersion << gradleVersionList
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
        hardWrapAt = 200
        java {
          alignParameterDescriptions = false
        }
        groovy {
          alignMultilineNamedArguments = false
          classCountToUseImportOnDemand = 999
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
            .withGradleVersion(gradleVersion)
            .withProjectDir(testProjectDir.root)
            .withArguments("printSettings", "-q")
            .withPluginClasspath()
            .build()
    then:

    def lines = result.output.readLines()
    JsonOutput.prettyPrint(lines[0]) ==
"""{
    "codeStyle": {
        "RIGHT_MARGIN": 200,
        "languages": {
            "java": {
                "JD_ALIGN_PARAM_COMMENTS": false
            },
            "groovy": {
                "CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND": 999,
                "ALIGN_NAMED_ARGS_IN_MAP": false
            }
        }
    }
}"""

    where:
    gradleVersion << gradleVersionList
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
            .withGradleVersion(gradleVersion)
            .withProjectDir(testProjectDir.root)
            .withArguments("projects", "--stacktrace")
            .withPluginClasspath()
            .build()
    then:
    // result.output.contains("p1")
    result.task(":projects").outcome == TaskOutcome.SUCCESS

    where:
    gradleVersion << gradleVersionList
  }

    def "test module settings"() {
        given:
        // language=groovy
        buildFile << """
          import org.jetbrains.gradle.ext.*
          
          plugins {
              id 'org.jetbrains.gradle.plugin.idea-ext'
              id 'java'
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
                
                rootModuleType = "SOME_TYPE"
                moduleType[sourceSets.main] = "JAVA_MODULE"
                moduleType[sourceSets.test] = "PYTHON_MODULE"
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
                .withGradleVersion(gradleVersion)
                .withProjectDir(testProjectDir.root)
                .withArguments("printSettings", "-q")
                .withPluginClasspath()
                .build()
        then:
        def lines = result.output.readLines()
        lines[1] == '{"moduleType":{"":"SOME_TYPE","main":"JAVA_MODULE","test":"PYTHON_MODULE"},' +
                '"facets":[{"type":"spring","contexts":' +
                '[{"file":"spring_parent.xml","name":"p1","parent":null},' +
                '{"file":"spring_new_child.xml","name":"p2","parent":"p1"}],"name":"spring"}]}'

        result.task(":printSettings").outcome == TaskOutcome.SUCCESS

      where:
      gradleVersion << gradleVersionList
    }

  def "test module package prefix settings"() {
    given:
    buildFile << """
          import org.jetbrains.gradle.ext.*
          
          plugins {
              id 'org.jetbrains.gradle.plugin.idea-ext'
          }
          
          idea {
            module {
              settings {
                  packagePrefix["main/groovy"] = "com.example.main.groovy"
                  packagePrefix["test/java"] = "com.example.test.java"
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
            .withGradleVersion(gradleVersion)
            .withProjectDir(testProjectDir.root)
            .withArguments("printSettings", "-q")
            .withPluginClasspath()
            .build()
    then:
    def lines = result.output.readLines()
    def moduleContentRoot = testProjectDir.root.canonicalPath.replace(File.separator, '/')
    lines[1] == '{"packagePrefix":{' +
            '"' + moduleContentRoot + '/main/groovy":"com.example.main.groovy",' +
            '"' + moduleContentRoot + '/test/java":"com.example.test.java"' +
            '}}'

    result.task(":printSettings").outcome == TaskOutcome.SUCCESS

    where:
    gradleVersion << gradleVersionList
  }

  def "test module settings with remote source root"() {
    given:
    buildFile << """
      import org.jetbrains.gradle.ext.*
      
      plugins {
          id 'org.jetbrains.gradle.plugin.idea-ext'
      }
      
      apply plugin: "java"
      
      sourceSets {
        main.java.srcDirs += "src"
        main.java.srcDirs += "../subproject/src"
      }
      
      idea {
        module {
          settings {
            packagePrefix["src"] = "com.example.java"
            packagePrefix["../subproject/src"] = "com.example.java.sub"
          }
        }
      }
      
      task printSettings {
        doLast {
          println project.idea.module.settings
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
    def moduleContentRoot = testProjectDir.root.canonicalPath.replace(File.separator, '/')
    lines[0] == '{"packagePrefix":{' +
            '"' + moduleContentRoot + '/src":"com.example.java",' +
            '"' + moduleContentRoot + '/../subproject/src":"com.example.java.sub"' +
            '}}'

    result.task(":printSettings").outcome == TaskOutcome.SUCCESS

    where:
    gradleVersion << gradleVersionList
  }

  def "test module settings with custom source root"() {
    given:
    buildFile << """
      import org.jetbrains.gradle.ext.*
      
      plugins {
          id 'org.jetbrains.gradle.plugin.idea-ext'
      }
      
      apply plugin: "java"
      
      sourceSets {
        main.java.srcDirs += "src"
      }
      
      idea {
        module {
          settings {
            packagePrefix["src"] = "com.example.java"
          }
        }
      }
      
      task printSettings {
        doLast {
          println project.idea.module.settings
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
    def moduleContentRoot = testProjectDir.root.canonicalPath.replace(File.separator, '/')
    lines[0] == '{"packagePrefix":{"' + moduleContentRoot + '/src":"com.example.java"}}'

    result.task(":printSettings").outcome == TaskOutcome.SUCCESS

    where:
    gradleVersion << gradleVersionList
  }

  def "test complex project encoding settings"() {
    given:
    buildFile << """
      import org.jetbrains.gradle.ext.*
      import org.jetbrains.gradle.ext.EncodingConfiguration.BomPolicy
      
      plugins {
          id 'org.jetbrains.gradle.plugin.idea-ext'
      }
      
      idea {
        project {
          settings {
            encodings {
              encoding = 'windows-1251'
              bomPolicy = BomPolicy.WITH_NO_BOM
              properties {
                encoding = 'windows-1251'
                transparentNativeToAsciiConversion = false
              }
              mapping['path/to/dir1'] = 'UTF-8'
              mapping['path/to/dir2'] = 'windows-1251'
              mapping['path/to/dir3'] = 'ISO-8859-1'
              mapping['../path/to/dir4'] = 'US-ASCII'
            }
          }
        }
      }
      
      task printSettings {
        doLast {
          println project.idea.project.settings
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
    def moduleContentRoot = testProjectDir.root.canonicalPath.replace(File.separator, '/')
    lines[0] == '{"encodings":{' +
            '"encoding":"windows-1251",' +
            '"bomPolicy":"WITH_NO_BOM",' +
            '"properties":{' +
            '"encoding":"windows-1251",' +
            '"transparentNativeToAsciiConversion":false' +
            '},' +
            '"mapping":{' +
            '"' + moduleContentRoot + '/path/to/dir1":"UTF-8",' +
            '"' + moduleContentRoot + '/path/to/dir2":"windows-1251",' +
            '"' + moduleContentRoot + '/path/to/dir3":"ISO-8859-1",' +
            '"' + moduleContentRoot + '/../path/to/dir4":"US-ASCII"' +
            '}' +
            '}' +
            '}'

    result.task(":printSettings").outcome == TaskOutcome.SUCCESS

    where:
    gradleVersion << gradleVersionList
  }

  def "test partial project encoding settings"() {
    given:
    buildFile << """
      import org.jetbrains.gradle.ext.*
      
      plugins {
          id 'org.jetbrains.gradle.plugin.idea-ext'
      }
      
      idea {
        project {
          settings {
            encodings {
              encoding = 'windows-1251'
              properties.encoding = 'windows-1251'
            }
          }
        }
      }
      
      task printSettings {
        doLast {
          println project.idea.project.settings
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
    lines[0] == '{"encodings":{' +
            '"encoding":"windows-1251",' +
            '"properties":{' +
            '"encoding":"windows-1251"' +
            '}' +
            '}' +
            '}'

    result.task(":printSettings").outcome == TaskOutcome.SUCCESS

    where:
    gradleVersion << gradleVersionList
  }

  def "test extending the DSL with custom run configurations and facets"() {
    given:
    buildFile << """
import org.jetbrains.gradle.ext.*
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.*
import javax.inject.Inject

class MyRC extends BaseRunConfiguration {
  def name = "value"
  def aKey = "a value"
  def type = "myRunConfiguration"
  @Inject
  MyRC(String param) { name = param }
  String getName() { return name }
  String getType() { return type }
  Map<String, Object> toMap() { return [ "name": name, "type":type, "aKey": aKey ] } 
}

class MyFacet implements Facet {
  def type = "MyFacetType"
  def name = "facet_name"
  def facetKey = "facet_value"
  @Inject
  MyFacet(param) { name = param }
  String getName() { return name }
  String getType() { return type }
  Map<String, Object> toMap() { return [ "type": type, "facetKey": facetKey ] }
}

class TestPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.apply plugin: 'org.jetbrains.gradle.plugin.idea-ext'
        def ideaModel = project.extensions.findByName('idea') as IdeaModel
    
        def projectSettings = (ideaModel.project as ExtensionAware).extensions.findByName("settings") as ProjectSettings
        projectSettings.runConfigurations.registerFactory(MyRC) { String name -> project.objects.newInstance(MyRC, name) }
        
        def moduleSettings = (ideaModel.module as ExtensionAware).extensions.findByName("settings") as ModuleSettings
        moduleSettings.facets.registerFactory(MyFacet) { String name -> project.objects.newInstance(MyFacet, name) }
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
      runConfigurations {
        "testRunConfig"(MyRC) {
          aKey = "project_test_value"
        }
      }
    }
  }
  module.settings.facets {
    testFacet(MyFacet) {
      facetKey = "module_facet_value"
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
            .withGradleVersion(gradleVersion)
            .withProjectDir(testProjectDir.root)
            .withArguments("printSettings", "-q")
            .withPluginClasspath()
            .build()
    then:

    def lines = result.output.readLines()
    lines[0] == '{"runConfigurations":[{"name":"testRunConfig","type":"myRunConfiguration","aKey":"project_test_value"}]}'
    lines[1] == '{"facets":[{"type":"MyFacetType","facetKey":"module_facet_value"}]}'

    where:
    gradleVersion << gradleVersionList

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
            .withGradleVersion(gradleVersion)
            .withProjectDir(testProjectDir.root)
            .withArguments("printSettings", "-q")
            .withPluginClasspath()
            .build()
    then:

    def lines = result.output.readLines()
    lines[0] == '{"myTest":{"name":"test_project_value"}}'
    lines[1] == '{"myTest":{"name":"test_module_value"}}'

    where:
    gradleVersion << gradleVersionList

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
            .withGradleVersion(gradleVersion)
            .withProjectDir(testProjectDir.root)
            .withArguments("printSettings", "-q")
            .withPluginClasspath()
            .build()
    then:

    def lines = result.output.readLines()
    lines[0] == '{"myTest":[{"name":"projectName1","param":1},{"name":"projectName2","param":2}]}'
    lines[1] == '{"myTest":[{"name":"moduleName1","param":1}]}'

    where:
    gradleVersion << gradleVersionList
  }


  def "test task triggers settings"() {
    given:
    settingsFile << """
rootProject.name = "ProjectName"
"""
    // language=groovy
    buildFile << """
      
import org.gradle.api.DefaultTask
import org.jetbrains.gradle.ext.*

      plugins {
          id 'org.jetbrains.gradle.plugin.idea-ext'
      }
      
    
      ext.lazyFlagForTaskTrigger = false  
      def provider = tasks.register("LazyTask", DefaultTask) {
          lazyFlagForTaskTrigger = true
      }
      
      idea.project.settings {
          taskTriggers {
              beforeSync(":help", provider)
          }
      }
      
      
      task printSettings {
        doLast {
          println("LazyFlag before=[\${project.lazyFlagForTaskTrigger}]")
          println(project.idea.project.settings)
          println("LazyFlag after=[\${project.lazyFlagForTaskTrigger}]")
          println(projectDir.absolutePath.replace('\\\\', '/'))
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
    def projectDir = lines[3]
    "LazyFlag before=[false]" == lines[0]
    "LazyFlag after=[true]"   == lines[2]
    def prettyOutput = JsonOutput.prettyPrint(lines[1])
    prettyOutput ==
"""{
    "taskTriggers": {
        "beforeSync": [
            {
                "taskPath": "help",
                "projectPath": "$projectDir"
            },
            {
                "taskPath": "LazyTask",
                "projectPath": "$projectDir"
            }
        ]
    }
}"""

    result.task(":printSettings").outcome == TaskOutcome.SUCCESS

    where:
    gradleVersion << gradleVersionList.findAll { (GradleVersion.version(it) >= GradleVersion.version("5.0")) }
  }
}