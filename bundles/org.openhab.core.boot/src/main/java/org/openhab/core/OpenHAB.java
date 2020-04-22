/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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

import org.openhab.core.config.core.ConfigConstants;
import org.osgi.framework.FrameworkUtil;

/**
 * Some core static methods that provide information about the running openHAB instance.
 *
 * @author Kai Kreuzer - Initial contribution
 */
public class OpenHAB {

    /** the service pid used for the definition of the base package and addons */
    public static final String ADDONS_SERVICE_PID = "org.openhab.addons";

    /** the configuraton parameter name used for the base package */
    public static final String CFG_PACKAGE = "package";

    private static String substringAfterLast(String str, String separator) {
        int index = str.lastIndexOf(separator);
        return index == -1 || index == str.length() - separator.length() ? ""
                : str.substring(index + separator.length());
    }

    private static String substringBeforeLast(String str, String separator) {
        int index = str.lastIndexOf(separator);
        return index == -1 ? str : str.substring(0, index);
    }

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

    public static String buildString() {
        Properties prop = new Properties();
        Path versionFilePath = Paths.get(ConfigConstants.getUserDataFolder(), "etc", "version.properties");
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
}
