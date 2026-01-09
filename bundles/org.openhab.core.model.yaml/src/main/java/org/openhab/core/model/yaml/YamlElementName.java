/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.model.yaml;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The {@link YamlElementName} is a required annotation for the inheritors of {@link YamlElement}. It specifies the root
 * element name in a YAML model that is described by the respective class. Code review MUST ensure that element names
 * are unique.
 *
 * @author Jan N. Klug - Initial contribution
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface YamlElementName {
    String value();
}
