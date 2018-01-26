package org.jetbrains.gradle.ext

import groovy.transform.PackageScope
import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.internal.DefaultPolymorphicDomainObjectContainer
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.reflect.Instantiator

/**
 * Helpers for Gradle internal APIs.
 */
@PackageScope
class GradleUtils {

    static <T extends Named> ExtensiblePolymorphicDomainObjectContainer<T> polymorphicContainer(Project project, Class<T> type) {
        def instantiator = (project as ProjectInternal).services.get(Instantiator.class)
        return instantiator.newInstance(DefaultPolymorphicDomainObjectContainer, type, instantiator)
    }

    static <T extends Named, C extends ExtensiblePolymorphicDomainObjectContainer<T>> C customPolymorphicContainer(Project project, Class<C> containerType) {
        def instantiator = (project as ProjectInternal).services.get(Instantiator.class)
        return instantiator.newInstance(containerType, instantiator)
    }
}
