/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.io.rest.sse.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseEventSink;

import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.core.events.Event;

/**
 * {@link SseEventSink} implementation that takes a filter parameter and only writes out events that match this filter.
 * Should only be used when the {@link OutboundSseEvent}s sent through this {@link SseEventSink} contain a data object
 * of
 * type {@link EventBean}
 *
 * @author Ivan Iliev - Initial contribution and API
 *
 */
public class CustomSseEventSink implements SseEventSink {

    /**
     * Splits the given topicFilter at any commas (",") and for each token replaces any wildcards(*) with the regex
     * pattern (.*)
     *
     * @param topicFilter
     * @return
     */
    public static List<String> convertToRegex(String topicFilter) {
        List<String> filters = new ArrayList<String>();

        if (StringUtils.isEmpty(topicFilter)) {
            filters.add(".*");
        } else {
            StringTokenizer tokenizer = new StringTokenizer(topicFilter, ",");
            while (tokenizer.hasMoreElements()) {
                String regex = tokenizer.nextToken().trim().replace("*", ".*") + ".*";
                filters.add(regex);
            }
        }

        return filters;
    }

    private List<String> regexFilters;
    private SseEventSink proxy;

    public CustomSseEventSink(SseEventSink sseEventSink, String topicFilter) {
        this.proxy = sseEventSink;
        this.regexFilters = convertToRegex(topicFilter);
    }

    @Override
    public boolean isClosed() {
        return proxy.isClosed();
    }

    @Override
    public CompletionStage<?> send(OutboundSseEvent event) {
        Event eventBean = (Event) event.getData();

        for (String filter : regexFilters) {
            if (eventBean.getTopic().matches(filter)) {
                return proxy.send(event);
            }
        }
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public void close() {
        proxy.close();
    }

}
