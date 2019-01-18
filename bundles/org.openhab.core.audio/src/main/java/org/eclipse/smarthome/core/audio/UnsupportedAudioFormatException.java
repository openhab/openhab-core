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

/**
 * Thrown when a requested format is not supported by an {@link AudioSource}
 * or {@link AudioSink} implementation
 * 
 * @author Harald Kuhn - Initial API
 * @author Kelly Davis - Modified to match discussion in #584
 */
public class UnsupportedAudioFormatException extends AudioException {

    private static final long serialVersionUID = 1L;

   /**
    * Unsupported {@link AudioFormat}
    */
    private AudioFormat unsupportedFormat;

   /**
    * Constructs a new exception with the specified detail message, unsupported format, and cause.
    *
    * @param message Detail message
    * @param unsupportedFormat Unsupported format
    * @param cause The cause 
    */
    public UnsupportedAudioFormatException(String message, AudioFormat unsupportedFormat, Throwable cause) {
        super(message, cause);
        this.unsupportedFormat = unsupportedFormat;
    }

   /**
    * Constructs a new exception with the specified detail message and unsupported format.
    *
    * @param message Detail message
    * @param unsupportedFormat Unsupported format
    */
    public UnsupportedAudioFormatException(String message, AudioFormat unsupportedFormat) {
        this(message, unsupportedFormat, null);
    }

   /**
    * Gets the unsupported format
    *
    * @return The unsupported format
    */
    public AudioFormat getUnsupportedFormat() {
        return unsupportedFormat;
    }
}
