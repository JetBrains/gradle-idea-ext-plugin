# gradle-idea-ext-plugin

[![JetBrains team project](http://jb.gg/badges/team.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

Proof-of-concept plugin to store arbitrary IJ settings in gradle script

### How to build

as simple as

    ./gradlew build

### How to apply

Drop resulting jar into root directory of a project and add following snippet to `build.grade`

    buildscript {
      dependencies {
        classpath files('gradle-idea-ext.jar')
      }
    }
    apply plugin: 'org.jetbrains.gradle.plugin.idea-ext'

### DSL Extensions
#### supported by IJ 2017.3 EAP 173.2290.1

    idea {
      module {
        settings {
          runConfigurations {
            ConfigurationName {
              type 'app'
              mainClass 'my.package.MainClass'
              jvmArgs  '-Xmx1g'
            }
            
            TestConfigName {
              type 'junit'
              class  'my.package.MyJUnitTestClass'
              jvmArgs '-Xmx500m'
            }
          }
        }
      }
      project {
        settings {
          compiler {
            resourcePatterns '!*.java;!*.class'
            clearOutputDirectory false
            addNotNullAssertions false
            autoShowFirstErrorInEditor false
            displayNotificationPopup false
            enableAutomake false
            parallelCompilation true
            rebuildModuleOnDependencyChange false
          }
        }
      }
    }
      
      
