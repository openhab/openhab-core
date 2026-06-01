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
package org.openhab.core.voice.text;

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This service provides functionality around voice services and is the central service to be used directly by others.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
@NonNullByDefault
public record InterpretationArguments(String hliIdList, String conversationId, String toolIdList,
        @Nullable String locationItem, @Nullable Locale locale) {
}
