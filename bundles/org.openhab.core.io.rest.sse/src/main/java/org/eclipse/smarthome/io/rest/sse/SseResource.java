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
package org.eclipse.smarthome.io.rest.sse;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent.Builder;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.auth.Role;
import org.eclipse.smarthome.core.events.Event;
import org.eclipse.smarthome.core.events.EventFilter;
import org.eclipse.smarthome.core.events.EventSubscriber;
import org.eclipse.smarthome.io.rest.sse.internal.CustomSseEventSink;
import org.osgi.service.component.annotations.Component;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * SSE Resource for pushing events to currently listening clients.
 *
 * @author Ivan Iliev - Initial Contribution and API
 * @author Yordan Zhelev - Added Swagger annotations
 * @author David Graeff - Migrate from Glassfish to official JAX RS SSE Api
 */
@Component(immediate = true, service = SseResource.class)
@Path(SseResource.PATH_EVENTS)
@RolesAllowed({ Role.USER })
@Singleton
@Produces(MediaType.SERVER_SENT_EVENTS)
@Api(value = SseResource.PATH_EVENTS, hidden = true)
@ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 400, message = "Topic is empty or contains invalid characters") })
@NonNullByDefault
public class SseResource implements EventSubscriber {
    public static final String PATH_EVENTS = "events";
    private static final String TOPIC_VALIDATE_PATTERN = "(\\w*\\*?\\/?,?\\s*)*";
    private static final String X_ACCEL_BUFFERING_HEADER = "X-Accel-Buffering";

    @Context
    private @NonNullByDefault({}) HttpServletResponse response;

    private @NonNullByDefault({}) Builder eventBuilder;
    private @NonNullByDefault({}) SseBroadcaster sseBroadcaster;

    public SseResource() {
    }

    @Context
    public void setSse(Sse sse) {
        this.eventBuilder = sse.newEventBuilder();
        this.sseBroadcaster = sse.newBroadcaster();
    }

    private final Set<String> subscribedEventTypes = Collections.singleton(EventSubscriber.ALL_EVENT_TYPES);

    @Override
    public Set<String> getSubscribedEventTypes() {
        return subscribedEventTypes;
    }

    @Nullable
    @Override
    public EventFilter getEventFilter() {
        return null;
    }

    @Override
    public void receive(Event event) {
        sseBroadcaster
                .broadcast(eventBuilder.name("message").mediaType(MediaType.APPLICATION_JSON_TYPE).data(event).build());
    }

    /**
     * Subscribes the connecting client to the stream of events filtered by the
     * given eventFilter.
     *
     * @param eventFilter A filter string. Multiple filters possible, separated by commas
     */
    @GET
    public void listen(@Context SseEventSink sseEventSink,
            @QueryParam("topics") @ApiParam(value = "topics") String eventFilter)
            throws IOException, InterruptedException {
        if (!StringUtils.isEmpty(eventFilter) && eventFilter.matches(TOPIC_VALIDATE_PATTERN)) {
            sseEventSink.close();
            throw new BadRequestException("Topic invalid");
        }

        // construct an EventOutput that will only write out events that match
        // the given filter
        this.sseBroadcaster.register(new CustomSseEventSink(sseEventSink, eventFilter));

        // Disables proxy buffering when using an nginx http server proxy for this response.
        // This allows you to not disable proxy buffering in nginx and still have working sse
        response.addHeader(X_ACCEL_BUFFERING_HEADER, "no");

        // we want to make sure
        // that the response is not compressed and buffered so that the
        // client receives server sent events at the moment of sending them
        response.addHeader(HttpHeaders.CONTENT_ENCODING, "identity");

        // Response headers are written now, since the thread will be
        // blocked later on.
        response.setStatus(HttpServletResponse.SC_OK);
        response.flushBuffer();
    }
}
