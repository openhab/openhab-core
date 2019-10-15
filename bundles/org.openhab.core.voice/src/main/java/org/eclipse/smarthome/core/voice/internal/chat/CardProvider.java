/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.core.voice.internal.chat;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.common.registry.DefaultAbstractManagedProvider;
import org.eclipse.smarthome.core.common.registry.ManagedProvider;
import org.eclipse.smarthome.core.storage.StorageService;
import org.eclipse.smarthome.core.voice.chat.Card;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The @link {@link ManagedProvider} for {@link Card} elements
 *
 * @author Yannick Schaus - Initial contribution
 * @author Laurent Garnier - Moved from HABot + storage name updated
 */
@Component(service = CardProvider.class, immediate = true)
public class CardProvider extends DefaultAbstractManagedProvider<Card, String> {

    @Activate
    public CardProvider(final @Reference StorageService storageService) {
        super(storageService);
    }

    @Override
    protected String getStorageName() {
        return "cards";
    }

    @Override
    protected @NonNull String keyToString(@NonNull String key) {
        return key;
    }

}
