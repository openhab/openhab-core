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
package org.openhab.core.magic.binding;

import java.net.URI;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.core.ConfigOptionProvider;

/**
 * A public interface for a service from this virtual bundle which is also a {@link ConfigOptionProvider}.
 *
 * @author Henning Treu - Initial contribution
 */
@NonNullByDefault
public interface MagicService extends ConfigOptionProvider {

    static final URI CONFIG_URI = URI.create("test:magic");

}
