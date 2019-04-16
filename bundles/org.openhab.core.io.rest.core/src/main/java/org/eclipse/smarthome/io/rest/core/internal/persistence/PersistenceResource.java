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
package org.eclipse.smarthome.io.rest.core.internal.persistence;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.auth.Role;
import org.eclipse.smarthome.core.i18n.TimeZoneProvider;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemNotFoundException;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.persistence.FilterCriteria;
import org.eclipse.smarthome.core.persistence.FilterCriteria.Ordering;
import org.eclipse.smarthome.core.persistence.HistoricItem;
import org.eclipse.smarthome.core.persistence.ModifiablePersistenceService;
import org.eclipse.smarthome.core.persistence.PersistenceItemInfo;
import org.eclipse.smarthome.core.persistence.PersistenceService;
import org.eclipse.smarthome.core.persistence.PersistenceServiceRegistry;
import org.eclipse.smarthome.core.persistence.QueryablePersistenceService;
import org.eclipse.smarthome.core.persistence.dto.ItemHistoryDTO;
import org.eclipse.smarthome.core.persistence.dto.PersistenceServiceDTO;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.TypeParser;
import org.eclipse.smarthome.io.rest.LocaleService;
import org.eclipse.smarthome.io.rest.RESTService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * This class acts as a REST resource for history data and provides different methods to interact with the persistence
 * store
 *
 * @author Chris Jackson - Initial Contribution and add support for ModifiablePersistenceService
 * @author Kai Kreuzer - Refactored to use PersistenceServiceRegistryImpl
 * @author Franck Dechavanne - Added DTOs to ApiResponses
 * @author Erdoan Hadzhiyusein - Adapted the convertTime() method to work with the new DateTimeType
 * @author Lyubomir Papazov - Change java.util.Date references to be of type java.time.ZonedDateTime
 *
 */
@Path(PersistenceResource.PATH)
@Api(value = PersistenceResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
@Component(immediate = true)
@JaxrsApplicationSelect("(osgi.jaxrs.name=" + RESTService.REST_APP_NAME + ")")
@JaxrsResource
@NonNullByDefault
public class PersistenceResource {

    private final Logger logger = LoggerFactory.getLogger(PersistenceResource.class);

    private final String MODIFYABLE = "Modifiable";
    private final String QUERYABLE = "Queryable";
    private final String STANDARD = "Standard";

    // The URI path to this resource
    public static final String PATH = "persistence";

    @Reference
    private @NonNullByDefault({}) ItemRegistry itemRegistry;
    @Reference
    private @NonNullByDefault({}) PersistenceServiceRegistry persistenceServiceRegistry;
    @Reference
    private @NonNullByDefault({}) TimeZoneProvider timeZoneProvider;
    @Reference
    private @NonNullByDefault({}) LocaleService localeService;

    @GET
    @RolesAllowed({ Role.ADMIN })
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Gets a list of persistence services.", response = String.class, responseContainer = "List")
    @ApiResponses(value = @ApiResponse(code = 200, message = "OK", response = String.class, responseContainer = "List"))
    public Stream<?> httpGetPersistenceServices(@Context HttpHeaders headers,
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = HttpHeaders.ACCEPT_LANGUAGE) String language) {
        Locale locale = localeService.getLocale(language);

        return persistenceServiceRegistry.getAll().stream().map(service -> {
            PersistenceServiceDTO serviceDTO = new PersistenceServiceDTO();
            serviceDTO.id = service.getId();
            serviceDTO.label = service.getLabel(locale);

            if (service instanceof ModifiablePersistenceService) {
                serviceDTO.type = MODIFYABLE;
            } else if (service instanceof QueryablePersistenceService) {
                serviceDTO.type = QUERYABLE;
            } else {
                serviceDTO.type = STANDARD;
            }
            return serviceDTO;
        });
    }

    @GET
    @RolesAllowed({ Role.ADMIN })
    @Path("/items")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Gets a list of items available via a specific persistence service.", response = String.class, responseContainer = "List")
    @ApiResponses(value = @ApiResponse(code = 200, message = "OK", response = String.class, responseContainer = "List"))
    public Set<PersistenceItemInfo> httpGetPersistenceServiceItems(@Context HttpHeaders headers,
            @ApiParam(value = "Id of the persistence service. If not provided the default service will be used", required = false) @QueryParam("serviceId") @Nullable String serviceId) {
        // If serviceId is null, then use the default service
        final PersistenceService service;
        if (serviceId == null) {
            service = persistenceServiceRegistry.getDefault();
        } else {
            service = persistenceServiceRegistry.get(serviceId);
        }

        if (service == null) {
            throw new BadRequestException("Persistence service not found: " + serviceId);
        }

        if (!(service instanceof QueryablePersistenceService)) {
            throw new BadRequestException("Persistence service not queryable: " + serviceId);
        }

        QueryablePersistenceService qService = (QueryablePersistenceService) service;
        return qService.getItemInfo();
    }

    @GET
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Path("/items/{itemname: [a-zA-Z_0-9]*}")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Gets item persistence data from the persistence service.", response = ItemHistoryDTO.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = ItemHistoryDTO.class),
            @ApiResponse(code = 404, message = "Unknown Item or persistence service") })
    public ItemHistoryDTO httpGetPersistenceItemData(
            @ApiParam(value = "Id of the persistence service. If not provided the default service will be used", required = false) @QueryParam("serviceId") String serviceId,
            @ApiParam(value = "The item name", required = true) @PathParam("itemname") String itemName,
            @ApiParam(value = "Start time of the data to return. Will default to 1 day before endtime. ["
                    + DateTimeType.DATE_PATTERN_WITH_TZ_AND_MS
                    + "]", required = false) @QueryParam("starttime") String startTime,
            @ApiParam(value = "End time of the data to return. Will default to current time. ["
                    + DateTimeType.DATE_PATTERN_WITH_TZ_AND_MS
                    + "]", required = false) @QueryParam("endtime") String endTime,
            @ApiParam(value = "Page number of data to return. This parameter will enable paging.", required = false) @QueryParam("page") int pageNumber,
            @ApiParam(value = "The length of each page.", required = false) @QueryParam("pagelength") int pageLength,
            @ApiParam(value = "Gets one value before and after the requested period.", required = false) @QueryParam("boundary") boolean boundary) {

        // Benchmarking timer...
        long timerStart = System.currentTimeMillis();

        ItemHistoryDTO dto = createDTO(serviceId, itemName, startTime, endTime, pageNumber, pageLength, boundary);
        if (dto == null) {
            throw new BadRequestException("Persistence service not queryable: " + serviceId);
        }
        logger.debug("Persistence returned {} rows in {}ms", dto.datapoints, System.currentTimeMillis() - timerStart);
        return dto;
    }

    @DELETE
    @RolesAllowed({ Role.ADMIN })
    @Path("/items/{itemname: [a-zA-Z_0-9]*}")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Delete item data from a specific persistence service.", response = String.class, responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = String.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Invalid filter parameters"),
            @ApiResponse(code = 404, message = "Unknown persistence service") })
    public void httpDeletePersistenceServiceItem(@Context HttpHeaders headers,
            @ApiParam(value = "Id of the persistence service.", required = true) @QueryParam("serviceId") @Nullable String serviceId,
            @ApiParam(value = "The item name.", required = true) @PathParam("itemname") String itemName,
            @ApiParam(value = "Start time of the data to return. [" + DateTimeType.DATE_PATTERN_WITH_TZ_AND_MS
                    + "]", required = true) @QueryParam("starttime") @Nullable String startTime,
            @ApiParam(value = "End time of the data to return. [" + DateTimeType.DATE_PATTERN_WITH_TZ_AND_MS
                    + "]", required = true) @QueryParam("endtime") @Nullable String endTime) {

        // For deleting, we must specify a service id - don't use the default service
        if (serviceId == null || serviceId.length() == 0) {
            logger.debug("Persistence service must be specified for delete operations.");
            throw new BadRequestException("Persistence service must be specified for delete operations.");
        }

        PersistenceService service = persistenceServiceRegistry.get(serviceId);
        if (service == null) {
            throw new BadRequestException("Persistence service not found: " + serviceId);
        }

        if (!(service instanceof ModifiablePersistenceService)) {
            throw new BadRequestException("Persistence service not modifiable: " + serviceId);
        }

        ModifiablePersistenceService mService = (ModifiablePersistenceService) service;

        if (startTime == null || endTime == null) {
            throw new BadRequestException("The start and end time must be set");
        }

        ZonedDateTime dateTimeBegin = new DateTimeType(startTime).getZonedDateTime();
        ZonedDateTime dateTimeEnd = new DateTimeType(endTime).getZonedDateTime();
        if (dateTimeEnd.isBefore(dateTimeBegin)) {
            throw new BadRequestException("Start time must be earlier than end time");
        }

        FilterCriteria filter;

        // First, get the value at the start time.
        // This is necessary for values that don't change often otherwise data will start after the start of the graph
        // (or not at all if there's no change during the graph period)
        filter = new FilterCriteria();
        filter.setBeginDate(dateTimeBegin);
        filter.setEndDate(dateTimeEnd);
        filter.setItemName(itemName);
        try {
            mService.remove(filter);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid filter parameters.", e);
        }
    }

    @PUT
    @RolesAllowed({ Role.ADMIN })
    @Path("/items/{itemname: [a-zA-Z_0-9]*}")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Stores item persistence data into the persistence service.", response = ItemHistoryDTO.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = ItemHistoryDTO.class),
            @ApiResponse(code = 404, message = "Unknown Item or persistence service") })
    public void httpPutPersistenceItemData(@Context HttpHeaders headers,
            @ApiParam(value = "Id of the persistence service. If not provided the default service will be used", required = false) @QueryParam("serviceId") @Nullable String serviceId,
            @ApiParam(value = "The item name.", required = true) @PathParam("itemname") String itemName,
            @ApiParam(value = "Time of the data to be stored. Will default to current time. ["
                    + DateTimeType.DATE_PATTERN_WITH_TZ_AND_MS
                    + "]", required = true) @QueryParam("time") @Nullable String time,
            @ApiParam(value = "The state to store.", required = true) @QueryParam("state") String value) {
        // If serviceId is null, then use the default service
        PersistenceService service = null;
        String effectiveServiceId = serviceId != null ? serviceId : persistenceServiceRegistry.getDefaultId();
        service = persistenceServiceRegistry.get(effectiveServiceId);

        if (service == null) {
            throw new BadRequestException("Persistence service not found: " + effectiveServiceId);
        }

        Item item;
        try {
            item = itemRegistry.getItem(itemName);
        } catch (ItemNotFoundException e) {
            throw new BadRequestException("Item not found: " + itemName);
        }

        // Try to parse a State from the input
        State state = TypeParser.parseState(item.getAcceptedDataTypes(), value);
        if (state == null) {
            throw new BadRequestException("State could not be parsed: " + value);
        }

        ZonedDateTime dateTime = null;
        if (time != null && time.length() != 0) {
            dateTime = new DateTimeType(time).getZonedDateTime();
        }
        if (dateTime == null || dateTime.toEpochSecond() == 0) {
            throw new BadRequestException("Time badly formatted.");
        }

        if (!(service instanceof ModifiablePersistenceService)) {
            throw new BadRequestException("Persistence service not modifiable: " + effectiveServiceId);
        }

        ModifiablePersistenceService mService = (ModifiablePersistenceService) service;

        mService.store(item, Date.from(dateTime.toInstant()), state);
    }

    protected @Nullable ItemHistoryDTO createDTO(@Nullable String serviceId, String itemName,
            @Nullable String timeBegin, @Nullable String timeEnd, int pageNumber, int pageLength, boolean boundary) {
        // If serviceId is null, then use the default service
        PersistenceService service = null;
        String effectiveServiceId = serviceId != null ? serviceId : persistenceServiceRegistry.getDefaultId();
        service = persistenceServiceRegistry.get(effectiveServiceId);

        if (service == null) {
            logger.debug("Persistence service not found '{}'.", effectiveServiceId);
            return null;
        }

        if (!(service instanceof QueryablePersistenceService)) {
            logger.debug("Persistence service not queryable '{}'.", effectiveServiceId);
            return null;
        }

        QueryablePersistenceService qService = (QueryablePersistenceService) service;

        ZonedDateTime dateTimeBegin = ZonedDateTime.now();
        ZonedDateTime dateTimeEnd = dateTimeBegin;
        if (timeBegin != null) {
            dateTimeBegin = new DateTimeType(timeBegin).getZonedDateTime();
        }

        if (timeEnd != null) {
            dateTimeEnd = new DateTimeType(timeEnd).getZonedDateTime();
        }

        // End now...
        if (dateTimeEnd.toEpochSecond() == 0) {
            dateTimeEnd = ZonedDateTime.of(LocalDateTime.now(), timeZoneProvider.getTimeZone());
        }
        if (dateTimeBegin.toEpochSecond() == 0) {
            // Default to 1 days data if the times are the same or the start time is newer
            // than the end time
            dateTimeBegin = ZonedDateTime.of(dateTimeEnd.toLocalDateTime().plusDays(-1),
                    timeZoneProvider.getTimeZone());
        }

        // Default to 1 days data if the times are the same or the start time is newer
        // than the end time
        if (dateTimeBegin.isAfter(dateTimeEnd) || dateTimeBegin.isEqual(dateTimeEnd)) {
            dateTimeBegin = ZonedDateTime.of(dateTimeEnd.toLocalDateTime().plusDays(-1),
                    timeZoneProvider.getTimeZone());
        }

        FilterCriteria filter;
        Iterable<HistoricItem> result;
        State state = null;

        Long quantity = 0l;

        ItemHistoryDTO dto = new ItemHistoryDTO();
        dto.name = itemName;

        filter = new FilterCriteria();
        filter.setItemName(itemName);

        // If "boundary" is true then we want to get one value before and after the requested period
        // This is necessary for values that don't change often otherwise data will start after the start of the graph
        // (or not at all if there's no change during the graph period)
        if (boundary) {
            // Get the value before the start time.
            filter.setEndDate(dateTimeBegin);
            filter.setPageSize(1);
            filter.setOrdering(Ordering.DESCENDING);
            result = qService.query(filter);
            if (result.iterator().hasNext()) {
                dto.addData(dateTimeBegin.toInstant().toEpochMilli(), result.iterator().next().getState());
                quantity++;
            }
        }

        if (pageLength == 0) {
            filter.setPageNumber(0);
            filter.setPageSize(Integer.MAX_VALUE);
        } else {
            filter.setPageNumber(pageNumber);
            filter.setPageSize(pageLength);
        }

        filter.setBeginDate(dateTimeBegin);
        filter.setEndDate(dateTimeEnd);
        filter.setOrdering(Ordering.ASCENDING);

        result = qService.query(filter);
        Iterator<HistoricItem> it = result.iterator();

        // Iterate through the data
        HistoricItem lastItem = null;
        while (it.hasNext()) {
            HistoricItem historicItem = it.next();
            state = historicItem.getState();

            // For 'binary' states, we need to replicate the data
            // to avoid diagonal lines
            if (state instanceof OnOffType || state instanceof OpenClosedType) {
                if (lastItem != null) {
                    dto.addData(historicItem.getTimestamp().getTime(), lastItem.getState());
                    quantity++;
                }
            }

            dto.addData(historicItem.getTimestamp().getTime(), state);
            quantity++;
            lastItem = historicItem;
        }

        if (boundary) {
            // Get the value after the end time.
            filter.setBeginDate(dateTimeEnd);
            filter.setPageSize(1);
            filter.setOrdering(Ordering.ASCENDING);
            result = qService.query(filter);
            if (result.iterator().hasNext()) {
                dto.addData(dateTimeEnd.toInstant().toEpochMilli(), result.iterator().next().getState());
                quantity++;
            }
        }

        dto.datapoints = Long.toString(quantity);
        return dto;
    }
}
