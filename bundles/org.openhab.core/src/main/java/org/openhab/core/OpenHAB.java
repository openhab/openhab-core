/**
 * Copyright (c) 2015-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.core;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.config.core.ConfigConstants;
import org.osgi.framework.FrameworkUtil;

/**
 * Some core static methods that provide information about the running openHAB instance.
 *
 * @author Kai Kreuzer
 *
 */
public class OpenHAB {

    /** the service pid used for the definition of the base package and addons */
    public static final String ADDONS_SERVICE_PID = "org.openhab.addons";

    /** the configuraton parameter name used for the base package */
    public static final String CFG_PACKAGE = "package";

    /**
     * Returns the current openHAB version, retrieving the information from the core bundle version.
     *
     * @return the openHAB runtime version
     */
    static public String getVersion() {
        String versionString = FrameworkUtil.getBundle(OpenHAB.class).getVersion().toString();
        // if the version string contains a qualifier, remove it!
        if (StringUtils.countMatches(versionString, ".") == 3) {
            versionString = StringUtils.substringBeforeLast(versionString, ".");
        }
        return versionString;
    }

    static public String buildString() {
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
