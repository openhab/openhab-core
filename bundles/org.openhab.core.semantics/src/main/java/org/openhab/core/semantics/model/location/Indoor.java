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
package org.openhab.core.semantics.model.location;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.semantics.model.Location;
import org.openhab.core.semantics.model.TagInfo;

/**
 * This class defines a Indoor.
 *
 * @author Generated from generateTagClasses.groovy - Initial contribution
 */
@NonNullByDefault
@TagInfo(id = "Location_Indoor", label = "Indoor", synonyms = "", description = "Anything that is inside a closed building")
public interface Indoor extends Location {
}
