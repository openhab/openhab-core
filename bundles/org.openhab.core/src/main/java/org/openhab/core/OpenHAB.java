/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.osgi.framework.FrameworkUtil;

/**
 * Some core static methods that provide information about the running openHAB instance.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class OpenHAB {

    /** The program argument name for setting the user data directory path */
    public static final String USERDATA_DIR_PROG_ARGUMENT = "openhab.userdata";

    /** The program argument name for setting the main config directory path */
    public static final String CONFIG_DIR_PROG_ARGUMENT = "openhab.conf";

    /** The default main configuration directory name */
    public static final String DEFAULT_CONFIG_FOLDER = "conf";

    /** The default user data directory name */
    public static final String DEFAULT_USERDATA_FOLDER = "userdata";

    /** The property to recognize a service instance created by a service factory */
    public static final String SERVICE_CONTEXT = "openhab.servicecontext";

    /** The property to separate service PIDs from their contexts */
    public static final String SERVICE_CONTEXT_MARKER = "#";

    /** the service pid used for the definition of the base package and add-ons */
    public static final String ADDONS_SERVICE_PID = "org.openhab.addons";

    /** the configuraton parameter name used for the base package */
    public static final String CFG_PACKAGE = "package";

    /**
     * Returns the current openHAB version, retrieving the information from the core bundle version.
     *
     * @return the openHAB runtime version
     */
    public static String getVersion() {
        String versionString = FrameworkUtil.getBundle(OpenHAB.class).getVersion().toString();
        // if the version string contains a "snapshot" qualifier, remove it!
        if (versionString.chars().filter(ch -> ch == '.').count() == 3) {
            final Pattern pattern = Pattern.compile("\\d+(\\.\\d+)?");
            String qualifier = substringAfterLast(versionString, ".");
            if (pattern.matcher(qualifier).matches() || "qualifier".equals(qualifier)) {
                versionString = substringBeforeLast(versionString, ".");
            }
        }
        return versionString;
    }

    /**
     * Provides the build number as it can be found in the version.properties file.
     *
     * @return The build string or "Unknown Build No." if none can be identified.
     */
    public static String buildString() {
        Properties prop = new Properties();
        Path versionFilePath = Paths.get(getUserDataFolder(), "etc", "version.properties");
        try (FileInputStream fis = new FileInputStream(versionFilePath.toFile())) {
            prop.load(fis);
            String buildNo = prop.getProperty("build-no");
            if (buildNo != null && !buildNo.isEmpty()) {
                return buildNo;
            }
        } catch (Exception e) {
            // ignore if the file is not there or not readable
        }
        return "Unknown Build No.";
    }

    /**
     * Returns the configuration folder path name. The main config folder <code>&lt;openhab-home&gt;/conf</code> can be
     * overwritten by setting the System property <code>openhab.conf</code>.
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
     * Returns the user data folder path name. The main user data folder <code>&lt;openhab-home&gt;/userdata</code> can
     * be overwritten by setting the System property <code>openhab.userdata</code>.
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

    private static String substringAfterLast(String str, String separator) {
        int index = str.lastIndexOf(separator);
        return index == -1 || index == str.length() - separator.length() ? ""
                : str.substring(index + separator.length());
    }

    private static String substringBeforeLast(String str, String separator) {
        int index = str.lastIndexOf(separator);
        return index == -1 ? str : str.substring(0, index);
    }
}
