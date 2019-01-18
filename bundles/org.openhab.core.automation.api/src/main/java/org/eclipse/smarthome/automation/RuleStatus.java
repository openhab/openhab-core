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
package org.eclipse.smarthome.automation;

/**
 * This enumeration is used to present the main status of a {@link Rule}.
 * <table>
 * <caption><b>Rule Status transitions</b></caption>
 * <tr>
 * </tr>
 * <tr>
 * <td><b>From/To</b></td>
 * <td><b>{@link #UNINITIALIZED}</b></td>
 * <td><b>{@link #INITIALIZING}</b></td>
 * <td><b>{@link #IDLE}</b></td>
 * <td><b>{@link #RUNNING}</b></td>
 * </tr>
 * <td></td>
 * <tr>
 * <td><b>{@link #UNINITIALIZED}</b></td>
 * <td><b>N/A</b></td>
 * <td>
 * <li><b>Add:</b> Rule, ModuleHandler, ModuleType, Template</li>
 * <li><b>Update:</b> Rule</li></td>
 * <td><b>N/A</b></td>
 * <td><b>N/A</b></td>
 * </tr>
 * <td></td>
 * <tr>
 * <td><b>{@link #INITIALIZING}</b></td>
 * <td>Resolving fails, Disable rule</td>
 * <td><b>N/A</b></td>
 * <td>Resolving succeeds</td>
 * <td><b>N/A</b></td>
 * </tr>
 * <td></td>
 * <tr>
 * <td><b>{@link #IDLE}</b></td>
 * <td>
 * <li><b>Remove:</b> Rule, ModuleHandler</li>
 * <li><b>Update:</b> ModuleType</li>
 * <li><b>Disable:</b> Rule</li></td>
 * <td><b>N/A</b></td>
 * <td><b>N/A</b></td>
 * <td>
 * <li>Triggered</li>
 * <li><b>{@link RuleRegistry#runNow(String) runNow}</b></li></td>
 * </tr>
 * <td></td>
 * <tr>
 * <td><b>{@link #RUNNING}</b></td>
 * <td>
 * <li><b>Remove:</b> Rule, ModuleHandler</li>
 * <li><b>Update:</b> ModuleType</li>
 * <li><b>Disable:</b> Rule</li></td>
 * <td><b>N/A</b></td>
 * <td>Execution finished</td>
 * <td><b>N/A</b></td>
 * </tr>
 * </table>
 *
 * @author Yordan Mihaylov - Initial contribution
 * @author Kai Kreuzer - Refactored to match ThingStatus implementation
 * @author Ana Dimova - add java doc
 */
public enum RuleStatus {
    UNINITIALIZED(1),
    INITIALIZING(2),
    IDLE(3),
    RUNNING(4);

    private final int value;

    private RuleStatus(final int newValue) {
        value = newValue;
    }

    /**
     * Gets the value of a rule status.
     *
     * @return the value
     */
    public int getValue() {
        return value;
    }
}
