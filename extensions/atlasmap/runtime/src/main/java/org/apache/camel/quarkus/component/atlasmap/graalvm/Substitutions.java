package org.apache.camel.quarkus.component.atlasmap.graalvm;

import java.util.Set;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

public class Substitutions {
}

@TargetClass(className = "io.atlasmap.core.DefaultAtlasCompoundClassLoader")
final class DefaultAtlasCompoundClassLoader_Substitute {
    @Alias
    private Set<ClassLoader> delegates;

    @Substitute
    public synchronized void addAlternativeLoader(ClassLoader cl) {
        delegates.add(Thread.currentThread().getContextClassLoader());
    }
}
