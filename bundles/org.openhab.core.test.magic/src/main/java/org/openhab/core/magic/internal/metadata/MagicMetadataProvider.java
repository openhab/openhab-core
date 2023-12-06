/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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

import static org.openhab.core.config.core.ConfigDescriptionParameterBuilder.create;

import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.config.core.metadata.MetadataConfigDescriptionProvider;
import org.osgi.service.component.annotations.Component;

/**
 * Describes the metadata for the "magic" namespace.
 *
 * @author Simon Kaufmann - Initial contribution
 */
@Component
@NonNullByDefault
public class MagicMetadataProvider implements MetadataConfigDescriptionProvider {

    @Override
    public String getNamespace() {
        return "magic";
    }

    @Override
    public @Nullable String getDescription(@Nullable Locale locale) {
        return "Make items magic";
    }

    @Override
    public @Nullable List<ParameterOption> getParameterOptions(@Nullable Locale locale) {
        return List.of( //
                new ParameterOption("just", "Just Magic"), //
                new ParameterOption("pure", "Pure Magic") //
        );
    }

    @Override
    public @Nullable List<ConfigDescriptionParameter> getParameters(String value, @Nullable Locale locale) {
        switch (value) {
            case "just":
                return List.of( //
                        create("electric", Type.BOOLEAN).withLabel("Use Electricity").build() //
                );
            case "pure":
                return List.of( //
                        create("spell", Type.TEXT).withLabel("Spell").withDescription("The exact spell to use").build(), //
                        create("price", Type.DECIMAL).withLabel("Price")
                                .withDescription("...because magic always comes with a price").build(), //
                        create("power", Type.INTEGER).withLabel("Power").withLimitToOptions(true).withOptions( //
                                List.of( //
                                        new ParameterOption("0", "Very High"), //
                                        new ParameterOption("1", "Incredible"), //
                                        new ParameterOption("2", "Insane"), //
                                        new ParameterOption("3", "Ludicrous") //
                                )).build() //
                );
        }
        return null;
    }
}
