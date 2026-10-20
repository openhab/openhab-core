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
package org.openhab.core.config.core.xml.util;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link ConversionException} is thrown if the content of an XML document within its root element is invalid,
 * cannot be parsed or cannot be converted into the according objects.
 *
 * @author Holger Friedrich - Initial contribution
 */
@NonNullByDefault
public class ConversionException extends XmlDocumentException {

    private static final long serialVersionUID = 1L;

    public ConversionException(String message) {
        super(message);
    }

    public ConversionException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
