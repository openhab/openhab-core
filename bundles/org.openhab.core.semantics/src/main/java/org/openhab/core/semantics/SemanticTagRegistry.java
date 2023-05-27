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
package org.openhab.core.semantics;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Registry;

/**
 * {@link SemanticTagRegistry} tracks all {@link SemanticTag}s from different {@link SemanticTagProvider}s
 * and provides access to them.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public interface SemanticTagRegistry extends Registry<SemanticTag, String> {

    /**
     * Returns the provided tag + all tags having the provided tag as ancestor.
     *
     * @param tag a tag in the registry
     * @return a list of all tags having the provided tag as ancestor, including the provided tag itself
     */
    public List<SemanticTag> getSubTree(SemanticTag tag);
}
