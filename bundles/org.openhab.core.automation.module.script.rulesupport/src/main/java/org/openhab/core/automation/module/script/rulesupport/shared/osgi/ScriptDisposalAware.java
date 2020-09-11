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
package org.openhab.core.automation.module.script.rulesupport.shared.osgi;

/**
 * Interface denoting an object which is aware of script disposal events.
 *
 * @author Jonathan Gilbert
 */
public interface ScriptDisposalAware {
    /**
     * Script with supplied name has been disposed
     * 
     * @param scriptIdentifier the identifier of the script which has been disposed
     */
    void unload(String scriptIdentifier);
}
