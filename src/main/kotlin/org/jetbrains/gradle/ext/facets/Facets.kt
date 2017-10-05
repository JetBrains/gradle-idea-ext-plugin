package org.jetbrains.gradle.ext.facets

import groovy.lang.Closure
import org.gradle.api.Project

open class Facet(val name: String, val type: String)

class SpringFacet(name: String, project: Project) : Facet(name, type = "spring") {
    val contexts = project.container(SpringContext::class.java)

    fun contexts(action: Closure<*>) {
        contexts.configure(action)
    }

    /* // this implementation causes test failures (wrong delegation when configuring instances inside container)
        fun contexts(action: Action<NamedDomainObjectContainer<SpringContext>>) {
        action.execute(contexts)
    }
     */
}

class SpringContext(val name: String) {
    var file: String? = null
    var parent: String? = null
}