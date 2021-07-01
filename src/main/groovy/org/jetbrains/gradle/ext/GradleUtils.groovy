package org.jetbrains.gradle.ext

import groovy.transform.PackageScope
import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.internal.CollectionCallbackActionDecorator
import org.gradle.api.internal.DefaultPolymorphicDomainObjectContainer
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.reflect.Instantiator
import org.gradle.util.GradleVersion
import org.jetbrains.gradle.ext.internal.DefaultRunConfigurationContainer
import org.jetbrains.gradle.ext.internal.DefaultRunConfigurationContainer51

/**
 * Helpers for Gradle internal APIs.
 */
@PackageScope
class GradleUtils {

    static boolean is_Gradle_5_1_or_newer = GradleVersion.current().compareTo(GradleVersion.version("5.1")) >= 0

    static <T extends Named> ExtensiblePolymorphicDomainObjectContainer<T> polymorphicContainer(Project project, Class<T> type) {
        def instantiator = (project as ProjectInternal).services.get(Instantiator.class)
        if (is_Gradle_5_1_or_newer) {
            return instantiator.newInstance(DefaultPolymorphicDomainObjectContainer, type, instantiator, CollectionCallbackActionDecorator.NOOP)
        } else {
            return instantiator.newInstance(DefaultPolymorphicDomainObjectContainer, type, instantiator)
        }
    }

    static RunConfigurationContainer runConfigurationsContainer(Project project) {
        if(is_Gradle_5_1_or_newer) {
            def instantiator = (project as ProjectInternal).services.get(Instantiator.class)
            return instantiator.newInstance(DefaultRunConfigurationContainer51, instantiator)
        } else {
            def instantiator = (project as ProjectInternal).services.get(Instantiator.class)
            return instantiator.newInstance(DefaultRunConfigurationContainer, instantiator)
        }
    }
}
