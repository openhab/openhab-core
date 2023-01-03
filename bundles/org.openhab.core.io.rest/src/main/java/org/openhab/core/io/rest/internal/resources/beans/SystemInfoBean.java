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
package org.openhab.core.io.rest.internal.resources.beans;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.OpenHAB;

/**
 * This is a java bean that is used to define system information for the REST interface.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class SystemInfoBean {

    public final SystemInfo systemInfo;

    public static class SystemInfo {
        public final String configFolder = OpenHAB.getConfigFolder();
        public final String userdataFolder = OpenHAB.getUserDataFolder();
        public final @Nullable String logFolder = System.getProperty("openhab.logdir");
        public final @Nullable String javaVersion = System.getProperty("java.version");
        public final @Nullable String javaVendor = System.getProperty("java.vendor");
        public final @Nullable String javaVendorVersion = System.getProperty("java.vendor.version");
        public final @Nullable String osName = System.getProperty("os.name");
        public final @Nullable String osVersion = System.getProperty("os.version");
        public final @Nullable String osArchitecture = System.getProperty("os.arch");
        public final int availableProcessors = Runtime.getRuntime().availableProcessors();
        public final long freeMemory = Runtime.getRuntime().freeMemory();
        public final long totalMemory = Runtime.getRuntime().totalMemory();
        public final int startLevel;

        public SystemInfo(int startLevel) {
            this.startLevel = startLevel;
        }
    }

    public SystemInfoBean(int startLevel) {
        systemInfo = new SystemInfo(startLevel);
    }
}
