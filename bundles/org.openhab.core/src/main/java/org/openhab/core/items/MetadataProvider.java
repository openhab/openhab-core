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
package org.openhab.core.items;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Provider;

/**
 * This is a marker interface for metadata provider implementations that should be used to register those as an OSGi
 * service.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public interface MetadataProvider extends Provider<Metadata> {

}
