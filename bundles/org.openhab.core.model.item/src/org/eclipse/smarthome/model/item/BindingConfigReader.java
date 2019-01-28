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
package org.eclipse.smarthome.model.item;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.model.item.internal.GenericItemProvider;

/**
 * This interface must be implemented by services, which can parse the generic
 * binding configuration string used in the {@link GenericItemProvider}.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
@NonNullByDefault
public interface BindingConfigReader {

    /**
     * This defines the type of binding this reader will process, e.g. "knx".
     *
     * @return the type of the binding
     */
    String getBindingType();

    /**
     * Validates if the type of <code>item</code> is valid for this binding.
     *
     * @param itemType the type of the item to validate
     * @param bindingConfig the config string which could be used to refine the
     *            validation
     *
     * @throws BindingConfigParseException if the item type is
     *             invalid for this binding
     */
    void validateItemType(String itemType, String bindingConfig) throws BindingConfigParseException;

    /**
     * This method is called by the {@link GenericItemProvider} whenever it comes
     * across a binding configuration string for an item.
     *
     * @param context a string of the context from where this item comes from. Usually the file name of the config
     *            file
     * @param itemType the item type for which the binding config is defined
     * @param itemName the item name for which the binding config is defined
     * @param bindingConfig the configuration string that must be processed
     *
     * @throws BindingConfigParseException if the configuration string is not valid
     */
    void processBindingConfiguration(String context, String itemType, String itemName, String bindingConfig,
            Configuration configuration) throws BindingConfigParseException;

    /**
     * Informs the reader that configurations will be processed for a given context. This is usually called if a config
     * file is reloaded, so that the old values are removed, before the new ones are processed.
     *
     * @param context the context of the configurations that will be processed
     */
    void startConfigurationUpdate(String context);

    /**
     * Informs the reader that configuration update is completed for a given context. This is usually called after a
     * config
     * file is reloaded, so that the reader can clean up afterwards.
     *
     * @param context the context of the configurations that were processed
     */
    void stopConfigurationUpdate(String context);
}
