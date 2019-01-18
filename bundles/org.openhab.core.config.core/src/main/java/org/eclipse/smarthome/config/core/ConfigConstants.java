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
package org.eclipse.smarthome.config.core;

/**
 * This class provides constants relevant for the configuration of Eclipse SmartHome
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
public class ConfigConstants {

    /** The program argument name for setting the user data directory path */
    public static final String USERDATA_DIR_PROG_ARGUMENT = "smarthome.userdata";

    /** The program argument name for setting the main config directory path */
    public static final String CONFIG_DIR_PROG_ARGUMENT = "smarthome.configdir";

    /** The default main configuration directory name */
    public static final String DEFAULT_CONFIG_FOLDER = "conf";

    /** The default user data directory name */
    public static final String DEFAULT_USERDATA_FOLDER = "userdata";

    /** The property to recognize a service instance created by a service factory */
    public static final String SERVICE_CONTEXT = "esh.servicecontext";

    /** The property to separate service PIDs from their contexts */
    public static final String SERVICE_CONTEXT_MARKER = "#";

    /**
     * Returns the configuration folder path name. The main config folder <code>&lt;smarthome&gt;/config</code> can be
     * overwritten by setting
     * the System property <code>smarthome.configdir</code>.
     *
     * @return the configuration folder path name
     */
    public static String getConfigFolder() {
        String progArg = System.getProperty(CONFIG_DIR_PROG_ARGUMENT);
        if (progArg != null) {
            return progArg;
        } else {
            return DEFAULT_CONFIG_FOLDER;
        }
    }

    /**
     * Returns the user data folder path name. The main user data folder <code>&lt;smarthome&gt;/userdata</code> can be
     * overwritten by setting
     * the System property <code>smarthome.userdata</code>.
     *
     * @return the user data folder path name
     */
    public static String getUserDataFolder() {
        String progArg = System.getProperty(USERDATA_DIR_PROG_ARGUMENT);
        if (progArg != null) {
            return progArg;
        } else {
            return DEFAULT_USERDATA_FOLDER;
        }
    }
}
