/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.io.rest;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Marker interface for an openHAB JAX RS resource that needs to be registered by the publisher.
 *
 * <p>
 * This marker is for providing OSGi services for services that should be registered by the REST publisher.
 * You should not use this. It is only for a migration path...
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
@Deprecated
public interface OhJAXRSResource {

}
