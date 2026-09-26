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
package org.openhab.core.model.thing;

import org.eclipse.xtext.conversion.IValueConverterService;
import org.eclipse.xtext.formatting.IFormatter;
import org.eclipse.xtext.linking.lazy.LazyURIEncoder;
import org.openhab.core.model.thing.formatting.ThingFormatter;
import org.openhab.core.model.thing.valueconverter.ThingValueConverters;

import com.google.inject.Binder;
import com.google.inject.name.Names;

/**
 * Use this class to register components to be used at runtime / without the Equinox extension registry.
 */
public class ThingRuntimeModule extends AbstractThingRuntimeModule {
    @Override
    public Class<? extends IValueConverterService> bindIValueConverterService() {
        return ThingValueConverters.class;
    }

    @Override
    public Class<? extends IFormatter> bindIFormatter() {
        return ThingFormatter.class;
    }

    @Override
    public void configureUseIndexFragmentsForLazyLinking(final Binder binder) {
        binder.<Boolean> bind(Boolean.TYPE).annotatedWith(Names.named(LazyURIEncoder.USE_INDEXED_FRAGMENTS_BINDING))
                .toInstance(Boolean.FALSE);
    }
}
