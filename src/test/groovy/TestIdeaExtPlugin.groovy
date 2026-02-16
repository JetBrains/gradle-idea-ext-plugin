import com.google.gson.Gson
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.jetbrains.gradle.ext.SerializationUtil
import spock.lang.Specification
import spock.lang.TempDir

import static org.assertj.core.api.Assertions.assertThat

class IdeaModelExtensionFunctionalTest extends Specification {
  @TempDir
  File testProjectDir
  File buildFile
  File settingsFile

  static List<String> gradleVersionList = Runtime.version().feature() > 16
    ? [ "7.6.4", "8.14", "9.0.0", "9.4.0-rc-1" ]
    : [ "5.0", "5.6.4", "6.0", "6.8.3", "7.0" ]

  def setup() {
      buildFile =  new File(testProjectDir, "build.gradle")
      settingsFile = new File(testProjectDir, "settings.gradle")
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
            generateImlFiles = true
            compiler.resourcePatterns '!*.java;!*.class'
            inspections {
                "some" { enabled = true }
            }
            runConfigurations {
                "Run my app"(Application) {
                    mainClass = 'foo.App'
                    workingDirectory = "\$projectDir" 
                    moduleRef project.getProject() 
                    includeProvidedDependencies = true 
                }
                defaults(Application) {
                    jvmArgs = '-DmyKey=myVal'
                }
            }
          }
        }
      }
      
      println(projectDir)
      println(project.idea.project.settings)
      task printSettings { }
    """
    when:
    def result = runPrintSettings(gradleVersion)
    then:

    def lines = result.output.readLines()
    def projectDir = lines[0]
    def prettyOutput = prettyPrintJSON(lines[1])
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
            "moduleName": "ProjectName",
            "workingDirectory": ${new Gson().toJson(projectDir)},
            "beforeRun": [],
            "mainClass": "foo.App",
            "includeProvidedDependencies": true
        },
        {
            "defaults": true,
            "type": "application",
            "name": "default_org.jetbrains.gradle.ext.Application",
            "beforeRun": [],
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
    prettyOutput.contains("""
    "generateImlFiles": true
"""
    )

    assertPrintSettingsSuccessOrUpToDate(result)
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

println(project.idea.project.settings)
task printSettings { }
"""

    when:
    def result = runPrintSettings(gradleVersion)
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

println(project.idea.project.settings)
task printSettings { }
"""

    when:
    def result = runPrintSettings(gradleVersion)
    then:

    def lines = result.output.readLines()
    prettyPrintJSON(lines[0]) == """{
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
            .withProjectDir(testProjectDir)
            .withArguments("buildIdeArtifact")
            .withPluginClasspath()
            .build()
    then:
    new File(testProjectDir, "build/idea-artifacts/root/dir1/build.gradle").exists()

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

println(project.idea.project.settings)
task printSettings { }
"""

    when:
    def result = runPrintSettings(gradleVersion)
    then:

    def lines = result.output.readLines()
    prettyPrintJSON(lines[0]) ==
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
     new File(testProjectDir,"p1").mkdirs()
     new File(testProjectDir,"p2").mkdirs()
     new File(testProjectDir,"p3").mkdirs()
    when:
    def result = GradleRunner.create()
            .withGradleVersion(gradleVersion)
            .withProjectDir(testProjectDir)
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
          
      println(projectDir)
      println(project.idea.module.settings)
      task printSettings { }
        """
        when:
        def result = runPrintSettings(gradleVersion)
        then:
        def lines = result.output.readLines()
        lines[1] == '{"moduleType":{"":"SOME_TYPE","main":"JAVA_MODULE","test":"PYTHON_MODULE"},' +
                '"facets":[{"type":"spring","contexts":' +
                '[{"file":"spring_parent.xml","name":"p1","parent":null},' +
                '{"file":"spring_new_child.xml","name":"p2","parent":"p1"}],"name":"spring"}]}'

        assertPrintSettingsSuccessOrUpToDate(result)

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
          
      println(projectDir)
      println(project.idea.module.settings)
      task printSettings { }
        """
    when:
    def result = runPrintSettings(gradleVersion)
    then:
    def lines = result.output.readLines()
    def moduleContentRoot = testProjectDir.canonicalPath.replace(File.separator, '/')
    lines[1] == '{"packagePrefix":{' +
            '"' + moduleContentRoot + '/main/groovy":"com.example.main.groovy",' +
            '"' + moduleContentRoot + '/test/java":"com.example.test.java"' +
            '}}'

    assertPrintSettingsSuccessOrUpToDate(result)

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
      
println(project.idea.module.settings)
task printSettings { }
    """
    when:
    def result = runPrintSettings(gradleVersion)
    then:
    def lines = result.output.readLines()
    def moduleContentRoot = testProjectDir.canonicalPath.replace(File.separator, '/')
    lines[0] == '{"packagePrefix":{' +
            '"' + moduleContentRoot + '/src":"com.example.java",' +
            '"' + moduleContentRoot + '/../subproject/src":"com.example.java.sub"' +
            '}}'

    assertPrintSettingsSuccessOrUpToDate(result)

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
      
      println(project.idea.module.settings)
      task printSettings { }
    """
    when:
    def result = runPrintSettings(gradleVersion)
    then:
    def lines = result.output.readLines()
    def moduleContentRoot = testProjectDir.canonicalPath.replace(File.separator, '/')
    lines[0] == '{"packagePrefix":{"' + moduleContentRoot + '/src":"com.example.java"}}'

    assertPrintSettingsSuccessOrUpToDate(result)

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
      
    println(project.idea.project.settings)
    task printSettings { }
    """
    when:
    def result = runPrintSettings(gradleVersion)
    then:
    def lines = result.output.readLines()
    def moduleContentRoot = testProjectDir.canonicalPath.replace(File.separator, '/')
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

    assertPrintSettingsSuccessOrUpToDate(result)

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
      
    println(project.idea.project.settings)
    task printSettings { }
    """
    when:
    def result = runPrintSettings(gradleVersion)
    then:
    def lines = result.output.readLines()
    lines[0] == '{"encodings":{' +
            '"encoding":"windows-1251",' +
            '"properties":{' +
            '"encoding":"windows-1251"' +
            '}' +
            '}' +
            '}'

    assertPrintSettingsSuccessOrUpToDate(result)

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

println(project.idea.project.settings)
println(project.idea.module.settings)
task printSettings { }
"""

    when:
    def result = runPrintSettings(gradleVersion)
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

println(project.idea.project.settings)
println(project.idea.module.settings)
task printSettings { }
"""

    when:
    def result = runPrintSettings(gradleVersion)
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

println(project.idea.project.settings)
println(project.idea.module.settings)
task printSettings { }
"""

    when:
    def result = runPrintSettings(gradleVersion)
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
      
      
      println("LazyFlag before=[\${project.lazyFlagForTaskTrigger}]")
      println(project.idea.project.settings)
      println("LazyFlag after=[\${project.lazyFlagForTaskTrigger}]")
      println(projectDir.absolutePath.replace('\\\\', '/'))
      task printSettings {}
    """
    when:
    def result = runPrintSettings(gradleVersion)
    then:

    def lines = result.output.readLines()
    def projectDir = lines[3]
    "LazyFlag before=[false]" == lines[0]
    "LazyFlag after=[true]"   == lines[2]
    def prettyOutput = prettyPrintJSON(lines[1])
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

    assertPrintSettingsSuccessOrUpToDate(result)

    where:
    gradleVersion << gradleVersionList.findAll { (GradleVersion.version(it) >= GradleVersion.version("5.0")) }
  }
    
    def "test process IDEA project files task"() {
        given:
        def layoutFile = new File(testProjectDir, "layout.json")
        def rootPath = testProjectDir.absolutePath.replace('\\', '/')
        layoutFile << """
{
  "ideaDirPath": "${rootPath}/.idea",
  "modulesMap": {
    "ProjectName": "${rootPath}/.idea/modules/ProjectName.iml",
    "ProjectName:test": "${rootPath}/.idea/modules/ProjectName.test.iml",
    "ProjectName:main": "${rootPath}/.idea/modules/ProjectName.main.iml"
  }
}
"""
        def modulesFolder = new File(testProjectDir, ".idea/modules")
        modulesFolder.mkdirs()
        def ideaDir = modulesFolder.parentFile
        def vcsFile = new File(ideaDir, "vcs.xml")
        // language=xml
        vcsFile << """<?xml version="1.0"?>
<project version="4">
  <component name="VcsDirectoryMappings">
    <mapping directory="" vcs="Git" />
  </component>
</project>"""

        def parentModuleFile = new File(modulesFolder, 'ProjectName.iml')
        // language=xml
        parentModuleFile << """<?xml version="1.0" encoding="UTF-8"?>
<module external.linked.project.id="ProjectName" external.linked.project.path="\$MODULE_DIR\$/../.." external.root.project.path="\$MODULE_DIR\$/../.." external.system.id="GRADLE" external.system.module.group="" external.system.module.version="1.0.2-SNAPSHOT" type="JAVA_MODULE" version="4">
  <component name="NewModuleRootManager" inherit-compiler-output="true">
    <exclude-output />
    <content url="file://\$MODULE_DIR\$/../..">
      <excludeFolder url="file://\$MODULE_DIR\$/../../.gradle" />
      <excludeFolder url="file://\$MODULE_DIR\$/../../build" />
    </content>
    <orderEntry type="inheritedJdk" />
    <orderEntry type="sourceFolder" forTests="false" />
  </component>
</module>
"""
        def mainModuleFile = new File(modulesFolder, 'ProjectName.main.iml')
        // language=xml
        mainModuleFile << """<?xml version="1.0" encoding="UTF-8"?>
<module external.linked.project.id="ProjectName:main" external.linked.project.path="\$MODULE_DIR\$/../.." external.root.project.path="\$MODULE_DIR\$/../.." external.system.id="GRADLE" external.system.module.group="" external.system.module.type="sourceSet" external.system.module.version="1.0.2-SNAPSHOT" type="JAVA_MODULE" version="4">
  <component name="NewModuleRootManager">
    <output url="file://\$MODULE_DIR\$/../../build/classes/java/main" />
    <exclude-output />
    <content url="file://\$MODULE_DIR\$/../../src/main">
      <sourceFolder url="file://\$MODULE_DIR\$/../../src/main/groovy" isTestSource="false" />
    </content>
    <orderEntry type="inheritedJdk" />
    <orderEntry type="sourceFolder" forTests="false" />
    <orderEntry type="library" name="Gradle: com.google.code.gson:gson:2.8.6" level="project" />
    <orderEntry type="library" name="Gradle: com.google.guava:guava:28.2-jre" level="project" />
  </component>
</module>
"""
        settingsFile << """
rootProject.name = "ProjectName"
"""
        // language=groovy
        buildFile << """
      
import org.gradle.api.DefaultTask
import org.gradle.api.XmlProvider
import org.jetbrains.gradle.ext.*
import org.w3c.dom.Element
import org.w3c.dom.Node

      plugins {
          id 'org.jetbrains.gradle.plugin.idea-ext'
          id 'java'
      }
      
      idea.project.settings {
          withIDEADir { File dir ->
            println("Callback 1 executed with: " + dir.absolutePath)         
          }

          withIDEADir { File dir ->
            println("Callback 2 executed with: " + dir.absolutePath)
          }
          
          withIDEAFileXml("vcs.xml") { XmlProvider p ->
            p.asNode().component
            .find { it.@name == 'VcsDirectoryMappings' }
            .mapping.@vcs = 'SVN'
          }
      }
      
      idea.module.settings {
        withModuleFile { File file ->
          println("Callback for parent module executed with " + file.absolutePath)
        }
        withModuleXml { XmlProvider p ->
          p.asNode().appendNode("test", ["key":"value"])
        }
        withModuleXml { XmlProvider p ->
          p.asNode().appendNode("test2", ["key":"value"])
        }
        withModuleFile(sourceSets.main) { File file ->
          println("Callback for main module executed with " + file.absolutePath)
        }
        withModuleXml(sourceSets.main) { XmlProvider p ->
          p.asNode().appendNode("testMain", ["key":"valueMain"])
        }
      }
    """
        when:
        def result = GradleRunner.create()
                .withGradleVersion(gradleVersion)
                .withProjectDir(testProjectDir)
                .withArguments("processIdeaSettings")
                .withPluginClasspath()
                .withDebug(true)
                .build()
        then:

        result.task(":processIdeaSettings").outcome == TaskOutcome.SUCCESS

        String ideaDirPath = ideaDir.absolutePath
        def lines = result.output.readLines()
        assertThat(lines).contains("Callback 1 executed with: " + ideaDirPath,
                "Callback 2 executed with: " + ideaDirPath,
                "Callback for parent module executed with " + parentModuleFile.absolutePath,
                "Callback for main module executed with " + mainModuleFile.absolutePath)

        assertThat(vcsFile).hasContent("""<?xml version="1.0"?>
<project version="4">
  <component name="VcsDirectoryMappings">
    <mapping directory="" vcs="SVN"/>
  </component>
</project>""")

        assertThat(parentModuleFile.text)
                .contains("""<test key="value"/>""")
                .contains("""<test2 key="value"/>""")
        assertThat(mainModuleFile.text).contains("""<testMain key="valueMain"/>""")

        assertThat(layoutFile).doesNotExist()

        where:
        gradleVersion << gradleVersionList
    }

    def "test process IDEA project files in multi-module project"() {
        given:
        File layoutFile = new File(testProjectDir, "layout.json")
        def rootPath = testProjectDir.absolutePath.replace('\\', '/')
        layoutFile << """
{
  "ideaDirPath": "${rootPath}/.idea",
  "modulesMap": {
    "ProjectName": "${rootPath}/.idea/modules/ProjectName.iml",
    "ProjectName:test": "${rootPath}/.idea/modules/ProjectName.test.iml",
    "ProjectName:main": "${rootPath}/.idea/modules/ProjectName.main.iml",
    ":Sub": "${rootPath}/.idea/modules/ProjectName.Sub.iml",
    ":ProjectName" : "${rootPath}/.idea/modules/ProjectName/ProjectName.ProjectName.iml",
    ":ProjectName:main" : "${rootPath}/.idea/modules/ProjectName/ProjectName.ProjectName.main.iml"
  }
}
"""
        def modulesFolder = new File(testProjectDir, ".idea/modules")
        modulesFolder.mkdirs()
        def ideaDir = modulesFolder.parentFile
        def vcsFile = new File(ideaDir, "vcs.xml")
        // language=xml
        vcsFile << """<?xml version="1.0"?>
<project version="4">
  <component name="VcsDirectoryMappings">
    <mapping directory="" vcs="Git" />
  </component>
</project>"""

        def parentModuleFile = new File(modulesFolder, 'ProjectName.iml')
        // language=xml
        parentModuleFile << """<?xml version="1.0" encoding="UTF-8"?>
<module external.linked.project.id="ProjectName" external.linked.project.path="\$MODULE_DIR\$/../.." external.root.project.path="\$MODULE_DIR\$/../.." external.system.id="GRADLE" external.system.module.group="" external.system.module.version="1.0.2-SNAPSHOT" type="JAVA_MODULE" version="4">
  <component name="NewModuleRootManager" inherit-compiler-output="true">
    <exclude-output />
    <content url="file://\$MODULE_DIR\$/../..">
      <excludeFolder url="file://\$MODULE_DIR\$/../../.gradle" />
      <excludeFolder url="file://\$MODULE_DIR\$/../../build" />
    </content>
    <orderEntry type="inheritedJdk" />
    <orderEntry type="sourceFolder" forTests="false" />
  </component>
</module>
"""
        def mainModuleFile = new File(modulesFolder, 'ProjectName.main.iml')
        // language=xml
        mainModuleFile << """<?xml version="1.0" encoding="UTF-8"?>
<module external.linked.project.id="ProjectName:main" external.linked.project.path="\$MODULE_DIR\$/../.." external.root.project.path="\$MODULE_DIR\$/../.." external.system.id="GRADLE" external.system.module.group="" external.system.module.type="sourceSet" external.system.module.version="1.0.2-SNAPSHOT" type="JAVA_MODULE" version="4">
  <component name="NewModuleRootManager">
    <output url="file://\$MODULE_DIR\$/../../build/classes/java/main" />
    <exclude-output />
    <content url="file://\$MODULE_DIR\$/../../src/main">
      <sourceFolder url="file://\$MODULE_DIR\$/../../src/main/groovy" isTestSource="false" />
    </content>
    <orderEntry type="inheritedJdk" />
    <orderEntry type="sourceFolder" forTests="false" />
    <orderEntry type="library" name="Gradle: com.google.code.gson:gson:2.8.6" level="project" />
    <orderEntry type="library" name="Gradle: com.google.guava:guava:28.2-jre" level="project" />
  </component>
</module>
"""
        def subModuleFile = new File(modulesFolder, 'ProjectName.Sub.iml')
        // language=xml
        subModuleFile << """<?xml version="1.0" encoding="UTF-8"?>
<module external.linked.project.id="ProjectName:Sub" external.linked.project.path="\$MODULE_DIR\$/../.." external.root.project.path="\$MODULE_DIR\$/../.." external.system.id="GRADLE" external.system.module.group="" external.system.module.type="sourceSet" external.system.module.version="1.0.2-SNAPSHOT" type="JAVA_MODULE" version="4">
  <component name="NewModuleRootManager">
    <output url="file://\$MODULE_DIR\$/../../build/classes/java/main" />
    <exclude-output />
    <content url="file://\$MODULE_DIR\$/../../src/main">
      <sourceFolder url="file://\$MODULE_DIR\$/../../src/main/groovy" isTestSource="false" />
    </content>
    <orderEntry type="inheritedJdk" />
    <orderEntry type="sourceFolder" forTests="false" />
    <orderEntry type="library" name="Gradle: com.google.code.gson:gson:2.8.6" level="project" />
  </component>
</module>
"""

        def ideaProjectNameDir = new File(testProjectDir, ".idea/modules/ProjectName")
        ideaProjectNameDir.mkdirs()
        def projectNameDuplicate = new File(ideaProjectNameDir, "ProjectName.ProjectName.iml")
        // language=xml
        projectNameDuplicate << """<?xml version="1.0" encoding="UTF-8"?>
<module/>
"""
        def projectNameDuplicateMain = new File(ideaProjectNameDir, "ProjectName.ProjectName.main.iml")
        // language=xml
        projectNameDuplicateMain << """<?xml version="1.0" encoding="UTF-8"?>
<module/>
"""

        settingsFile << """
rootProject.name = "ProjectName"
include 'Sub'
include 'ProjectName'
"""
        // language=groovy
        buildFile << """
      plugins {
          id 'org.jetbrains.gradle.plugin.idea-ext'
          id 'java'
      }
    """
        def subDir = new File(testProjectDir, "Sub")
        subDir.mkdirs()
        File subBuildFile = new File(subDir, "build.gradle")

        // language=groovy
        subBuildFile << """import org.gradle.api.DefaultTask
import org.gradle.api.XmlProvider
import org.jetbrains.gradle.ext.*
import org.w3c.dom.Element
import org.w3c.dom.Node

      plugins {
          id 'org.jetbrains.gradle.plugin.idea-ext'
          id 'java'
      }
      
      rootProject.idea.project.settings {
          withIDEAFileXml("vcs.xml") { XmlProvider p ->
             p.asNode().appendNode("test1", ["key1":"value1"])
          }
      }
      
      idea.module.settings {
        withModuleXml { XmlProvider p ->
          p.asNode().appendNode("test2", ["key2":"value2"])
        }
      }
"""

        File projectNameDir = new File(testProjectDir, "ProjectName")
        projectNameDir.mkdirs()
        File projectNameDuplicateBuild = new File(projectNameDir, "build.gradle")
        projectNameDuplicateBuild << """import org.gradle.api.DefaultTask
import org.gradle.api.XmlProvider
import org.jetbrains.gradle.ext.*
import org.w3c.dom.Element
import org.w3c.dom.Node

      plugins {
          id 'org.jetbrains.gradle.plugin.idea-ext'
          id 'java'
      }
          
      idea.module.settings {
        withModuleXml { XmlProvider p ->
          p.asNode().appendNode("test", ["k":"v"])
        }
        
        withModuleXml(sourceSets.main) { XmlProvider p ->
          p.asNode().appendNode("test.main", ["k":"v"])
        }
      }
"""

        when:
        def result = GradleRunner.create()
                .withGradleVersion(gradleVersion)
                .withProjectDir(testProjectDir)
                .withArguments("processIdeaSettings", "-s")
                .withPluginClasspath()
                .withDebug(true)
                .build()
        then:

        result.task(":processIdeaSettings").outcome == TaskOutcome.SUCCESS

        assertThat(vcsFile.text)
                .contains("""<test1 key1="value1"/>""")

        assertThat(subModuleFile.text)
                .contains("""<test2 key2="value2"/>""")

        assertThat(projectNameDuplicate.text)
                .contains("""<test k="v"/>""")

        assertThat(projectNameDuplicateMain.text)
                .contains("""<test.main k="v"/>""")

        assertThat(layoutFile).doesNotExist()

        where:
        gradleVersion << gradleVersionList
    }

    private static String prettyPrintJSON(String line) {
        return SerializationUtil.prettyPrintJsonStr(line)
    }

    private BuildResult runPrintSettings(String gradleVersion) {
        return GradleRunner.create()
                .withGradleVersion(gradleVersion)
                .withProjectDir(testProjectDir)
                .withArguments("printSettings", "-q", "-Dorg.gradle.unsafe.isolated-projects=true")
                .withPluginClasspath()
                .build()
    }

    private static boolean assertPrintSettingsSuccessOrUpToDate(BuildResult result) {
        def success = result.task(":printSettings").outcome == TaskOutcome.SUCCESS
        def upToDate = result.task(":printSettings").outcome == TaskOutcome.UP_TO_DATE
        return success || upToDate
    }
}