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
package org.openhab.core.io.rest.core.internal.persistence;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.annotation.security.RolesAllowed;
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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.openhab.core.auth.Role;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.io.rest.JSONResponse;
import org.openhab.core.io.rest.LocaleService;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.persistence.FilterCriteria;
import org.openhab.core.persistence.FilterCriteria.Ordering;
import org.openhab.core.persistence.HistoricItem;
import org.openhab.core.persistence.ModifiablePersistenceService;
import org.openhab.core.persistence.PersistenceService;
import org.openhab.core.persistence.PersistenceServiceRegistry;
import org.openhab.core.persistence.QueryablePersistenceService;
import org.openhab.core.persistence.dto.ItemHistoryDTO;
import org.openhab.core.persistence.dto.PersistenceServiceDTO;
import org.openhab.core.types.State;
import org.openhab.core.types.TypeParser;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
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
 * @author Chris Jackson - Initial contribution
 * @author Kai Kreuzer - Refactored to use PersistenceServiceRegistryImpl
 * @author Franck Dechavanne - Added DTOs to ApiResponses
 * @author Erdoan Hadzhiyusein - Adapted the convertTime() method to work with the new DateTimeType
 * @author Lyubomir Papazov - Change java.util.Date references to be of type java.time.ZonedDateTime
 */
@Path(PersistenceResource.PATH)
@Api(value = PersistenceResource.PATH)
@Component
public class PersistenceResource implements RESTResource {

    private final Logger logger = LoggerFactory.getLogger(PersistenceResource.class);

    private static final String MODIFYABLE = "Modifiable";
    private static final String QUERYABLE = "Queryable";
    private static final String STANDARD = "Standard";

    // The URI path to this resource
    public static final String PATH = "persistence";

    private ItemRegistry itemRegistry;
    private PersistenceServiceRegistry persistenceServiceRegistry;
    private TimeZoneProvider timeZoneProvider;

    private LocaleService localeService;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setPersistenceServiceRegistry(PersistenceServiceRegistry persistenceServiceRegistry) {
        this.persistenceServiceRegistry = persistenceServiceRegistry;
    }

    protected void unsetPersistenceServiceRegistry(PersistenceServiceRegistry persistenceServiceRegistry) {
        this.persistenceServiceRegistry = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setTimeZoneProvider(TimeZoneProvider timeZoneProvider) {
        this.timeZoneProvider = timeZoneProvider;
    }

    protected void unsetTimeZoneProvider(TimeZoneProvider timeZoneProvider) {
        this.timeZoneProvider = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    protected void unsetItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setLocaleService(LocaleService localeService) {
        this.localeService = localeService;
    }

    protected void unsetLocaleService(LocaleService localeService) {
        this.localeService = null;
    }

    @GET
    @RolesAllowed({ Role.ADMIN })
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Gets a list of persistence services.", response = String.class, responseContainer = "List")
    @ApiResponses(value = @ApiResponse(code = 200, message = "OK", response = String.class, responseContainer = "List"))
    public Response httpGetPersistenceServices(@Context HttpHeaders headers,
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = HttpHeaders.ACCEPT_LANGUAGE) String language) {
        Locale locale = localeService.getLocale(language);

        Object responseObject = getPersistenceServiceList(locale);
        return Response.ok(responseObject).build();
    }

    @GET
    @RolesAllowed({ Role.ADMIN })
    @Path("/items")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Gets a list of items available via a specific persistence service.", response = String.class, responseContainer = "List")
    @ApiResponses(value = @ApiResponse(code = 200, message = "OK", response = String.class, responseContainer = "List"))
    public Response httpGetPersistenceServiceItems(@Context HttpHeaders headers,
            @ApiParam(value = "Id of the persistence service. If not provided the default service will be used", required = false) @QueryParam("serviceId") String serviceId) {
        return getServiceItemList(serviceId);
    }

    @GET
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Path("/items/{itemname: [a-zA-Z_0-9]+}")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Gets item persistence data from the persistence service.", response = ItemHistoryDTO.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = ItemHistoryDTO.class),
            @ApiResponse(code = 404, message = "Unknown Item or persistence service") })
    public Response httpGetPersistenceItemData(@Context HttpHeaders headers,
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
        return getItemHistoryDTO(serviceId, itemName, startTime, endTime, pageNumber, pageLength, boundary);
    }

    @DELETE
    @RolesAllowed({ Role.ADMIN })
    @Path("/items/{itemname: [a-zA-Z_0-9]+}")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Delete item data from a specific persistence service.", response = String.class, responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = String.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Invalid filter parameters"),
            @ApiResponse(code = 404, message = "Unknown persistence service") })
    public Response httpDeletePersistenceServiceItem(@Context HttpHeaders headers,
            @ApiParam(value = "Id of the persistence service.", required = true) @QueryParam("serviceId") String serviceId,
            @ApiParam(value = "The item name.", required = true) @PathParam("itemname") String itemName,
            @ApiParam(value = "Start time of the data to return. [" + DateTimeType.DATE_PATTERN_WITH_TZ_AND_MS
                    + "]", required = true) @QueryParam("starttime") String startTime,
            @ApiParam(value = "End time of the data to return. [" + DateTimeType.DATE_PATTERN_WITH_TZ_AND_MS
                    + "]", required = true) @QueryParam("endtime") String endTime) {
        return deletePersistenceItemData(serviceId, itemName, startTime, endTime);
    }

    @PUT
    @RolesAllowed({ Role.ADMIN })
    @Path("/items/{itemname: [a-zA-Z_0-9]+}")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Stores item persistence data into the persistence service.", response = ItemHistoryDTO.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = ItemHistoryDTO.class),
            @ApiResponse(code = 404, message = "Unknown Item or persistence service") })
    public Response httpPutPersistenceItemData(@Context HttpHeaders headers,
            @ApiParam(value = "Id of the persistence service. If not provided the default service will be used", required = false) @QueryParam("serviceId") String serviceId,
            @ApiParam(value = "The item name.", required = true) @PathParam("itemname") String itemName,
            @ApiParam(value = "Time of the data to be stored. Will default to current time. ["
                    + DateTimeType.DATE_PATTERN_WITH_TZ_AND_MS + "]", required = true) @QueryParam("time") String time,
            @ApiParam(value = "The state to store.", required = true) @QueryParam("state") String value) {
        return putItemState(serviceId, itemName, value, time);
    }

    private ZonedDateTime convertTime(String sTime) {
        DateTimeType dateTime = new DateTimeType(sTime);
        return dateTime.getZonedDateTime();
    }

    private Response getItemHistoryDTO(String serviceId, String itemName, String timeBegin, String timeEnd,
            int pageNumber, int pageLength, boolean boundary) {
        // Benchmarking timer...
        long timerStart = System.currentTimeMillis();

        ItemHistoryDTO dto = createDTO(serviceId, itemName, timeBegin, timeEnd, pageNumber, pageLength, boundary);
        if (dto == null) {
            JSONResponse.createErrorResponse(Status.BAD_REQUEST, "Persistence service not queryable: " + serviceId);
        }
        logger.debug("Persistence returned {} rows in {}ms", dto.datapoints, System.currentTimeMillis() - timerStart);

        return JSONResponse.createResponse(Status.OK, dto, "");
    }

    protected ItemHistoryDTO createDTO(String serviceId, String itemName, String timeBegin, String timeEnd,
            int pageNumber, int pageLength, boolean boundary) {
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
            dateTimeBegin = convertTime(timeBegin);
        }

        if (timeEnd != null) {
            dateTimeEnd = convertTime(timeEnd);
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
            if (result != null && result.iterator().hasNext()) {
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
        if (result != null) {
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
        }

        if (boundary) {
            // Get the value after the end time.
            filter.setBeginDate(dateTimeEnd);
            filter.setPageSize(1);
            filter.setOrdering(Ordering.ASCENDING);
            result = qService.query(filter);
            if (result != null && result.iterator().hasNext()) {
                dto.addData(dateTimeEnd.toInstant().toEpochMilli(), result.iterator().next().getState());
                quantity++;
            }
        }

        dto.datapoints = Long.toString(quantity);
        return dto;
    }

    /**
     * Gets a list of persistence services currently configured in the system
     *
     * @return list of persistence services as {@link ServiceBean}
     */
    private List<PersistenceServiceDTO> getPersistenceServiceList(Locale locale) {
        List<PersistenceServiceDTO> dtoList = new ArrayList<>();

        for (PersistenceService service : persistenceServiceRegistry.getAll()) {
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

            dtoList.add(serviceDTO);
        }

        return dtoList;
    }

    private Response getServiceItemList(String serviceId) {
        // If serviceId is null, then use the default service
        PersistenceService service = null;
        if (serviceId == null) {
            service = persistenceServiceRegistry.getDefault();
        } else {
            service = persistenceServiceRegistry.get(serviceId);
        }

        if (service == null) {
            logger.debug("Persistence service not found '{}'.", serviceId);
            return JSONResponse.createErrorResponse(Status.BAD_REQUEST, "Persistence service not found: " + serviceId);
        }

        if (!(service instanceof QueryablePersistenceService)) {
            logger.debug("Persistence service not queryable '{}'.", serviceId);
            return JSONResponse.createErrorResponse(Status.BAD_REQUEST,
                    "Persistence service not queryable: " + serviceId);
        }

        QueryablePersistenceService qService = (QueryablePersistenceService) service;

        return JSONResponse.createResponse(Status.OK, qService.getItemInfo(), "");
    }

    private Response deletePersistenceItemData(String serviceId, String itemName, String timeBegin, String timeEnd) {
        // For deleting, we must specify a service id - don't use the default service
        if (serviceId == null || serviceId.length() == 0) {
            logger.debug("Persistence service must be specified for delete operations.");
            return JSONResponse.createErrorResponse(Status.BAD_REQUEST,
                    "Persistence service must be specified for delete operations.");
        }

        PersistenceService service = persistenceServiceRegistry.get(serviceId);
        if (service == null) {
            logger.debug("Persistence service not found '{}'.", serviceId);
            return JSONResponse.createErrorResponse(Status.BAD_REQUEST, "Persistence service not found: " + serviceId);
        }

        if (!(service instanceof ModifiablePersistenceService)) {
            logger.warn("Persistence service not modifiable '{}'.", serviceId);
            return JSONResponse.createErrorResponse(Status.BAD_REQUEST,
                    "Persistence service not modifiable: " + serviceId);
        }

        ModifiablePersistenceService mService = (ModifiablePersistenceService) service;

        if (timeBegin == null | timeEnd == null) {
            return JSONResponse.createErrorResponse(Status.BAD_REQUEST, "The start and end time must be set");
        }

        ZonedDateTime dateTimeBegin = convertTime(timeBegin);
        ZonedDateTime dateTimeEnd = convertTime(timeEnd);
        if (dateTimeEnd.isBefore(dateTimeBegin)) {
            return JSONResponse.createErrorResponse(Status.BAD_REQUEST, "Start time must be earlier than end time");
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
            return JSONResponse.createErrorResponse(Status.BAD_REQUEST, "Invalid filter parameters.");
        }

        return Response.status(Status.OK).build();
    }

    private Response putItemState(String serviceId, String itemName, String value, String time) {
        // If serviceId is null, then use the default service
        PersistenceService service = null;
        String effectiveServiceId = serviceId != null ? serviceId : persistenceServiceRegistry.getDefaultId();
        service = persistenceServiceRegistry.get(effectiveServiceId);

        if (service == null) {
            logger.warn("Persistence service not found '{}'.", effectiveServiceId);
            return JSONResponse.createErrorResponse(Status.BAD_REQUEST,
                    "Persistence service not found: " + effectiveServiceId);
        }

        Item item;
        try {
            if (itemRegistry == null) {
                logger.warn("Item registry not set.");
                return JSONResponse.createErrorResponse(Status.CONFLICT, "Item registry not set.");
            }
            item = itemRegistry.getItem(itemName);
        } catch (ItemNotFoundException e) {
            logger.warn("Item not found '{}'.", itemName);
            return JSONResponse.createErrorResponse(Status.BAD_REQUEST, "Item not found: " + itemName);
        }

        // Try to parse a State from the input
        State state = TypeParser.parseState(item.getAcceptedDataTypes(), value);
        if (state == null) {
            // State could not be parsed
            logger.warn("Can't persist item {} with invalid state '{}'.", itemName, value);
            return JSONResponse.createErrorResponse(Status.BAD_REQUEST, "State could not be parsed: " + value);
        }

        ZonedDateTime dateTime = null;
        if (time != null && time.length() != 0) {
            dateTime = convertTime(time);
        }
        if (dateTime == null || dateTime.toEpochSecond() == 0) {
            logger.warn("Error with persistence store to {}. Time badly formatted {}.", itemName, time);
            return JSONResponse.createErrorResponse(Status.BAD_REQUEST, "Time badly formatted.");
        }

        if (!(service instanceof ModifiablePersistenceService)) {
            logger.warn("Persistence service not modifiable '{}'.", effectiveServiceId);
            return JSONResponse.createErrorResponse(Status.BAD_REQUEST,
                    "Persistence service not modifiable: " + effectiveServiceId);
        }

        ModifiablePersistenceService mService = (ModifiablePersistenceService) service;

        mService.store(item, Date.from(dateTime.toInstant()), state);
        return Response.status(Status.OK).build();
    }

    @Override
    public boolean isSatisfied() {
        return itemRegistry != null && persistenceServiceRegistry != null && timeZoneProvider != null
                && localeService != null;
    }
}
