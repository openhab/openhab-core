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

import org.eclipse.smarthome.automation.template.Template;
import org.eclipse.smarthome.automation.type.ModuleType;

/**
 * Defines visibility values of {@link Rule}s, {@link ModuleType}s and {@link Template}s.
 *
 * @author Yordan Mihaylov - Initial Contribution
 *
 */
public enum Visibility {
    /**
     * The UI has always to show an object with such visibility.
     */
    VISIBLE,

    /**
     * The UI has always to hide an object with such visibility.
     */
    HIDDEN,

    /**
     * The UI has to show an object with such visibility only to experts.
     */
    EXPERT

}
