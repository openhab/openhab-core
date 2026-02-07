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
package org.openhab.core.model.yaml.internal.util.preprocessor.constructor;

import java.util.function.BiFunction;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholder.InterpolablePlaceholder;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.nodes.Node;

/**
 * The {@link ConstructInterpolablePlaceholder} is a generic constructor used to create
 * placeholder objects for nodes whose value can be interpolated by {@link YamlPreprocessor}.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
class ConstructInterpolablePlaceholder<T extends InterpolablePlaceholder<T>> extends AbstractConstruct {
    private final BiFunction<@Nullable Object, String, T> creator;
    private final ModelConstructor constructor;

    ConstructInterpolablePlaceholder(ModelConstructor constructor, BiFunction<@Nullable Object, String, T> creator) {
        this.constructor = constructor;
        this.creator = creator;
    }

    @Override
    public @Nullable Object construct(@Nullable Node node) {
        if (node == null) {
            return null;
        }
        Object value = constructor.constructByType(node);
        return creator.apply(value, constructor.getLocation(node));
    }
}
