package org.jetbrains.gradle.ext

import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.internal.extensibility.DefaultConvention
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class IdeaFilesProcessorTest {
    lateinit var myProject: Project

    @BeforeEach
    fun setUp() {
        myProject = ProjectBuilder.builder().build()
    }

    @Test
    fun `test callback presence is detected`() {
        myProject.apply(mapOf("plugin" to "java"))
        val sourceSet = myProject.sourceSets.findByName("main")

        var processor = IdeaFilesProcessor(myProject)
        processor.withIDEAFileXml("file.xml") { println(it) }
        assertThat(processor.hasPostprocessors()).isTrue()

        processor = IdeaFilesProcessor(myProject)
        processor.withIDEAdir({ println(it)} )
        assertThat(processor.hasPostprocessors()).isTrue()

        processor = IdeaFilesProcessor(myProject)
        processor.withModuleFile(sourceSet) { println(it) }
        assertThat(processor.hasPostprocessors()).isTrue()

        processor = IdeaFilesProcessor(myProject)
        processor.withModuleXml(sourceSet) { println(it) }
        assertThat(processor.hasPostprocessors()).isTrue()
    }
}

private val Project.sourceSets: NamedDomainObjectCollection<SourceSet>
    get() {
        return (this.extensions as DefaultConvention)
                .extensionsAsDynamicObject.getProperty("sourceSets") as? NamedDomainObjectCollection<SourceSet>
                ?: throw IllegalStateException("Source sets are not available for this project. Check that 'java' plugin is applied")
    }