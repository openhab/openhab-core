/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link YamlParseException} is used when an error is detected when parsing the content
 * of a YAML configuration file.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class YamlParseException extends Exception {

    private static final long serialVersionUID = 1L;

    public YamlParseException(String message) {
        super(message);
    }

    public YamlParseException(Throwable cause) {
        super(cause);
    }

    public YamlParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
