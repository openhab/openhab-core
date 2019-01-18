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
package org.eclipse.smarthome.magic.binding.handler;

import org.eclipse.smarthome.core.library.types.RawType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.io.net.http.HttpUtil;

/**
 * The {@link MagicImageHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Henning Treu - Initial contribution
 */
public class MagicImageHandler extends BaseThingHandler {

    private String url;

    public MagicImageHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command == RefreshType.REFRESH) {
            RawType image = HttpUtil.downloadImage(url, 30000);
            if (image != null) {
                updateState(channelUID, image);
            }
        }
    }

    @Override
    public void initialize() {
        this.url = (String) getConfig().get("url");
        if (url == null || url.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "The URL must not be blank");
        }

        updateStatus(ThingStatus.ONLINE);
    }
}
