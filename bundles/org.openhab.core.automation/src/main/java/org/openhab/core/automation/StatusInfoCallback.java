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
package org.openhab.core.automation;

/**
 * This interface is used by {@link RuleRegistry} implementation to be notified of changes related to statuses of rules.
 *
 * @author Yordan Mihaylov - Initial contribution
 */
public interface StatusInfoCallback {

    /**
     * The method is called when the rule has update of its status.
     *
     * @param ruleUID UID of the {@link Rule}
     * @param statusInfo new status info releated to the {@link Rule}
     */
    void statusInfoChanged(String ruleUID, RuleStatusInfo statusInfo);
}
