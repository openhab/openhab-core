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
package org.openhab.core.voice.internal;

import java.util.Locale;

import org.openhab.core.voice.Voice;

/**
 * A {@link Voice} stub used for the tests.
 *
 * @author Mihaela Memova - Initial contribution
 * @author Velin Yordanov - migrated from groovy to java
 */
public class VoiceStub implements Voice {

    private TTSServiceStub ttsService = new TTSServiceStub();

    private static final String VOICE_STUB_ID = "voiceStubID";
    private static final String VOICE_STUB_LABEL = "voiceStubLabel";

    @Override
    public String getUID() {
        return ttsService.getId() + ":" + VOICE_STUB_ID;
    }

    @Override
    public String getLabel() {
        return VOICE_STUB_LABEL;
    }

    @Override
    public Locale getLocale() {
        // we need to return something different from null here (the real value is not important)
        return Locale.getDefault();
    }
}
