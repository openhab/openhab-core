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
package org.openhab.core.media.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.media.MediaService;
import org.openhab.core.media.model.MediaEntry;
import org.openhab.core.media.model.MediaRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * Implementation of
 *
 * @author Laurent Arnal - Initial contribution
 *
 */
@NonNullByDefault
@Component(immediate = true)
public class MediaServiceImpl implements MediaService {

    public MediaRegistry registry = new MediaRegistry();

    @Activate
    public MediaServiceImpl() {
    }

    @Override
    public void registerMediaEntry(MediaEntry mediaEntry) {
        registry.addChild(mediaEntry.getName(), mediaEntry);
    }

    @Override
    public MediaRegistry getMediaRegistry() {
        return registry;
    }

}
