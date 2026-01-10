/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.audio;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * This is a {@link AudioStream}, which can also provide information about its absolute length and get cloned.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Gwendal Roulleau - Separate getClonedStream and length into their own interface.
 * @deprecated You should consider using {@link ClonableAudioStream} and/or {@link SizeableAudioStream} to detect audio
 *             stream capabilities
 */
@NonNullByDefault
@Deprecated
public abstract class FixedLengthAudioStream extends AudioStream implements SizeableAudioStream, ClonableAudioStream {

}
