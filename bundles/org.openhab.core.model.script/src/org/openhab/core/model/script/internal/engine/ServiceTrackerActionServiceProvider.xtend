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
package org.openhab.core.model.script.internal.engine;

import com.google.inject.Singleton
import org.openhab.core.model.script.ScriptServiceUtil
import org.openhab.core.model.script.engine.IActionServiceProvider

@Singleton
class ServiceTrackerActionServiceProvider implements IActionServiceProvider {

    val ScriptServiceUtil scriptServiceUtil
    
    new(ScriptServiceUtil scriptServiceUtil) {
        this.scriptServiceUtil = scriptServiceUtil;
    }

	override get() {
		return scriptServiceUtil.getActionServiceInstances();
	}

}
