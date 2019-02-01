/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.core.io.json.gson.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openhab.core.io.json.JsonBindingService;
import org.openhab.core.io.json.gson.GsonBindingService;
import org.openhab.core.io.json.gson.GsonTypeAdapterProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON data binding implementation that uses Google Gson to serialize and deserialize between
 * Java objects and JSON.
 *
 * @author Flavio Costa - Initial contribution and API
 *
 * @param <T> Type to be serialized or deserialized.
 */
@Component(immediate = true, service = JsonBindingService.class)
public class InternalGsonBindingService<T> extends GsonBindingService<T> {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(InternalGsonBindingService.class);

    /**
     * Providers for Gson type adapters.
     */
    private final List<GsonTypeAdapterProvider> providers = new ArrayList<>();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addTypeAdapterProvider(GsonTypeAdapterProvider provider) {
        providers.add(provider);
    }

    public void removeTypeAdapterProvider(GsonTypeAdapterProvider provider) {
        providers.remove(provider);
    }

    @Activate
    public void activate(Map<String, Object> properties) {
        Boolean prettyPrinting = logger.isDebugEnabled();
        if (prettyPrinting) {
            // produces formatted output, should be disabled if not in debug mode
            setFormattedOutput(prettyPrinting);
            logger.debug("JSON output formatting is activated");
        }
        // add type adapters to the configuration
        providers.stream().map(GsonTypeAdapterProvider::getTypeAdapters).map(Map::entrySet)
                .forEach(this::registerTypeAdapters);
    }
}
