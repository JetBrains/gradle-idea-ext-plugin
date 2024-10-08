import com.google.gson.Gson
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.jetbrains.gradle.ext.SerializationUtil
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Ignore
import spock.lang.Specification

import static org.assertj.core.api.Assertions.assertThat

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
      idea.project {
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
          println(idea.project.settings.toString())
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

    def "test module package prefix settings"() {
        given:
        // language=kotlin
        buildFile << """
      import org.jetbrains.gradle.ext.*

      plugins {
        id ("org.jetbrains.gradle.plugin.idea-ext")
      }
          
          idea {
            module {
              settings {
                  packagePrefix["main/groovy"] = "com.example.main.groovy"
                  packagePrefix["test/java"] = "com.example.test.java"
              }
            }
          }
      tasks.register("printSettings") {
        doLast {
          println(project.projectDir)
          println(project.idea.module.settings.toString())
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


    def "test project settings"() {
        given:
        settingsFile << """
rootProject.name = "ProjectName"
"""
        // language=kotlin
        buildFile << """
      import org.gradle.plugins.ide.idea.model.IdeaModel
      import org.jetbrains.gradle.ext.*
      import org.jetbrains.gradle.ext.Application

      plugins {
          id("org.jetbrains.gradle.plugin.idea-ext")
      }
      
      idea {
        project {
          settings {
            copyright {
               profiles {
                 create("myProfile") {
                   notice = "My private license text placeholder"
                 }
               }
               useDefault = "myProfile"
            }
            compiler.resourcePatterns = "!*.java;!*.class"
            inspections {
                register("some") { enabled = true }
            }
            runConfigurations {
                create("Run my app", Application::class.java) {
                    mainClass = "foo.App"
                    workingDirectory = "\$projectDir" 
                    moduleRef(project.getProject()) 
                    includeProvidedDependencies = true 
                }
                defaults(Application::class.java) {
                    jvmArgs = "-DmyKey=myVal"
                }
            }
          }
        }
      }
      
      
      tasks.register("printSettings") {
        doLast {
          println(projectDir)
          println(idea.project.settings.toString())
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
            "beforeRun": [],
            "moduleName": "ProjectName",
            "workingDirectory": ${new Gson().toJson(projectDir)},
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

        result.task(":printSettings").outcome == TaskOutcome.SUCCESS
        where:
        gradleVersion << gradleVersionList
    }

    def "test groovy compiler settings"() {
        given:
        // language=kotlin
        buildFile << """
import org.jetbrains.gradle.ext.*
plugins {
  id("org.jetbrains.gradle.plugin.idea-ext")
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

tasks.register("printSettings") {
  doLast {
    println(idea.project.settings)
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

    @Ignore(value = "ide artifacts DSL in kotlin is not yet supported")
    def "test artifacts settings"() {
        given:
        // language=kotlin
        buildFile << """
import org.jetbrains.gradle.ext.*
plugins {
  id("org.jetbrains.gradle.plugin.idea-ext")
}

idea {
  project {
    settings {
      ideArtifacts {
        create("myArt") {
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

tasks.register("printSettings") {
  doLast {
    println(idea.project.settings)
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

    @Ignore(value = "ide artifacts DSL in kotlin is not yet supported")
    def "test build ide artifact reference"() {
        given:
        // language=kotlin
        buildFile << """
import org.jetbrains.gradle.ext.*
plugins {
  id("org.jetbrains.gradle.plugin.idea-ext")
}

idea {
  project {
    settings {
      ideArtifacts {
        create("ref"){
          directory("dir1") {
            file("build.gradle")
          }
        }
        create("root"){
          artifact("ref")
        }
      }
    }
  }
}

tasks.register<org.jetbrains.gradle.ext.BuildIdeArtifact>("buildIdeArtifact") {
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


    def "test module settings"() {
        given:
        // language=kotlin
        buildFile << """
          import org.jetbrains.gradle.ext.*
          
          plugins {
              id("org.jetbrains.gradle.plugin.idea-ext")
              id("java")
          }
          
          idea {
            module {
              settings {
                facets {
                  create("spring", SpringFacet::class.java) {
                    contexts {
                      create("p1") {
                        file = "spring_parent.xml"
                      }
                      
                      create("p2") {
                        file = "spring_child.xml"
                        parent = "p1"
                      }
                    }
                  }
                }
                 
                // TODO: support rootModuleType in KTS 
                // ext.set("rootModuleType", "SOME_TYPE")
                moduleType[sourceSets.getByName("main")] = "JAVA_MODULE"
                moduleType[sourceSets.getByName("test")] = "PYTHON_MODULE"
              }
            }
          }

          val springFacet: SpringFacet = idea.module.settings.facets.getByName("spring") as SpringFacet
          val p2 = springFacet.contexts.getByName("p2") 
          p2.file = "spring_new_child.xml"
          
          tasks.register("printSettings") {
            doLast {
              println(project.projectDir)
              println(idea.module.settings)
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
        lines[1] == '{"moduleType":{"main":"JAVA_MODULE","test":"PYTHON_MODULE"},' +
                '"facets":[{"type":"spring","contexts":' +
                '[{"file":"spring_parent.xml","name":"p1","parent":null},' +
                '{"file":"spring_new_child.xml","name":"p2","parent":"p1"}],"name":"spring"}]}'

        result.task(":printSettings").outcome == TaskOutcome.SUCCESS

        where:
        gradleVersion << gradleVersionList
    }

    def "test module settings with remote source root"() {
        given:
        // language=kotlin
        buildFile << """
      import org.jetbrains.gradle.ext.*
      
      plugins {
          id("org.jetbrains.gradle.plugin.idea-ext")
          id("java")
      }
      
      sourceSets {
        main {
            java {
                srcDir("src")
                srcDir("../subproject/src")
            }
        }
      }
      
      idea {
        module {
          settings {
            packagePrefix["src"] = "com.example.java"
            packagePrefix["../subproject/src"] = "com.example.java.sub"
          }
        }
      }
      
      tasks.register("printSettings") {
        doLast {
          println(idea.module.settings)
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
        // language=kotlin
        buildFile << """
      import org.jetbrains.gradle.ext.*
      
      plugins {
          id("org.jetbrains.gradle.plugin.idea-ext")
          id("java")
      }
      
      sourceSets {
        main {
          java.srcDir("src")
        }
      }
      
      idea {
        module {
          settings {
            packagePrefix["src"] = "com.example.java"
          }
        }
      }
      
      tasks.register("printSettings") {
        doLast {
          println(idea.module.settings)
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
        // language=kotlin
        buildFile << """
      import org.jetbrains.gradle.ext.*
      import org.jetbrains.gradle.ext.EncodingConfiguration.BomPolicy
      
      plugins {
          id("org.jetbrains.gradle.plugin.idea-ext")
      }
      
      idea {
        project {
          settings {
            encodings {
              encoding = "windows-1251"
              bomPolicy = BomPolicy.WITH_NO_BOM
              properties {
                encoding = "windows-1251"
                transparentNativeToAsciiConversion = false
              }
              mapping["path/to/dir1"] = "UTF-8"
              mapping["path/to/dir2"] = "windows-1251"
              mapping["path/to/dir3"] = "ISO-8859-1"
              mapping["../path/to/dir4"] = "US-ASCII"
            }
          }
        }
      }
      
      tasks.register("printSettings") {
        doLast {
          println(idea.project.settings)
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
        // language=kotlin
        buildFile << """
      import org.jetbrains.gradle.ext.*
      
      plugins {
          id("org.jetbrains.gradle.plugin.idea-ext")
      }
      
      idea {
        project {
          settings {
            encodings {
              encoding = "windows-1251"
              properties.encoding = "windows-1251"
            }
          }
        }
      }
      
      tasks.register("printSettings") {
        doLast {
          println(idea.project.settings)
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

    def "test task triggers settings"() {
        given:
        // language=kotlin
        settingsFile << """
rootProject.name = "ProjectName"
"""
        // language=kotlin
        buildFile << """
      
import org.gradle.api.DefaultTask
import org.jetbrains.gradle.ext.*

      plugins {
          id("org.jetbrains.gradle.plugin.idea-ext")
      }
      
    
      var lazyFlagForTaskTrigger: Boolean by extra
      lazyFlagForTaskTrigger = false  
      val provider = tasks.register<DefaultTask>("LazyTask") {
          lazyFlagForTaskTrigger = true
      }
      
      idea.project.settings {
          taskTriggers {
              beforeSync(":help", provider)
          }
      }
      
      
      tasks.register("printSettings") {
        doLast {
          println("LazyFlag before=[\${lazyFlagForTaskTrigger}]")
          println(idea.project.settings)
          println("LazyFlag after=[\${lazyFlagForTaskTrigger}]")
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

        result.task(":printSettings").outcome == TaskOutcome.SUCCESS

        where:
        gradleVersion << gradleVersionList.findAll { (GradleVersion.version(it) >= GradleVersion.version("5.0")) }
    }

    def "test process IDEA project files task"() {
        given:
        def layoutFile = testProjectDir.newFile('layout.json')
        def rootPath = testProjectDir.root.absolutePath.replace('\\', '/')

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
        def modulesFolder = testProjectDir.newFolder(".idea", "modules")
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
        // language=kotlin
        buildFile << """
      
import org.gradle.api.DefaultTask
import org.gradle.api.XmlProvider
import org.jetbrains.gradle.ext.*
import groovy.util.Node

      plugins {
          id("org.jetbrains.gradle.plugin.idea-ext")
          id("java")
      }
      
      idea.project.settings {
          withIDEADir(object: Action<File> { 
              override fun execute(dir: File) { println("Callback 1 executed with: " + dir.absolutePath) }
          })

          withIDEADir(object: Action<File> { 
              override fun execute(dir: File) { println("Callback 2 executed with: " + dir.absolutePath) }
          })
          
          withIDEAFileXml("vcs.xml", object: Action<XmlProvider> {
          // XmlProvider is very Groovy-centric and difficult to use from Kotlin
          override fun execute(p: XmlProvider) {
                val allNodes = p.asNode().depthFirst() as List<Node>
                allNodes
                .find { it.name() == "mapping" }
                ?.attributes()?.put("vcs", "SVN")
            }
          })
      }
      
      idea.module.settings {
        withModuleFile(object: Action<File> {  
           override fun execute(file: File) { println("Callback for parent module executed with " + file.absolutePath) }
        })
        withModuleXml(object: Action<XmlProvider> {
          override fun execute(p: XmlProvider) { p.asNode().appendNode("test", mapOf("key" to "value")) }
        })
        withModuleXml(object: Action<XmlProvider> {
          override fun execute(p: XmlProvider) { p.asNode().appendNode("test2", mapOf("key" to "value")) }
        })
        withModuleFile(sourceSets.getByName("main"), object: Action<File> { 
          override fun execute(file: File) { println("Callback for main module executed with " + file.absolutePath) }
        })
        withModuleXml(sourceSets.getByName("main"), object: Action<XmlProvider> {
          override fun execute(p: XmlProvider) { p.asNode().appendNode("testMain", mapOf("key" to "valueMain")) }
        })
      }
    """
        when:
        def result = GradleRunner.create()
                .withGradleVersion(gradleVersion)
                .withProjectDir(testProjectDir.root)
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

        where:
        gradleVersion << gradleVersionList.findAll { (GradleVersion.version(it) >= GradleVersion.version("7.0")) }
    }

    private static String prettyPrintJSON(String line) {
        return SerializationUtil.prettyPrintJsonStr(line)
    }
}
