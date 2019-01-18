/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.model.thing

import org.eclipse.smarthome.model.thing.valueconverter.ThingValueConverters
import org.eclipse.xtext.conversion.IValueConverterService
import org.eclipse.xtext.linking.lazy.LazyURIEncoder
import com.google.inject.Binder
import com.google.inject.name.Names

/** 
 * Use this class to register components to be used at runtime / without the Equinox extension registry.
 */
@SuppressWarnings("restriction") class ThingRuntimeModule extends org.eclipse.smarthome.model.thing.AbstractThingRuntimeModule {
    override Class<? extends IValueConverterService> bindIValueConverterService() {
        return ThingValueConverters
    }

    override Class<? extends org.eclipse.xtext.serializer.sequencer.ISyntacticSequencer> bindISyntacticSequencer() {
        return org.eclipse.smarthome.model.thing.serializer.ThingSyntacticSequencerExtension
    }

    override void configureUseIndexFragmentsForLazyLinking(Binder binder) {
        binder.bind(Boolean.TYPE).annotatedWith(Names.named(LazyURIEncoder.USE_INDEXED_FRAGMENTS_BINDING)).toInstance(
            Boolean.FALSE)
    }
}
