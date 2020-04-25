/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.io.rest.sse.internal;

import java.io.IOException;
import java.util.List;

import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.openhab.core.io.rest.sse.beans.EventBean;
import org.openhab.core.io.rest.sse.internal.util.SseUtil;

/**
 * {@link EventOutput} implementation that takes a filter parameter and only writes out events that match this filter.
 * Should only be used when the {@link OutboundEvent}s sent through this {@link EventOutput} contain a data object of
 * type {@link EventBean}
 * 
 * @author Ivan Iliev - Initial contribution
 * 
 */
public class SseEventOutput extends EventOutput {

    private List<String> regexFilters;

    public SseEventOutput(String topicFilter) {
        super();
        this.regexFilters = SseUtil.convertToRegex(topicFilter);
    }

    @Override
    public void write(OutboundEvent chunk) throws IOException {
        EventBean event = (EventBean) chunk.getData();

        for (String filter : regexFilters) {
            if (event.topic.matches(filter)) {
                super.write(chunk);
                return;
            }
        }
    }
}
