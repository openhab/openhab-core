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
package org.openhab.core.model.lazygen;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.xtext.xtext.generator.XtextGeneratorLanguage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Holger Schill, Simon Kaufmann - Initial contribution
 */
@NonNullByDefault
public class LazyLanguageConfig extends XtextGeneratorLanguage {

    private final Logger logger = LoggerFactory.getLogger(LazyLanguageConfig.class);
}
