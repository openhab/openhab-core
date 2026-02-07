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
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholder.SubstitutionPlaceholder;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.nodes.Node;

/**
 * The {@link ConstructStr} is the constructor used for STR tag which
 * may create a {@link SubstitutionPlaceholder}, depending on the current
 * substitution stack.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
class ConstructStr extends AbstractConstruct {
    private final ModelConstructor constructor;

    ConstructStr(ModelConstructor constructor) {
        this.constructor = constructor;
    }

    @Override
    @NonNullByDefault({})
    public Object construct(Node node) {
        return constructor.constructScalarOrSubstitution(node);
    }
}
