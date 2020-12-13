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
package org.openhab.core.audio.internal.webaudio;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.AbstractEventFactory;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFactory;
import org.osgi.service.component.annotations.Component;

/**
 * This is an {@link EventFactory} for creating web audio events.
 * The only currently supported event type is {@link PlayURLEvent}.
 *
 * @author Kai Kreuzer - Initial contribution and API
 */
@NonNullByDefault
@Component(service = EventFactory.class, immediate = true)
public class WebAudioEventFactory extends AbstractEventFactory {

    private static final String PLAY_URL_TOPIC = "openhab/webaudio/playurl";

    /**
     * Constructs a new WebAudioEventFactory.
     */
    public WebAudioEventFactory() {
        super(Set.of(PlayURLEvent.TYPE));
    }

    @Override
    protected Event createEventByType(String eventType, String topic, String payload, @Nullable String source)
            throws Exception {
        if (PlayURLEvent.TYPE.equals(eventType)) {
            String url = deserializePayload(payload, String.class);
            return createPlayURLEvent(url);
        }
        throw new IllegalArgumentException("The event type '" + eventType + "' is not supported by this factory.");
    }

    /**
     * Creates a PlayURLEvent event.
     *
     * @param url the url to play
     * @return the according event
     */
    public static PlayURLEvent createPlayURLEvent(String url) {
        String payload = serializePayload(url);
        return new PlayURLEvent(PLAY_URL_TOPIC, payload, url);
    }
}
