/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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

import static java.nio.file.StandardWatchEventKinds.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.service.AbstractWatchService;

/**
 * Listens for changes to script libraries
 *
 * @author Jonathan Gilbert - Initial contribution
 */
@NonNullByDefault
public class ScriptLibraryWatcher extends AbstractWatchService {

    // package private for testing
    final Consumer<String> listener;

    public ScriptLibraryWatcher(final String libraryPath, Consumer<String> listener) {
        super(libraryPath);

        this.listener = listener;
    }

    @Override
    protected boolean watchSubDirectories() {
        return true;
    }

    @Override
    protected WatchEvent.Kind<?> @Nullable [] getWatchEventKinds(Path path) {
        return new WatchEvent.Kind<?>[] { ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY };
    }

    @Override
    protected void processWatchEvent(WatchEvent<?> watchEvent, WatchEvent.Kind<?> kind, Path path) {
        File file = path.toFile();
        if (!file.isHidden()) {
            if (kind.equals(ENTRY_DELETE)) {
                listener.accept(file.getPath());
            }

            if (file.canRead() && (kind.equals(ENTRY_CREATE) || kind.equals(ENTRY_MODIFY))) {
                listener.accept(file.getPath());
            }
        }
    }
}
