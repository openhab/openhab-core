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
package org.openhab.core.magic.internal.metadata;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.items.MetadataPredicates;
import org.openhab.core.items.MetadataRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example service which makes use of the metadata of the "magic" namespace.
 *
 * @author Simon Kaufmann - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true)
public class MagicMetadataUsingService {

    private final Logger logger = LoggerFactory.getLogger(MagicMetadataUsingService.class);
    private final ScheduledExecutorService scheduler = ThreadPoolManager.getScheduledPool("magic");

    private final MetadataRegistry metadataRegistry;

    private @Nullable ScheduledFuture<?> job;

    @Activate
    public MagicMetadataUsingService(final @Reference MetadataRegistry metadataRegistry) {
        this.metadataRegistry = metadataRegistry;
        job = scheduler.scheduleWithFixedDelay(() -> run(), 30, 30, TimeUnit.SECONDS);
    }

    @Deactivate
    public void deactivate() {
        if (job != null) {
            job.cancel(false);
            job = null;
        }
    }

    private void run() {
        metadataRegistry.stream().filter(MetadataPredicates.hasNamespace("magic")).forEach(metadata -> {
            logger.info("Item {} is {} with {}", metadata.getUID().getItemName(), metadata.getValue(),
                    metadata.getConfiguration());
        });
    }
}
