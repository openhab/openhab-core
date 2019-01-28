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
package org.eclipse.smarthome.core.audio;

import java.io.InputStream;

/**
 * Wrapper for a source of audio data.
 *
 * In contrast to {@link AudioSource}, this is often a "one time use" instance for passing some audio data,
 * but it is not meant to be registered as a service.
 *
 * The stream needs to be closed by the client that uses it.
 *
 * @author Harald Kuhn - Initial API
 * @author Kelly Davis - Modified to match discussion in #584
 * @author Kai Kreuzer - Refactored to be only a temporary instance for the stream
 */
public abstract class AudioStream extends InputStream {

    /**
     * Gets the supported audio format
     *
     * @return The supported audio format
     */
    public abstract AudioFormat getFormat();

}
