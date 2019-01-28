/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.model.script.internal;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.parser.IEncodingProvider;

/**
 * {@link IEncodingProvider} implementation for scripts.
 * <p>
 * It makes sure that synthetic resources are interpreted as UTF-8 because they will be handed in as strings and turned
 * into UTF-8 encoded streams by the script engine.
 *
 * @author Simon Kaufmann - initial contribution and API
 */
public class ScriptEncodingProvider implements IEncodingProvider {

    @Override
    public String getEncoding(URI uri) {
        if (uri.toString().startsWith("__synthetic")) {
            return StandardCharsets.UTF_8.name();
        }
        return Charset.defaultCharset().name();
    }

}
