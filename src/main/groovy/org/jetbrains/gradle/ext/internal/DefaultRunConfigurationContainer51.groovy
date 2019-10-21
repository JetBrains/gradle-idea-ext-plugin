package org.jetbrains.gradle.ext.internal

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.internal.CollectionCallbackActionDecorator
import org.gradle.api.internal.DefaultPolymorphicDomainObjectContainer
import org.gradle.internal.reflect.Instantiator
import org.jetbrains.gradle.ext.RunConfiguration
import org.jetbrains.gradle.ext.RunConfigurationContainer

import javax.inject.Inject

@CompileStatic
class DefaultRunConfigurationContainer51 extends DefaultPolymorphicDomainObjectContainer<RunConfiguration>
        implements RunConfigurationContainer {

    @Inject
    DefaultRunConfigurationContainer51(Instantiator instantiator) {
        super(RunConfiguration, instantiator, CollectionCallbackActionDecorator.NOOP)
    }

    @Override
    public <T extends RunConfiguration> void defaults(Class<T> type, Action<T> action) {
        def defaults = maybeCreate("default_$type.name", type)
        defaults.defaults = true
        action.execute(defaults)
    }
}