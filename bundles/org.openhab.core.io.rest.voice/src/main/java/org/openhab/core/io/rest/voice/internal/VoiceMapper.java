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
 * Mapper class that maps {@link Voice} instanced to their respective DTOs.
 *
 * @author Laurent Garnier - Initial contribution
 */
public class VoiceMapper {

    /**
     * Maps a {@link Voice} to an {@link VoiceDTO}.
     *
     * @param voice the voice
     *
     * @return the corresponding DTO
     */
    public static VoiceDTO map(Voice voice) {
        VoiceDTO dto = new VoiceDTO();
        dto.id = voice.getUID();
        dto.label = voice.getLabel();
        dto.locale = voice.getLocale().toString();
        return dto;
    }

}
