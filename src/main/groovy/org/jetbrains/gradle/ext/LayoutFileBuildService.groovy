package org.jetbrains.gradle.ext

import groovy.transform.CompileStatic
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

@CompileStatic
abstract class LayoutFileBuildService implements BuildService<Params>, AutoCloseable {
    interface Params extends BuildServiceParameters {
        Property<File> getLayoutFile();
    }

    File getFilePath() {
        return getParameters().getLayoutFile().get()
    }

    @Override
    void close() throws Exception {
        def file = getParameters().getLayoutFile().get()
        if (file != null && file.exists()) {
            file.delete()
        }
    }
}
