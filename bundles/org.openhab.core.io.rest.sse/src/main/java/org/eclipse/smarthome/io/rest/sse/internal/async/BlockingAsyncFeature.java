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
package org.eclipse.smarthome.io.rest.sse.internal.async;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

/**
 * A {@link Feature} implementation that registers our custom {@link BlockingAsyncBinder}.
 *
 * @author Ivan Iliev - Initial Contribution and API
 *
 */
public class BlockingAsyncFeature implements Feature {

    @Override
    public boolean configure(FeatureContext context) {
        if (context.getConfiguration().isEnabled(BlockingAsyncFeature.class)) {
            return false;
        }

        context.register(new BlockingAsyncBinder());

        return true;
    }
}
