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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.media.MediaService;
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

    public List<String> playList = new ArrayList<String>();

    @Activate
    public MediaServiceImpl() {
    }

    @Override
    public void registerPlayList(String name) {
        playList.add(name);
    }

    @Override
    public List<String> getPlayList() {
        return playList;
    }

}
