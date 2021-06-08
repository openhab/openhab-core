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
package org.openhab.core.io.rest.update.internal.updaters;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link YumUpdater} is the shell script for updating openHAB on this OS resp. Package Manager.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class YumUpdater extends DebianUpdater {
    /*
     * This updater operates exactly the same way as the Debian updater. The only difference is that it uses a different
     * script in the '/scripts/YumUpdater.txt' resource (which is why we need it to have a different class name to find
     * the respective resource).
     */

    private final Logger logger = LoggerFactory.getLogger(YumUpdater.class);

    /**
     * This updater's script has not yet been implemented, so log an error and throw an exception.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    protected void initializeExtendedPlaceholders() throws UnsupportedOperationException {
        UnsupportedOperationException e = new UnsupportedOperationException(
                "Sorry, the updater for the 'Yum' Package Manager has not yet been implemented.");
        logger.warn("{}", e.getMessage());
        throw e;
    }
}
