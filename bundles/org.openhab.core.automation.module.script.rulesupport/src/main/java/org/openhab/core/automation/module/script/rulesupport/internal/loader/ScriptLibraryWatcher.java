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

package org.openhab.core.automation.module.script.rulesupport.internal.loader;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.WatchEvent;

import org.openhab.core.OpenHAB;
import org.openhab.core.service.AbstractWatchService;

/**
 * Listens for changes to script libraries
 *
 * @author Jonathan Gilbert
 */
abstract class ScriptLibraryWatcher extends AbstractWatchService {

    public static final String LIB_PATH = String.join(File.separator, OpenHAB.getConfigFolder(), "automation", "lib",
            "javascript");

    ScriptLibraryWatcher() {
        super(LIB_PATH);
    }

    @Override
    protected boolean watchSubDirectories() {
        return true;
    }

    @Override
    protected WatchEvent.Kind<?>[] getWatchEventKinds(Path path) {
        return new WatchEvent.Kind<?>[] { ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY };
    }

    @Override
    protected void processWatchEvent(WatchEvent<?> watchEvent, WatchEvent.Kind<?> kind, Path path) {
        File file = path.toFile();
        if (!file.isHidden()) {
            if (kind.equals(ENTRY_DELETE)) {
                this.updateFile(file.getPath());
            }

            if (file.canRead() && (kind.equals(ENTRY_CREATE) || kind.equals(ENTRY_MODIFY))) {
                this.updateFile(file.getPath());
            }
        }
    }

    abstract void updateFile(String filePath);
}
