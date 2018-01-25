package org.jetbrains.gradle.ext

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

import javax.inject.Inject

interface Facet {

    String getName()

    String getType()

    Map<String, ?> toMap()
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
