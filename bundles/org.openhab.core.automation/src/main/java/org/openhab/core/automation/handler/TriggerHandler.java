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
package org.openhab.core.automation.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * This Handler interface is used by the RuleManager to set a callback interface to
 * itself. The callback has to implemented {@link TriggerHandlerCallback} interface
 * and it is used to notify the RuleManager when {@link TriggerHandler} was triggered
 *
 * @author Yordan Mihaylov - Initial contribution
 * @author Ana Dimova - Initial contribution
 * @author Vasil Ilchev - Initial contribution
 * @see ModuleHandler
 */
@NonNullByDefault
public interface TriggerHandler extends ModuleHandler {

}
