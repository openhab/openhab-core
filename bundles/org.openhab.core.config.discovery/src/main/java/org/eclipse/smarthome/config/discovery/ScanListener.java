/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.config.discovery;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link ScanListener} interface for receiving scan operation events.
 * <p>
 * A class that is interested in errors and termination of an active scan has to implement this interface.
 *
 * @author Kai Kreuzer - Initial Contribution.
 *
 * @see DiscoveryService
 */
@NonNullByDefault
public interface ScanListener {

    /**
     * Invoked synchronously when the according scan has finished.
     * <p>
     * This signal is sent latest when the defined timeout for the scan has been reached.
     */
    void onFinished();

    /**
     * Invoked synchronously when the according scan has caused an error or has been aborted.
     *
     * @param exception the error which occurred (could be null)
     */
    void onErrorOccurred(@Nullable Exception exception);

}
