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

import static java.nio.file.StandardWatchEventKinds.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;

import org.eclipse.smarthome.core.service.AbstractWatchService;

/**
 * This class is an implementation of {@link AbstractWatchService} which is responsible for tracking changes in file
 * system by Java WatchService.
 * <p>
 * It provides functionality for tracking {@link #watchingDir} changes to import or remove the automation objects.
 *
 * @author Ana Dimova - Initial Contribution
 *
 */
@SuppressWarnings("rawtypes")
public class AutomationWatchService extends AbstractWatchService {

    private AbstractFileProvider provider;

    public AutomationWatchService(AbstractFileProvider provider, String watchingDir) {
        super(watchingDir);
        this.provider = provider;
    }

    @Override
    protected boolean watchSubDirectories() {
        return true;
    }

    @Override
    protected Kind<?>[] getWatchEventKinds(Path subDir) {
        return new Kind<?>[] { ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY };
    }

    @Override
    protected void processWatchEvent(WatchEvent<?> event, Kind<?> kind, Path path) {
        File file = path.toFile();
        if (!file.isHidden()) {
            if (kind.equals(ENTRY_DELETE)) {
                provider.removeResources(file);
            } else if (file.canRead()) {
                provider.importResources(file);
            }
        }
    }

}
