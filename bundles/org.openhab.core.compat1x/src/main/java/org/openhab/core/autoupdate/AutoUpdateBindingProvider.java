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
package org.openhab.core.autoupdate;

import org.openhab.core.binding.BindingProvider;

/**
 * This interface is implemented by classes that can provide configuration
 * information of the AutoUpdate feature.
 *
 * Implementing classes should register themselves as a service in order to be
 * taken into account.
 *
 * @author Thomas.Eichstaedt-Engelen
 */
public interface AutoUpdateBindingProvider extends BindingProvider {

    /**
     * Indicates whether an Item with the given <code>itemName</code> is
     * configured to automatically update it's State after receiving a Command
     * or not.
     *
     * @param itemName the name of the Item for which to find the configuration
     * @return <code>false</code> to disable the automatic update,
     *         <code>true</code> to enable the automatic update and <code>null</code>
     *         if there is no configuration for this item.
     */
    Boolean autoUpdate(String itemName);

}
