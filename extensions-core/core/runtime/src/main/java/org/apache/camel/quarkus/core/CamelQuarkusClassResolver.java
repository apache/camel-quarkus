package org.apache.camel.quarkus.core;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import org.apache.camel.spi.ClassResolver;
import org.apache.camel.util.CastUtils;
import org.apache.camel.util.ObjectHelper;

public class CamelQuarkusClassResolver implements ClassResolver {

    private final ClassLoader applicationContextClassLoader;

    public CamelQuarkusClassResolver(ClassLoader applicationContextClassLoader) {
        this.applicationContextClassLoader = applicationContextClassLoader;
    }

    @Override
    public Class<?> resolveClass(String name) {
        return loadClass(name, applicationContextClassLoader);
    }

    @Override
    public <T> Class<T> resolveClass(String name, Class<T> type) {
        return CastUtils.cast(loadClass(name, applicationContextClassLoader));
    }

    @Override
    public Class<?> resolveClass(String name, ClassLoader loader) {
        return loadClass(name, loader);
    }

    @Override
    public <T> Class<T> resolveClass(String name, Class<T> type, ClassLoader loader) {
        return CastUtils.cast(loadClass(name, loader));
    }

    @Override
    public Class<?> resolveMandatoryClass(String name) throws ClassNotFoundException {
        Class<?> answer = resolveClass(name);
        if (answer == null) {
            throw new ClassNotFoundException(name);
        }
        return answer;
    }

    @Override
    public <T> Class<T> resolveMandatoryClass(String name, Class<T> type) throws ClassNotFoundException {
        Class<T> answer = resolveClass(name, type);
        if (answer == null) {
            throw new ClassNotFoundException(name);
        }
        return answer;
    }

    @Override
    public Class<?> resolveMandatoryClass(String name, ClassLoader loader) throws ClassNotFoundException {
        Class<?> answer = resolveClass(name, loader);
        if (answer == null) {
            throw new ClassNotFoundException(name);
        }
        return answer;
    }

    @Override
    public <T> Class<T> resolveMandatoryClass(String name, Class<T> type, ClassLoader loader) throws ClassNotFoundException {
        Class<T> answer = resolveClass(name, type, loader);
        if (answer == null) {
            throw new ClassNotFoundException(name);
        }
        return answer;
    }

    @Override
    public InputStream loadResourceAsStream(String uri) {
        return ObjectHelper.loadResourceAsStream(uri, applicationContextClassLoader);
    }

    @Override
    public URL loadResourceAsURL(String uri) {
        return ObjectHelper.loadResourceAsURL(uri, applicationContextClassLoader);
    }

    @Override
    public Enumeration<URL> loadResourcesAsURL(String uri) {
        return loadAllResourcesAsURL(uri);
    }

    @Override
    public Enumeration<URL> loadAllResourcesAsURL(String uri) {
        return ObjectHelper.loadResourcesAsURL(uri);
    }

    protected Class<?> loadClass(String name, ClassLoader loader) {
        return ObjectHelper.loadClass(name, loader);
    }

}
