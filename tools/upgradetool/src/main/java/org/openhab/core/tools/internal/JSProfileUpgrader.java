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
package org.openhab.core.tools.internal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.storage.json.internal.JsonStorage;
import org.openhab.core.thing.internal.link.ItemChannelLinkConfigDescriptionProvider;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.tools.Upgrader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link JSProfileUpgrader} upgrades JS Profile configurations
 *
 * @Since 4.0.0
 *
 * @author Jan N. Klug - Initial contribution
 * @author Jimmy Tanagra - Refactored into a separate class
 */
@NonNullByDefault
public class JSProfileUpgrader implements Upgrader {
    private final Logger logger = LoggerFactory.getLogger(JSProfileUpgrader.class);

    @Override
    public String getName() {
        return "linkUpgradeJSProfile"; // keep the old name for backwards compatibility
    }

    @Override
    public String getDescription() {
        return "Upgrade JS profile configuration to new format";
    }

    @Override
    public boolean execute(@Nullable Path userdataPath, @Nullable Path confPath) {
        if (userdataPath == null) {
            logger.error("{} skipped: no userdata directory found.", getName());
            return false;
        }

        Path dataPath = userdataPath.resolve("jsondb");

        Path linkJsonDatabasePath = dataPath.resolve("org.openhab.core.thing.link.ItemChannelLink.json");
        logger.info("Upgrading JS profile configuration in database '{}'", linkJsonDatabasePath);

        if (!Files.isWritable(linkJsonDatabasePath)) {
            logger.error("Cannot access link database '{}', check path and access rights.", linkJsonDatabasePath);
            return false;
        }
        JsonStorage<ItemChannelLink> linkStorage = new JsonStorage<>(linkJsonDatabasePath.toFile(), null, 5, 0, 0,
                List.of());

        List.copyOf(linkStorage.getKeys()).forEach(linkUid -> {
            ItemChannelLink link = Objects.requireNonNull(linkStorage.get(linkUid));
            Configuration configuration = link.getConfiguration();
            String profileName = (String) configuration.get(ItemChannelLinkConfigDescriptionProvider.PARAM_PROFILE);
            if ("transform:JS".equals(profileName)) {
                String function = (String) configuration.get("function");
                if (function != null) {
                    configuration.put("toItemScript", function);
                    configuration.put("toHandlerScript", "|input");
                    configuration.remove("function");
                    configuration.remove("sourceFormat");

                    linkStorage.put(linkUid, link);
                    logger.info("{}: rewrote JS profile link to new format", linkUid);
                } else {
                    logger.info("{}: link already has correct configuration", linkUid);
                }
            }
        });

        linkStorage.flush();
        return true;
    }
}
