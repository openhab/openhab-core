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
package org.openhab.core.internal.items;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.internal.types.CommandDescriptionImpl;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.types.CommandDescription;
import org.openhab.core.types.CommandDescriptionProvider;
import org.openhab.core.types.CommandOption;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link CommandDescription} provider from items' metadata
 *
 * @author Yannick Schaus - initial contribution
 *
 */
@NonNullByDefault
@Component(service = CommandDescriptionProvider.class)
public class MetadataCommandDescriptionProvider implements CommandDescriptionProvider {

    private final Logger logger = LoggerFactory.getLogger(MetadataCommandDescriptionProvider.class);

    public static final String COMMANDDESCRIPTION_METADATA_NAMESPACE = "commandDescription";

    private MetadataRegistry metadataRegistry;

    @Activate
    public MetadataCommandDescriptionProvider(final @Reference MetadataRegistry metadataRegistry,
            Map<String, Object> properties) {
        this.metadataRegistry = metadataRegistry;
    }

    @Override
    public @Nullable CommandDescription getCommandDescription(String itemName, @Nullable Locale locale) {
        Metadata metadata = metadataRegistry.get(new MetadataKey(COMMANDDESCRIPTION_METADATA_NAMESPACE, itemName));

        if (metadata != null) {
            try {
                CommandDescriptionImpl commandDescription = new CommandDescriptionImpl();
                if (metadata.getConfiguration().containsKey("options")) {
                    Stream.of(metadata.getConfiguration().get("options").toString().split(",")).forEach(o -> {
                        if (o.contains("=")) {
                            commandDescription.addCommandOption(
                                    new CommandOption(o.split("=")[0].trim(), o.split("=")[1].trim()));
                        } else {
                            commandDescription.addCommandOption(new CommandOption(o.trim(), null));
                        }
                    });

                    return commandDescription;
                }
            } catch (Exception e) {
                logger.warn("Unable to parse the commandDescription from metadata for item {}, ignoring it", itemName);
                return null;
            }
        }

        return null;
    }
}
