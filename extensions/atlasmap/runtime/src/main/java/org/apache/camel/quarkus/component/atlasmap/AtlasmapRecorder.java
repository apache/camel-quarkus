package org.apache.camel.quarkus.component.atlasmap;

import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import io.atlasmap.core.DefaultAtlasContextFactory;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.component.atlasmap.AtlasMapComponent;
import org.jboss.logging.Logger;

@Recorder
public class AtlasmapRecorder {

    public RuntimeValue<AtlasMapComponent> createAtlasmapComponent() {
        /*
         * TODO simplify once https://github.com/atlasmap/atlasmap/issues/2704 is solved
         * Currently there is no way to directly create a DefaultAtlasContextFactory with a custom compound class loader
         */
        final DefaultAtlasContextFactory cf = DefaultAtlasContextFactory.getInstance();
        cf.destroy();
        final CamelQuarkusCompoundClassLoader compoundClassLoader = new CamelQuarkusCompoundClassLoader();
        final ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        compoundClassLoader.addAlternativeLoader(tccl);
        cf.init(compoundClassLoader);

        final AtlasMapComponent component = new AtlasMapComponent();
        component.setAtlasContextFactory(cf);
        return new RuntimeValue<AtlasMapComponent>(component);
    }

    /**
     * TODO: remove once https://github.com/atlasmap/atlasmap/pull/2703 is fixed in an AtlasMap version we use
     */
    static class CamelQuarkusCompoundClassLoader extends io.atlasmap.core.CompoundClassLoader {
        private static final Logger LOG = Logger.getLogger(CamelQuarkusCompoundClassLoader.class);

        private Set<ClassLoader> delegates = new LinkedHashSet<>();

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            for (ClassLoader cl : delegates) {
                try {
                    return cl.loadClass(name);
                } catch (Throwable t) {
                    LOG.debugf(t, "Class '%s' was not found with ClassLoader '%s'", name, cl);
                }
            }
            throw new ClassNotFoundException(name);
        }

        @Override
        public URL getResource(String name) {
            for (ClassLoader cl : delegates) {
                URL url = cl.getResource(name);
                if (url != null) {
                    return url;
                }
                LOG.debugf("Resource '%s' was not found with ClassLoader '%s'", name, cl);
            }
            return null;
        }

        @Override
        public Enumeration<URL> getResources(String name) {
            List<URL> answer = new LinkedList<>();
            for (ClassLoader cl : delegates) {
                try {
                    Enumeration<URL> urls = cl.getResources(name);
                    while (urls != null && urls.hasMoreElements()) {
                        answer.add(urls.nextElement());
                    }
                } catch (Exception e) {
                    LOG.debugf(e, "I/O error while looking for a resource '%s' with ClassLoader '%s'", name, cl);
                }
            }
            return Collections.enumeration(answer);
        }

        @Override
        public synchronized void addAlternativeLoader(ClassLoader cl) {
            delegates.add(cl);
        }
    }
}
