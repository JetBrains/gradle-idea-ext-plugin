package org.jetbrains.gradle.ext.internal

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.internal.DefaultPolymorphicDomainObjectContainer
import org.gradle.internal.reflect.Instantiator
import org.jetbrains.gradle.ext.RunConfiguration
import org.jetbrains.gradle.ext.RunConfigurationContainer

import javax.inject.Inject

/**
 * This class is only used with Gradle <5.1.
 * With 5.1+ the {@link DefaultRunConfigurationContainer51} will be created
 */
@CompileStatic
class DefaultRunConfigurationContainer
        extends DefaultPolymorphicDomainObjectContainer<RunConfiguration>
        implements RunConfigurationContainer {

    @Inject
    DefaultRunConfigurationContainer(Instantiator instantiator) {
        super(RunConfiguration, instantiator)
    }

    @Override
    public <T extends RunConfiguration> void defaults(Class<T> type, Action<T> action) {
        def defaults = maybeCreate("default_$type.name", type)
        defaults.defaults = true
        action.execute(defaults)
    }
}


