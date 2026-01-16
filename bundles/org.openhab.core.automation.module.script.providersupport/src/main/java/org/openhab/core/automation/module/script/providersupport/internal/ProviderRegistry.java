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
package org.openhab.core.automation.module.script.providersupport.internal;

/**
 * Interface to be implemented by all {@link org.openhab.core.common.registry.Registry} (delegates) that are used to
 * provide openHAB entities from scripts.
 *
 * @author Florian Hotze - Initial contribution
 */
public interface ProviderRegistry {

    /**
     * Removes all elements that are provided by the script the {@link ProviderRegistry} instance is bound to.
     * To be called when the script is unloaded or reloaded.
     */
    void removeAllAddedByScript();
}
