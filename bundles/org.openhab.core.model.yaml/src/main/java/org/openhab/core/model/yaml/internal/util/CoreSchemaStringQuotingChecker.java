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

import java.io.Serial;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.snakeyaml.engine.v2.nodes.Tag;
import org.snakeyaml.engine.v2.resolver.ScalarResolver;
import org.snakeyaml.engine.v2.schema.CoreSchema;

import tools.jackson.dataformat.yaml.util.StringQuotingChecker;

/**
 * A {@link StringQuotingChecker} that also quotes String values which the YAML 1.2 core schema would read as
 * another type.
 * <p>
 * The default checker decides on number-like values with JSON schema patterns, so with
 * {@code MINIMIZE_QUOTES} Strings like {@code 0x1F}, {@code 0o17} or {@code .NaN} are written without quotes and
 * read back as numbers by a mapper using the core schema.
 *
 * @author Holger Friedrich - Initial contribution
 */
@NonNullByDefault
public class CoreSchemaStringQuotingChecker extends StringQuotingChecker.Default {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final ScalarResolver CORE_SCHEMA_RESOLVER = new CoreSchema().getScalarResolver();

    @Override
    public boolean needToQuoteValue(@NonNullByDefault({}) String value) {
        // blank values are always quoted by the emitter itself, so keep its choice of quote style
        return super.needToQuoteValue(value)
                || (!value.isBlank() && !Tag.STR.equals(CORE_SCHEMA_RESOLVER.resolve(value, true)));
    }
}
