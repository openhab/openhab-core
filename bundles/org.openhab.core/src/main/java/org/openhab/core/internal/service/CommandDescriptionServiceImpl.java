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
package org.openhab.core.internal.service;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.service.CommandDescriptionService;
import org.openhab.core.types.CommandDescription;
import org.openhab.core.types.CommandDescriptionProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * The {@link CommandDescriptionService} combines all available {@link CommandDescriptionProvider} implementations to
 * build a resulting {@link CommandDescription}.
 *
 * @author Henning Treu - Initial contribution
 */
@NonNullByDefault
@Component
public class CommandDescriptionServiceImpl implements CommandDescriptionService {

    private final List<CommandDescriptionProvider> commandDescriptionProviders = new CopyOnWriteArrayList<>();

    @Override
    public @Nullable CommandDescription getCommandDescription(String itemName, @Nullable Locale locale) {
        /*
         * As of now there is only the ChannelCommandDescriptionProvider, so there is no merge logic as for
         * {@link StateDescriptionFragment}s. Just take the first CommandDescription which was provided.
         */
        for (CommandDescriptionProvider cdp : commandDescriptionProviders) {
            CommandDescription commandDescription = cdp.getCommandDescription(itemName, locale);
            if (commandDescription != null) {
                return commandDescription;
            }
        }

        return null;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addCommandDescriptionProvider(CommandDescriptionProvider commandDescriptionProvider) {
        commandDescriptionProviders.add(commandDescriptionProvider);
    }

    protected void removeCommandDescriptionProvider(CommandDescriptionProvider commandDescriptionProvider) {
        commandDescriptionProviders.remove(commandDescriptionProvider);
    }

}
