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
package org.openhab.core.config.discovery;

/**
 * The {@link DiscoveryResultFlag} class specifies a list of flags
 * which a {@link DiscoveryResult} object can take.
 *
 * @author Michael Grammling - Initial contribution
 *
 * @see DiscoveryResult
 */
public enum DiscoveryResultFlag {

    /**
     * The flag {@code NEW} to signal that the result object should be regarded
     * as <i>new</i> by the system so that a further processing should be applied.
     */
    NEW,

    /**
     * The flag {@code IGNORED} to signal that the result object should be regarded
     * as <i>known</i> by the system so that a further processing should be skipped.
     */
    IGNORED;

}
