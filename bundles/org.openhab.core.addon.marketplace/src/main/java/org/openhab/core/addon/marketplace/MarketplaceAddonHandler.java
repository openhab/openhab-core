/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.addon.marketplace;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.addon.Addon;

/**
 * This interface can be implemented by services that want to register as handlers for specific marketplace add-on
 * content types and content types.
 * In a system there should always only be exactly one handler responsible for a given type+contentType
 * combination. If
 * multiple handers support it, it is undefined which one will be called.
 * This mechanism allows solutions to add support for specific formats (e.g. Karaf features) that are not supported by
 * openHAB out of the box.
 * It also allows to decide which add-on types are made available at all.
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Yannick Schaus - refactoring
 *
 */
@NonNullByDefault
public interface MarketplaceAddonHandler {

    /**
     * Tells whether this handler supports a given add-on.
     *
     * @param addon the add-on in question
     * @return true, if the add-on is supported, false otherwise
     */
    boolean supports(String type, String contentType);

    /**
     * Tells whether a given add-on is currently installed.
     * Note: This method is only called, if the hander claimed support for the add-on before.
     *
     * @param id the id of the add-on in question
     * @return true, if the add-on is installed, false otherwise
     */
    boolean isInstalled(String id);

    /**
     * Installs a given add-on.
     * Note: This method is only called, if the hander claimed support for the add-on before.
     *
     * @param addon the add-on to install
     * @throws MarketplaceHandlerException if the installation failed for some reason
     */
    void install(Addon addon) throws MarketplaceHandlerException;

    /**
     * Uninstalls a given add-on.
     * Note: This method is only called, if the hander claimed support for the add-on before.
     *
     * @param addon the add-on to uninstall
     * @throws MarketplaceHandlerException if the uninstallation failed for some reason
     */
    void uninstall(Addon addon) throws MarketplaceHandlerException;
}
