/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.voice;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A tagging interface for dialog trigger events.
 *
 * This interface is intended to represent events that can trigger a dialog,
 * without being specific to any particular type of event such as keyword spotting.
 * 
 * @author Miguel Álvarez Díez - Initial contribution
 */
@NonNullByDefault
public interface DTEvent {
}
