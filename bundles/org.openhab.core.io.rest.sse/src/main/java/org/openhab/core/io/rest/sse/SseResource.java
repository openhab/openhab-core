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
package org.openhab.core.io.rest.sse;

import static org.openhab.core.io.rest.sse.internal.SseSinkItemInfo.*;
import static org.openhab.core.io.rest.sse.internal.SseSinkTopicInfo.matchesTopic;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.security.RolesAllowed;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.auth.Role;
import org.openhab.core.events.Event;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.SseBroadcaster;
import org.openhab.core.io.rest.sse.internal.SseItemStatesEventBuilder;
import org.openhab.core.io.rest.sse.internal.SsePublisher;
import org.openhab.core.io.rest.sse.internal.SseSinkItemInfo;
import org.openhab.core.io.rest.sse.internal.SseSinkTopicInfo;
import org.openhab.core.io.rest.sse.internal.dto.EventDTO;
import org.openhab.core.io.rest.sse.internal.util.SseUtil;
import org.openhab.core.items.events.ItemStateChangedEvent;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JSONRequired;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * SSE Resource for pushing events to currently listening clients.
 *
 * @author Ivan Iliev - Initial contribution
 * @author Yordan Zhelev - Added Swagger annotations
 * @author Yannick Schaus - Add endpoints to track item state updates
 * @author Markus Rathgeb - Drop Glassfish dependency and use API only
 * @author Wouter Born - Rework SSE item state sinks for dropping Glassfish
 */
@Component(service = SsePublisher.class)
@JaxrsResource
@JaxrsName(SseResource.PATH_EVENTS)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(SseResource.PATH_EVENTS)
@RolesAllowed({ Role.USER })
@Singleton
@NonNullByDefault
public class SseResource implements SsePublisher {

    // The URI path to this resource
    public static final String PATH_EVENTS = "events";

    private static final String X_ACCEL_BUFFERING_HEADER = "X-Accel-Buffering";

    private final Logger logger = LoggerFactory.getLogger(SseResource.class);

    private @Context @NonNullByDefault({}) Sse sse;

    private final SseBroadcaster<SseSinkItemInfo> itemStatesBroadcaster = new SseBroadcaster<>();
    private final SseItemStatesEventBuilder itemStatesEventBuilder;
    private final SseBroadcaster<SseSinkTopicInfo> topicBroadcaster = new SseBroadcaster<>();

    private ExecutorService executorService;

    @Activate
    public SseResource(@Reference SseItemStatesEventBuilder itemStatesEventBuilder) {
        this.executorService = Executors.newSingleThreadExecutor();
        this.itemStatesEventBuilder = itemStatesEventBuilder;
    }

    @Deactivate
    public void deactivate() {
        itemStatesBroadcaster.close();
        topicBroadcaster.close();
        executorService.shutdown();
    }

    @Override
    public void broadcast(Event event) {
        if (sse == null) {
            logger.trace("broadcast skipped (no one listened since activation)");
            return;
        }

        executorService.execute(() -> {
            handleEventBroadcastTopic(event);
            if (event instanceof ItemStateChangedEvent) {
                handleEventBroadcastItemState((ItemStateChangedEvent) event);
            }
        });
    }

    private void addCommonResponseHeaders(final HttpServletResponse response) {
        // Disables proxy buffering when using an nginx http server proxy for this response.
        // This allows you to not disable proxy buffering in nginx and still have working sse
        response.addHeader(X_ACCEL_BUFFERING_HEADER, "no");

        // We want to make sure that the response is not compressed and buffered so that the client receives server sent
        // events at the moment of sending them.
        response.addHeader(HttpHeaders.CONTENT_ENCODING, "identity");

        try {
            response.flushBuffer();
        } catch (final IOException ex) {
            logger.trace("flush buffer failed", ex);
        }
    }

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @ApiOperation(value = "Get all events.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 400, message = "Topic is empty or contains invalid characters") })
    public void listen(@Context final SseEventSink sseEventSink, @Context final HttpServletResponse response,
            @QueryParam("topics") @ApiParam(value = "topics") String eventFilter) {
        if (!SseUtil.isValidTopicFilter(eventFilter)) {
            response.setStatus(Status.BAD_REQUEST.getStatusCode());
            return;
        }

        topicBroadcaster.add(sseEventSink, new SseSinkTopicInfo(eventFilter));

        addCommonResponseHeaders(response);
    }

    private void handleEventBroadcastTopic(Event event) {
        final EventDTO eventDTO = SseUtil.buildDTO(event);
        final OutboundSseEvent sseEvent = SseUtil.buildEvent(sse.newEventBuilder(), eventDTO);

        topicBroadcaster.sendIf(sseEvent, matchesTopic(eventDTO.topic));
    }

    /**
     * Subscribes the connecting client for state updates. It will initially only send a "ready" event with an unique
     * connectionId that the client can use to dynamically alter the list of tracked items.
     *
     * @return {@link EventOutput} object associated with the incoming connection.
     */
    @GET
    @Path("/states")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @ApiOperation(value = "Initiates a new item state tracker connection")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK") })
    public void getStateEvents(@Context final SseEventSink sseEventSink, @Context final HttpServletResponse response) {
        final SseSinkItemInfo sinkItemInfo = new SseSinkItemInfo();
        itemStatesBroadcaster.add(sseEventSink, sinkItemInfo);

        addCommonResponseHeaders(response);

        String connectionId = sinkItemInfo.getConnectionId();
        OutboundSseEvent readyEvent = sse.newEventBuilder().id("0").name("ready").data(connectionId).build();
        itemStatesBroadcaster.sendIf(readyEvent, hasConnectionId(connectionId));
    }

    /**
     * Alters the list of tracked items for a given state update connection
     *
     * @param connectionId the connection Id to change
     * @param itemNames the list of items to track
     */
    @POST
    @Path("/states/{connectionId}")
    @ApiOperation(value = "Changes the list of items a SSE connection will receive state updates to.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Unknown connectionId") })
    public Object updateTrackedItems(@PathParam("connectionId") String connectionId,
            @ApiParam("items") Set<String> itemNames) {
        Optional<SseSinkItemInfo> itemStateInfo = itemStatesBroadcaster.getInfoIf(hasConnectionId(connectionId))
                .findFirst();
        if (!itemStateInfo.isPresent()) {
            return Response.status(Status.NOT_FOUND).build();
        }

        itemStateInfo.get().updateTrackedItems(itemNames);

        OutboundSseEvent itemStateEvent = itemStatesEventBuilder.buildEvent(sse.newEventBuilder(), itemNames);
        if (itemStateEvent != null) {
            itemStatesBroadcaster.sendIf(itemStateEvent, hasConnectionId(connectionId));
        }

        return Response.ok().build();
    }

    /**
     * Broadcasts a state event to all currently listening clients, after transforming it to a simple map.
     *
     * @param stateChangeEvent the {@link ItemStateChangedEvent} containing the new state
     */
    public void handleEventBroadcastItemState(final ItemStateChangedEvent stateChangeEvent) {
        String itemName = stateChangeEvent.getItemName();
        boolean isTracked = itemStatesBroadcaster.getInfoIf(info -> true).anyMatch(tracksItem(itemName));
        if (isTracked) {
            OutboundSseEvent event = itemStatesEventBuilder.buildEvent(sse.newEventBuilder(), Set.of(itemName));
            if (event != null) {
                itemStatesBroadcaster.sendIf(event, tracksItem(itemName));
            }
        }
    }
}
