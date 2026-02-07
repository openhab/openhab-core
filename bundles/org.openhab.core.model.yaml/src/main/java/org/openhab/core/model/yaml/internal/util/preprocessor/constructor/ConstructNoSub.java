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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.nodes.Node;

/**
 * The {@link ConstructNoSub} is a constructor for the <code>!nosub</code> tag.
 *
 * It just needs to resolve our custom tag and construct the underlying node
 * as usual.
 *
 * We don't need a special placeholder since the tracking of substitution state
 * is done in {@link ModelConstructor#constructObject(Node)}.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
class ConstructNoSub extends AbstractConstruct {
    protected final ModelConstructor constructor;

    ConstructNoSub(ModelConstructor constructor) {
        this.constructor = constructor;
    }

    @Override
    @NonNullByDefault({})
    public @Nullable Object construct(Node node) {
        return constructor.constructByType(node);
    }
}
