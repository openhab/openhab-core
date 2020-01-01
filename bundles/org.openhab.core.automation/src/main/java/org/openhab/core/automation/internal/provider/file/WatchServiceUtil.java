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
package org.openhab.core.automation.internal.provider.file;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * This class isolates the java 1.7 functionality which tracks the file system changes.
 *
 * @author Ana Dimova - Initial contribution
 */
@SuppressWarnings("rawtypes")
public class WatchServiceUtil {

    private static final Map<AbstractFileProvider, Map<String, AutomationWatchService>> WATCH_SERVICES = new HashMap<>();

    public static void initializeWatchService(String watchingDir, AbstractFileProvider provider) {
        AutomationWatchService aws = null;
        synchronized (WATCH_SERVICES) {
            Map<String, AutomationWatchService> watchers = WATCH_SERVICES.get(provider);
            if (watchers == null) {
                watchers = new HashMap<>();
                WATCH_SERVICES.put(provider, watchers);
            }
            if (watchers.get(watchingDir) == null) {
                aws = new AutomationWatchService(provider, watchingDir);
                watchers.put(watchingDir, aws);
            }
        }
        if (aws != null) {
            aws.activate();
            provider.importResources(new File(watchingDir));
        }
    }

    public static void deactivateWatchService(String watchingDir, AbstractFileProvider provider) {
        AutomationWatchService aws;
        synchronized (WATCH_SERVICES) {
            Map<String, AutomationWatchService> watchers = WATCH_SERVICES.get(provider);
            aws = watchers.remove(watchingDir);
            if (watchers.isEmpty()) {
                WATCH_SERVICES.remove(provider);
            }
        }
        if (aws != null) {
            aws.deactivate();
            provider.removeResources(aws.getSourcePath().toFile());
        }
    }
}
