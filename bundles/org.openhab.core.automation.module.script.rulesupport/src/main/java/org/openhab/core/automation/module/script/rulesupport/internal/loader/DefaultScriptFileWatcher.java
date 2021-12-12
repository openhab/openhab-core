/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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

import java.io.File;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.automation.module.script.ScriptEngineManager;
import org.openhab.core.automation.module.script.rulesupport.loader.ScriptFileWatcher;
import org.openhab.core.service.ReadyService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link DefaultScriptFileWatcher} watches the jsr223 directory for files. If a new/modified file is detected, the
 * script is read and passed to the {@link ScriptEngineManager}.
 *
 * @author Jonathan Gilbert - initial contribution
 */
@NonNullByDefault
@Component(immediate = true)
public class DefaultScriptFileWatcher extends ScriptFileWatcher {

    private static final String FILE_DIRECTORY = "automation" + File.separator + "jsr223";

    @Activate
    public DefaultScriptFileWatcher(final @Reference ScriptEngineManager manager,
            final @Reference ReadyService readyService) {
        super(manager, null, readyService, FILE_DIRECTORY);
    }

    @Activate
    @Override
    public void activate() {
        super.activate();
    }

    @Deactivate
    @Override
    public void deactivate() {
        super.deactivate();
    }
}
