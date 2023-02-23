/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package com.acme;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.core.xml.util.XmlDocumentReader;

/**
 * A class that is in a non-framework package.
 *
 * Used to test if the XStream security configuration in the {@link XmlDocumentReader} forbids deserialization.
 *
 * @author Wouter Born - Initial contribution
 */
@NonNullByDefault
public class Product {
}
