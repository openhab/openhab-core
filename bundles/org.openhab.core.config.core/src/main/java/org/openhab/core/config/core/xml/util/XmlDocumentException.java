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
 * The {@link XmlDocumentException} is thrown if an XML document cannot be read at all, i.e. if it cannot be opened,
 * if its root element cannot be parsed or if its root element is not supported.
 * <p>
 * Errors within the root element are reported by the subclass {@link ConversionException}. This corresponds to the
 * distinction between {@code XStreamException} and {@code ConversionException} of XStream, which was used before.
 *
 * @author Holger Friedrich - Initial contribution
 */
@NonNullByDefault
public class XmlDocumentException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public XmlDocumentException(String message) {
        super(message);
    }

    public XmlDocumentException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
