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
package org.openhab.core.model.yaml.internal.util;

import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.nodes.Tag;
import org.snakeyaml.engine.v2.resolver.ScalarResolver;
import org.snakeyaml.engine.v2.schema.CoreSchema;

/**
 * The YAML 1.2 core schema, but plain scalars with a leading zero and a digit 8 or 9 (e.g. {@code 09} or
 * {@code 09123}) are read as Strings.
 * <p>
 * The core schema resolves them as integers, which Jackson then fails to parse as octal numbers. YAML 1.1, as used
 * with Jackson 2, read them as Strings.
 *
 * @author Holger Friedrich - Initial contribution
 */
@NonNullByDefault
public class LeadingZeroStringCoreSchema extends CoreSchema {

    private static final Pattern LEADING_ZERO_NON_OCTAL = Pattern.compile("[-+]?0[0-9]*[89][0-9]*");

    /**
     * Creates {@link LoadSettings} using this schema, for {@code YAMLFactory.builder().loadSettings(...)}.
     *
     * @return the load settings
     */
    public static LoadSettings loadSettings() {
        return LoadSettings.builder().setSchema(new LeadingZeroStringCoreSchema()).build();
    }

    @Override
    public ScalarResolver getScalarResolver() {
        ScalarResolver coreResolver = super.getScalarResolver();
        return (value, implicit) -> Boolean.TRUE.equals(implicit) && LEADING_ZERO_NON_OCTAL.matcher(value).matches()
                ? Tag.STR
                : coreResolver.resolve(value, implicit);
    }
}
