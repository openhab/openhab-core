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
package org.eclipse.smarthome.automation.internal.provider.file;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * This class isolates the java 1.7 functionality which tracks the file system changes.
 *
 * @author Ana Dimova - Initial Contribution
 *
 */
@SuppressWarnings("rawtypes")
public class WatchServiceUtil {

    private static final Map<AbstractFileProvider, Map<String, AutomationWatchService>> WATCH_SERVICES = new HashMap<AbstractFileProvider, Map<String, AutomationWatchService>>();

    public static void initializeWatchService(String watchingDir, AbstractFileProvider provider) {
        AutomationWatchService aws = null;
        synchronized (WATCH_SERVICES) {
            Map<String, AutomationWatchService> watchers = WATCH_SERVICES.get(provider);
            if (watchers == null) {
                watchers = new HashMap<String, AutomationWatchService>();
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
            if (watchers.size() == 0) {
                WATCH_SERVICES.remove(provider);
            }
        }
        if (aws != null) {
            aws.deactivate();
            provider.removeResources(aws.getSourcePath().toFile());
        }
    }
}
