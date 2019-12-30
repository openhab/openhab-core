/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.core.internal.common;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Combines multiple class loaders into one.
 *
 * @author Markus Rathgeb - Initial contribution
 */
public class CombinedClassLoader extends ClassLoader {

    private final Logger logger = LoggerFactory.getLogger(CombinedClassLoader.class);
    private final Map<ClassLoader, Set<Class<?>>> delegateClassLoaders;

    private static String info(final ClassLoader classLoader, final Set<Class<?>> clazzes) {
        return String.format("classloader \"%s\" for \"%s\"", classLoader, clazzes);
    }

    public static CombinedClassLoader fromClasses(final ClassLoader parent, final Stream<Class<?>> delegateClasses) {
        final Map<ClassLoader, Set<Class<?>>> cls = new HashMap<>();
        delegateClasses.forEach(clazz -> {
            cls.compute(clazz.getClassLoader(), (k, v) -> {
                if (v == null) {
                    final Set<Class<?>> set = new HashSet<>();
                    set.add(clazz);
                    return set;
                } else {
                    v.add(clazz);
                    return v;
                }
            });
        });
        return new CombinedClassLoader(parent, cls);
    }

    public static CombinedClassLoader fromClassLoaders(final ClassLoader parent,
            final ClassLoader... delegateClassLoaders) {
        return fromClassLoaders(parent, Arrays.stream(delegateClassLoaders));
    }

    public static CombinedClassLoader fromClassLoaders(final ClassLoader parent,
            final Stream<ClassLoader> delegateClassLoaders) {
        return new CombinedClassLoader(parent,
                delegateClassLoaders.collect(Collectors.toMap(cl -> cl, cl -> Collections.emptySet())));
    }

    private CombinedClassLoader(ClassLoader parent, Map<ClassLoader, Set<Class<?>>> delegateClassLoaders) {
        super(parent);
        this.delegateClassLoaders = Collections.unmodifiableMap(delegateClassLoaders);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        for (final Entry<ClassLoader, Set<Class<?>>> entry : delegateClassLoaders.entrySet()) {
            try {
                final Class<?> clazz = entry.getKey().loadClass(name);
                if (logger.isDebugEnabled()) {
                    logger.debug("Loaded class \"{}\" by {}", name, info(entry.getKey(), entry.getValue()));
                }
                return clazz;
            } catch (final ClassNotFoundException ex) {
            }
        }
        throw new ClassNotFoundException(name);
    }

    @Override
    protected URL findResource(String name) {
        // Try to get the resource from one of the delegate class loaders.
        // Return the first found one.
        // If no delegate class loader can get the resource, return null.
        return delegateClassLoaders.keySet().stream().map(cl -> cl.getResource(name)).filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        final Vector<URL> vector = new Vector<>();
        for (final ClassLoader delegate : delegateClassLoaders.keySet()) {
            final Enumeration<URL> enumeration = delegate.getResources(name);
            while (enumeration.hasMoreElements()) {
                vector.add(enumeration.nextElement());
            }
        }
        return vector.elements();
    }

}
