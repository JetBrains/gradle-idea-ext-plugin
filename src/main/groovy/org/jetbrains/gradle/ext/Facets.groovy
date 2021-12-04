package org.jetbrains.gradle.ext

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet

import javax.inject.Inject

@CompileStatic
interface Facet extends Named, MapConvertible {
    String getType()
}

class SpringFacet implements Facet {

    final String name
    final String type = "spring"
    final NamedDomainObjectContainer<SpringContext> contexts

    @Inject
    SpringFacet(String name, Project project) {
        this.name = name
        contexts = project.container(SpringContext)
    }

    def contexts(Action<NamedDomainObjectContainer<SpringContext>> action) {
        action.execute(contexts)
    }

    Map<String, ?> toMap() {
        return [
                "type"    : type,
                "contexts": contexts.asList().collect { it.toMap() },
                "name"    : name
        ]
    }
}

@CompileStatic
class SpringContext {

    final String name
    String file
    String parent

    SpringContext(String name) {
        this.name = name;
    }

    Map<String, ?> toMap() {
        return [
                "file"  : file,
                "name"  : name,
                "parent": parent
        ]
    }
}

class WebFacet implements Facet {

    final String name
    final String type = "web"

    SourceSet sourceSet
    Map<String, String> webRoots

    Project project

    @Inject
    WebFacet(String name, Project project) {
        this.project = project
        this.name = name
    }

    Map<String, ?> toMap() {
        return [
                "type"     : type,
                "sourceSet": sourceSet?.name ?: 'main',
                "webRoots" : webRoots.collectEntries { source, target -> [project.file(source).path, target]},
                "name"     : name
        ]
    }
}