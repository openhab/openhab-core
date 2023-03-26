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
package org.openhab.core.model.script.internal.engine.action;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.action.ScriptExecution;
import org.openhab.core.model.script.engine.action.ActionService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This class registers an OSGi service for the ScriptExecution action.
 *
 * @author Jan N. Klug - Initial contribution
 */
@Component(immediate = true)
@NonNullByDefault
public class ScriptExecutionActionService implements ActionService {

    private static @Nullable ScriptExecution scriptExecution;

    @Activate
    public ScriptExecutionActionService(final @Reference ScriptExecution scriptExecution) {
        ScriptExecutionActionService.scriptExecution = scriptExecution;
    }

    @Override
    public Class<?> getActionClass() {
        return ScriptExecution.class;
    }

    public static ScriptExecution getScriptExecution() {
        return Objects.requireNonNull(scriptExecution);
    }
}
