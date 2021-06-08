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
import org.openhab.core.OpenHAB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MacUpdater} is the shell script for updating openHAB on this OS resp. Package Manager.
 *
 * @author Andrew Fiddian-Green - initial contribution
 */
@NonNullByDefault
public class MacUpdater extends BaseUpdater {

    private final Logger logger = LoggerFactory.getLogger(MacUpdater.class);

    private static final String EXEC_FOLDER = OpenHAB.getUserDataFolder().replace("/userdata", "");
    private static final String EXEC_FILENAME = FILE_ID + ".sh";
    private static final String EXEC_COMMAND = "echo " + PlaceHolder.PASSWORD.key + " | sudo -S -k nohup sh "
            + EXEC_FILENAME + " >/dev/null 2>&1";
    private static final String RUNTIME_FOLDER = OpenHAB.getUserDataFolder().replace("userdata", "runtime");

    /**
     * This updater class is not been fully implemented and tested, so in the meantime log this fact.
     */
    @Override
    protected void initializeExtendedPlaceholders() throws UnsupportedOperationException {
        placeHolders.put(PlaceHolder.EXEC_FOLDER, EXEC_FOLDER);
        placeHolders.put(PlaceHolder.EXEC_FILENAME, EXEC_FILENAME);
        placeHolders.put(PlaceHolder.EXEC_COMMAND, EXEC_COMMAND);
        placeHolders.put(PlaceHolder.RUNTIME_FOLDER, RUNTIME_FOLDER);

        logger.warn("Sorry, the updater for macOS has not yet been fully implemented.");
    }
}
