/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.model.core.types;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Objects;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.access.IJvmTypeProvider;
import org.eclipse.xtext.common.types.access.impl.URIHelperConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Tests for {@link DisposableTypeProviderFactory}.
 *
 * @author Thomas Leber - Initial contribution
 */
@NonNullByDefault
public class DisposableTypeProviderFactoryTest {

    private @NonNullByDefault({}) Injector injector;
    private @NonNullByDefault({}) DisposableTypeProviderFactory factory;
    private @NonNullByDefault({}) ResourceSet resourceSet;

    @BeforeEach
    public void setUp() {
        injector = createInjector();
        factory = injector.getInstance(DisposableTypeProviderFactory.class);
        resourceSet = new ResourceSetImpl();
    }

    private Injector createInjector() {
        return Guice.createInjector(binder -> binder.bind(ClassLoader.class)
                .toInstance(Objects.requireNonNull(DisposableTypeProviderFactoryTest.class.getClassLoader())));
    }

    private @Nullable Object registeredTypeProvider() {
        return resourceSet.getResourceFactoryRegistry().getProtocolToFactoryMap().get(URIHelperConstants.PROTOCOL);
    }

    private long typeResourceCount() {
        return resourceSet.getResources().stream()
                .filter(resource -> URIHelperConstants.PROTOCOL.equals(resource.getURI().scheme())).count();
    }

    private JvmType loadType(Class<?> type) {
        Resource resource = resourceSet.getResource(URIHelperConstants.OBJECTS_URI.appendSegment(type.getName()), true);
        return (JvmType) resource.getContents().get(0);
    }

    @Test
    public void typeProviderIsRegisteredInResourceSetAndReused() {
        IJvmTypeProvider typeProvider = factory.findOrCreateTypeProvider(resourceSet);

        assertThat(registeredTypeProvider(), is(sameInstance(typeProvider)));
        assertThat(factory.findOrCreateTypeProvider(resourceSet), is(sameInstance(typeProvider)));
    }

    @Test
    public void disposeRemovesOwnTypeProviderAndTypeResources() {
        factory.findOrCreateTypeProvider(resourceSet);

        assertThat(loadType(String.class), is(notNullValue()));

        Resource model = new ResourceImpl(URI.createURI("file:/test.rules"));
        resourceSet.getResources().add(model);

        assertThat(typeResourceCount(), is(not(0L)));

        factory.dispose();

        assertThat(registeredTypeProvider(), is(nullValue()));
        assertThat(typeResourceCount(), is(0L));
        assertThat(resourceSet.getResources(), hasItem(model));
    }

    @Test
    public void disposeKeepsTypeProviderOfOtherFactory() {
        DisposableTypeProviderFactory otherFactory = createInjector().getInstance(DisposableTypeProviderFactory.class);
        factory.createTypeProvider(resourceSet);
        IJvmTypeProvider otherTypeProvider = otherFactory.createTypeProvider(resourceSet);

        assertThat(loadType(String.class), is(notNullValue()));

        long typeResources = typeResourceCount();

        factory.dispose();

        assertThat(registeredTypeProvider(), is(sameInstance(otherTypeProvider)));
        assertThat(typeResourceCount(), is(typeResources));
    }

    @Test
    public void newTypeProviderIsCreatedAfterDispose() {
        IJvmTypeProvider typeProvider = factory.findOrCreateTypeProvider(resourceSet);

        factory.dispose();
        IJvmTypeProvider newTypeProvider = factory.findOrCreateTypeProvider(resourceSet);

        assertThat(newTypeProvider, is(not(sameInstance(typeProvider))));
        assertThat(registeredTypeProvider(), is(sameInstance(newTypeProvider)));

        JvmType type = loadType(String.class);

        assertThat(type, is(notNullValue()));
        assertThat(type.eResource().getResourceSet(), is(sameInstance(resourceSet)));
    }

    @Test
    public void factoryIsSingletonPerInjector() {
        assertThat(injector.getInstance(DisposableTypeProviderFactory.class), is(sameInstance(factory)));
    }
}
