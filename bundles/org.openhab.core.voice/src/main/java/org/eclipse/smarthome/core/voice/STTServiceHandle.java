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
package org.eclipse.smarthome.core.voice;

/**
 * An handle to a {@link STTService}
 *
 * @author Kelly Davis - Initial contribution and API
 */
public interface STTServiceHandle {
   /**
    * Aborts recognition in the associated {@link STTService}
    */
    public void abort();
}
