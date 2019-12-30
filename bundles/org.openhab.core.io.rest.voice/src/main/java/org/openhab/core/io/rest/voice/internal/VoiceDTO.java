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
package org.openhab.core.io.rest.voice.internal;

import org.openhab.core.voice.Voice;

/**
 * A DTO that is used on the REST API to provide infos about {@link Voice} to UIs.
 *
 * @author Laurent Garnier - Initial contribution
 */
public class VoiceDTO {
    public String id;
    public String label;
    public String locale;
}
