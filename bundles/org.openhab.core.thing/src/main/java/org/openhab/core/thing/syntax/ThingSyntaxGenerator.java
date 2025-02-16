/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.thing.syntax;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.Thing;

/**
 * {@link ThingSyntaxGenerator} is the interface to implement by any syntax generator for {@link Thing} object.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public interface ThingSyntaxGenerator {

    /**
     * Returns the format of the syntax.
     *
     * @return the syntax format
     */
    String getGeneratorFormat();

    /**
     * Generate the syntax for a sorted list of things.
     *
     * @param things the things
     * @param hideDefaultParameters true to hide the configuration parameters having the default value
     * @return the syntax for the things
     */
    String generateSyntax(List<Thing> things, boolean hideDefaultParameters);
}
