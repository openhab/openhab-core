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

import org.eclipse.smarthome.core.common.registry.Provider;

/**
 * This class is responsible for providing {@link Rule}s. {@link RuleProvider}s are tracked by the {@link RuleRegistry}
 * service, which collect all rules from different providers of the same type.
 *
 * @author Kai Kreuzer - Initial contribution
 */
public interface RuleProvider extends Provider<Rule> {

}
