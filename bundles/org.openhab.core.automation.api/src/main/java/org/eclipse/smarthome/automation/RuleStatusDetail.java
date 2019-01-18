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
 * This enumeration is used to represent a detail of a {@link RuleStatus}. It can be considered as a sub-status.
 * It shows the specific reasons why the status of the rule is like as is.
 *
 * <table>
 * <caption><b>Rule Status Details</b></caption>
 * <td></td>
 * <tr>
 * <td><b>Detail/Status</b></td>
 * <td><b>{@link RuleStatus#UNINITIALIZED UNINITIALIZED}</b></td>
 * <td><b>{@link RuleStatus#INITIALIZING INITIALIZING}</b></td>
 * <td><b>{@link RuleStatus#IDLE IDLE}</b></td>
 * <td><b>{@link RuleStatus#RUNNING RUNNING}</b></td>
 * </tr>
 * <tr>
 * <td><b>{@link #NONE}</b></td>
 * <td>Initial State</td>
 * <td>Resolving started</td>
 * <td>Successfully resolved</td>
 * <td>Running</td>
 * </tr>
 * <tr>
 * <td><b>{@link #CONFIGURATION_ERROR}</b></td>
 * <td>Resolving failed</td>
 * <td><b>N/A</b></td>
 * <td><b>N/A</b></td>
 * <td><b>N/A</b></td>
 * </tr>
 * <tr>
 * <td><b>{@link #HANDLER_INITIALIZING_ERROR}</b></td>
 * <td>Resolving failed</td>
 * <td><b>N/A</b></td>
 * <td><b>N/A</b></td>
 * <td><b>N/A</b></td>
 * </tr>
 * <tr>
 * <td><b>{@link #HANDLER_MISSING_ERROR}</b></td>
 * <td>Resolving failed</td>
 * <td><b>N/A</b></td>
 * <td><b>N/A</b></td>
 * <td><b>N/A</b></td>
 * </tr>
 * <tr>
 * <td><b>{@link #TEMPLATE_MISSING_ERROR}</b></td>
 * <td>Resolving failed</td>
 * <td><b>N/A</b></td>
 * <td><b>N/A</b></td>
 * <td><b>N/A</b></td>
 * </tr>
 * <tr>
 * <td><b>{@link #INVALID_RULE}</b></td>
 * <td>Resolving failed</td>
 * <td><b>N/A</b></td>
 * <td><b>N/A</b></td>
 * <td><b>N/A</b></td>
 * </tr>
 * <tr>
 * <td><b>{@link #DISABLED}</b></td>
 * <td>Disabled</td>
 * <td><b>N/A</b></td>
 * <td><b>N/A</b></td>
 * <td><b>N/A</b></td>
 * </tr>
 * </table>
 *
 * @author Yordan Mihaylov - Initial contribution
 * @author Kai Kreuzer - Refactored to match ThingStatusDetail implementation
 * @author Ana Dimova - add java doc
 */
public enum RuleStatusDetail {
    NONE(0),
    HANDLER_MISSING_ERROR(1),
    HANDLER_INITIALIZING_ERROR(2),
    CONFIGURATION_ERROR(3),
    TEMPLATE_MISSING_ERROR(4),
    INVALID_RULE(5),
    DISABLED(6);

    private final int value;

    private RuleStatusDetail(final int newValue) {
        value = newValue;
    }

    /**
     * Gets the value of the status detail.
     *
     * @return the value of the status detail.
     */
    public int getValue() {
        return value;
    }
}
