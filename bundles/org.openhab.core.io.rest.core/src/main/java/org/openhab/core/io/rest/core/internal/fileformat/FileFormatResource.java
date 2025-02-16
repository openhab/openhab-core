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
package org.openhab.core.io.rest.core.internal.fileformat;

import static org.openhab.core.config.discovery.inbox.InboxPredicates.forThingUID;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.Role;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.ConfigUtil;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.inbox.Inbox;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.io.rest.DTOMapper;
import org.openhab.core.io.rest.LocaleService;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.io.rest.core.item.EnrichedItemDTO;
import org.openhab.core.io.rest.core.item.EnrichedItemDTOMapper;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemBuilderFactory;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.items.dto.ItemDTOMapper;
import org.openhab.core.items.dto.MetadataDTO;
import org.openhab.core.items.syntax.ItemSyntaxGenerator;
import org.openhab.core.items.syntax.ItemSyntaxParser;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingFactory;
import org.openhab.core.thing.dto.ChannelDTO;
import org.openhab.core.thing.dto.ThingDTO;
import org.openhab.core.thing.dto.ThingDTOMapper;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.syntax.ThingSyntaxGenerator;
import org.openhab.core.thing.syntax.ThingSyntaxParser;
import org.openhab.core.thing.type.BridgeType;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeRegistry;
import org.openhab.core.thing.util.ThingHelper;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JSONRequired;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * This class acts as a REST resource for items and provides different methods to interact with them, like ...
 *
 * <p>
 * This resource is registered with the Jersey servlet.
 *
 * @author Laurent Garnier - Initial contribution
 */
@Component
@JaxrsResource
@JaxrsName(FileFormatResource.PATH_FILE_FORMAT)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(FileFormatResource.PATH_FILE_FORMAT)
@Tag(name = FileFormatResource.PATH_FILE_FORMAT)
@NonNullByDefault
public class FileFormatResource implements RESTResource {

    /** The URI path to this resource */
    public static final String PATH_FILE_FORMAT = "file-format";

    /**
     * Replaces part of the URI builder by forwarded headers.
     *
     * @param uriBuilder the URI builder
     * @param httpHeaders the HTTP headers
     */
    private static void respectForwarded(final UriBuilder uriBuilder, final @Context HttpHeaders httpHeaders) {
        Optional.ofNullable(httpHeaders.getHeaderString("X-Forwarded-Host")).ifPresent(host -> {
            final int pos1 = host.indexOf("[");
            final int pos2 = host.indexOf("]");
            final String hostWithIpv6 = (pos1 >= 0 && pos2 > pos1) ? host.substring(pos1, pos2 + 1) : null;
            final String[] parts = hostWithIpv6 == null ? host.split(":") : host.substring(pos2 + 1).split(":");
            uriBuilder.host(hostWithIpv6 != null ? hostWithIpv6 : parts[0]);
            if (parts.length > 1) {
                uriBuilder.port(Integer.parseInt(parts[1]));
            }
        });
        Optional.ofNullable(httpHeaders.getHeaderString("X-Forwarded-Proto")).ifPresent(uriBuilder::scheme);
    }

    private final Logger logger = LoggerFactory.getLogger(FileFormatResource.class);

    private final DTOMapper dtoMapper;
    private final ItemBuilderFactory itemBuilderFactory;
    private final ItemRegistry itemRegistry;
    private final MetadataRegistry metadataRegistry;
    private final ItemChannelLinkRegistry itemChannelLinkRegistry;
    private final ThingRegistry thingRegistry;
    private final Inbox inbox;
    private final ThingTypeRegistry thingTypeRegistry;
    private final ChannelTypeRegistry channelTypeRegistry;
    private final ConfigDescriptionRegistry configDescRegistry;
    private final LocaleService localeService;
    private final TimeZoneProvider timeZoneProvider;
    private final Map<String, ItemSyntaxGenerator> itemSyntaxGenerators = new ConcurrentHashMap<>();
    private final Map<String, ItemSyntaxParser> itemSyntaxParsers = new ConcurrentHashMap<>();
    private final Map<String, ThingSyntaxGenerator> thingSyntaxGenerators = new ConcurrentHashMap<>();
    private final Map<String, ThingSyntaxParser> thingSyntaxParsers = new ConcurrentHashMap<>();

    @Activate
    public FileFormatResource(//
            final @Reference DTOMapper dtoMapper, //
            final @Reference ItemBuilderFactory itemBuilderFactory, //
            final @Reference ItemRegistry itemRegistry, //
            final @Reference MetadataRegistry metadataRegistry,
            final @Reference ItemChannelLinkRegistry itemChannelLinkRegistry,
            final @Reference ThingRegistry thingRegistry, //
            final @Reference Inbox inbox, //
            final @Reference ThingTypeRegistry thingTypeRegistry, //
            final @Reference ChannelTypeRegistry channelTypeRegistry, //
            final @Reference ConfigDescriptionRegistry configDescRegistry, //
            final @Reference LocaleService localeService, //
            final @Reference TimeZoneProvider timeZoneProvider) {
        this.dtoMapper = dtoMapper;
        this.itemBuilderFactory = itemBuilderFactory;
        this.itemRegistry = itemRegistry;
        this.metadataRegistry = metadataRegistry;
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
        this.thingRegistry = thingRegistry;
        this.inbox = inbox;
        this.thingTypeRegistry = thingTypeRegistry;
        this.channelTypeRegistry = channelTypeRegistry;
        this.configDescRegistry = configDescRegistry;
        this.localeService = localeService;
        this.timeZoneProvider = timeZoneProvider;
    }

    @Deactivate
    void deactivate() {
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void addItemSyntaxGenerator(ItemSyntaxGenerator itemSyntaxGenerator) {
        itemSyntaxGenerators.put(itemSyntaxGenerator.getGeneratorFormat(), itemSyntaxGenerator);
    }

    protected void removeItemSyntaxGenerator(ItemSyntaxGenerator itemSyntaxGenerator) {
        itemSyntaxGenerators.remove(itemSyntaxGenerator.getGeneratorFormat());
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void addItemSyntaxParser(ItemSyntaxParser itemSyntaxParser) {
        itemSyntaxParsers.put(itemSyntaxParser.getParserFormat(), itemSyntaxParser);
    }

    protected void removeItemSyntaxParser(ItemSyntaxParser itemSyntaxGenerator) {
        itemSyntaxParsers.remove(itemSyntaxGenerator.getParserFormat());
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void addThingSyntaxGenerator(ThingSyntaxGenerator thingSyntaxGenerator) {
        thingSyntaxGenerators.put(thingSyntaxGenerator.getGeneratorFormat(), thingSyntaxGenerator);
    }

    protected void removeThingSyntaxGenerator(ThingSyntaxGenerator thingSyntaxGenerator) {
        thingSyntaxGenerators.remove(thingSyntaxGenerator.getGeneratorFormat());
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void addThingSyntaxParser(ThingSyntaxParser thingSyntaxParser) {
        thingSyntaxParsers.put(thingSyntaxParser.getParserFormat(), thingSyntaxParser);
    }

    protected void removeThingSyntaxParser(ThingSyntaxParser thingSyntaxParser) {
        thingSyntaxParsers.remove(thingSyntaxParser.getParserFormat());
    }

    private UriBuilder uriBuilder(final UriInfo uriInfo, final HttpHeaders httpHeaders) {
        final UriBuilder uriBuilder = uriInfo.getBaseUriBuilder().path(PATH_FILE_FORMAT).path("{itemName}");
        respectForwarded(uriBuilder, httpHeaders);
        return uriBuilder;
    }

    @GET
    @RolesAllowed({ Role.ADMIN })
    @Path("/existing/items")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(operationId = "createFileFormatForAllItems", summary = "Create file format for existing items in the items registry.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode = "400", description = "Unsupported syntax generator.") })
    public Response createFileFormatForAllItems(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @DefaultValue("DSL") @QueryParam("format") @Parameter(description = "syntax format") String format,
            @DefaultValue("true") @QueryParam("hideDefaultParameters") @Parameter(description = "hide the configuration parameters having the default value") boolean hideDefaultParameters) {
        ItemSyntaxGenerator generator = itemSyntaxGenerators.get(format);
        if (generator == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("No syntax generator available for format " + format + "!").build();
        }
        Collection<Item> items = itemRegistry.getAll();
        return Response.ok(generator.generateSyntax(sortItems(items), getMetadata(items), hideDefaultParameters))
                .build();
    }

    @POST
    @RolesAllowed({ Role.ADMIN })
    @Path("/parse/items")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "parseFileFormat", summary = "Parse items from file format.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = EnrichedItemDTO.class), uniqueItems = true))),
                    @ApiResponse(responseCode = "400", description = "Unsupported syntax parser."),
                    @ApiResponse(responseCode = "400", description = "Invalid syntax.") })
    public Response parseFileFormat(final @Context UriInfo uriInfo, final @Context HttpHeaders httpHeaders,
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @DefaultValue("DSL") @QueryParam("format") @Parameter(description = "syntax format") String format,
            @Parameter(description = "item syntax", required = true) String syntax) {
        final Locale locale = localeService.getLocale(language);
        final ZoneId zoneId = timeZoneProvider.getTimeZone();

        ItemSyntaxParser parser = itemSyntaxParsers.get(format);
        if (parser == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("No syntax parser available for format " + format + "!").build();
        }

        Collection<Item> items = new ArrayList<>();
        Collection<Metadata> metadata = new ArrayList<>();
        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        if (!parser.parseSyntax(syntax, items, metadata, errors, warnings)) {
            return Response.status(Response.Status.BAD_REQUEST).entity(errors.toString()).build();
        }

        List<EnrichedItemDTO> itemsDTO = new ArrayList<>();
        items.forEach(item -> {
            EnrichedItemDTO dto = EnrichedItemDTOMapper.map(item, false, null, uriBuilder(uriInfo, httpHeaders), locale,
                    zoneId);

            Map<String, Object> metadataMap = new HashMap<>();
            metadata.stream().filter(md -> dto.name.equals(md.getUID().getItemName())).forEach(md -> {
                MetadataDTO mdDto = new MetadataDTO();
                mdDto.value = md.getValue();
                mdDto.config = md.getConfiguration().isEmpty() ? null : md.getConfiguration();
                metadataMap.put(md.getUID().getNamespace(), mdDto);
            });
            if (!metadataMap.isEmpty()) {
                // we only set it in the dto if there is really data available
                dto.metadata = metadataMap;
            }

            dto.editable = true;

            itemsDTO.add(dto);
        });
        return Response.ok(itemsDTO).build();
    }

    @POST
    @RolesAllowed({ Role.ADMIN })
    @Path("/create/items")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(operationId = "createFileFormat", summary = "Create items in file format.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode = "400", description = "Unsupported syntax generator."),
                    @ApiResponse(responseCode = "400", description = "Invalid input (items data).") })
    public Response createFileFormat(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @DefaultValue("DSL") @QueryParam("format") @Parameter(description = "syntax format") String format,
            @DefaultValue("true") @QueryParam("hideDefaultParameters") @Parameter(description = "hide the configuration parameters having the default value") boolean hideDefaultParameters,
            @Parameter(description = "items data", required = true) EnrichedItemDTO[] itemsData) {
        ItemSyntaxGenerator generator = itemSyntaxGenerators.get(format);
        if (generator == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("No syntax generator available for format " + format + "!").build();
        }

        List<Item> items = new ArrayList<>();
        Collection<Metadata> metadata = new ArrayList<>();
        for (EnrichedItemDTO itemData : itemsData) {
            String name = itemData.name;
            if (name == null || name.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Item name missing in items data!").build();
            }

            Item item;
            try {
                item = ItemDTOMapper.map(itemData, itemBuilderFactory);
                if (item == null) {
                    return Response.status(Response.Status.BAD_REQUEST).entity("Invalid item type in items data!")
                            .build();
                }
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Invalid item name in items data!").build();
            }
            items.add(item);

            if (itemData.metadata != null) {
                for (Map.Entry<String, Object> entry : itemData.metadata.entrySet()) {
                    // MetadataKey key = new MetadataKey(entry.getKey(), name);
                    // MetadataDTO mdDto = (MetadataDTO) entry.getValue();
                    // Metadata md = new Metadata(key, Objects.requireNonNull(mdDto.value), mdDto.config);
                    // metadata.add(md);
                }
            }
        }

        return Response.ok(generator.generateSyntax(items, metadata, hideDefaultParameters)).build();
    }

    @GET
    @RolesAllowed({ Role.ADMIN })
    @Path("/existing/things")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(operationId = "createFileFormatForAllThings", summary = "Create file format for existing things in the things registry.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode = "400", description = "Unsupported syntax generator.") })
    public Response createFileFormatForAllThings(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @DefaultValue("DSL") @QueryParam("format") @Parameter(description = "syntax format") String format,
            @DefaultValue("true") @QueryParam("hideDefaultParameters") @Parameter(description = "hide the configuration parameters having the default value") boolean hideDefaultParameters) {
        ThingSyntaxGenerator generator = thingSyntaxGenerators.get(format);
        if (generator == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("No syntax generator available for format " + format + "!").build();
        }
        return Response.ok(generator.generateSyntax(sortThings(thingRegistry.getAll()), hideDefaultParameters)).build();
    }

    @POST
    @RolesAllowed({ Role.ADMIN })
    @Path("/parse/things")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "parseFileFormat", summary = "Parse things from file format.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ThingDTO.class), uniqueItems = true))),
                    @ApiResponse(responseCode = "400", description = "Unsupported syntax parser."),
                    @ApiResponse(responseCode = "400", description = "Invalid syntax.") })
    public Response parseFileFormat(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @DefaultValue("DSL") @QueryParam("format") @Parameter(description = "syntax format") String format,
            @Parameter(description = "thing syntax", required = true) String syntax) {
        final Locale locale = localeService.getLocale(language);

        ThingSyntaxParser parser = thingSyntaxParsers.get(format);
        if (parser == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("No syntax parser available for format " + format + "!").build();
        }

        Collection<Thing> things = new ArrayList<>();
        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        if (!parser.parseSyntax(syntax, things, errors, warnings)) {
            return Response.status(Response.Status.BAD_REQUEST).entity(errors.toString()).build();
        }

        List<ThingDTO> thingsDTO = new ArrayList<>();
        things.forEach(thing -> {
            thingsDTO.add(ThingDTOMapper.map(thing));
        });
        return Response.ok(thingsDTO).build();
    }

    @POST
    @RolesAllowed({ Role.ADMIN })
    @Path("/create/things")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(operationId = "createFileFormat", summary = "Create things in file format.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode = "400", description = "Unsupported syntax generator."),
                    @ApiResponse(responseCode = "400", description = "Invalid input (things data).") })
    public Response createFileFormat(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @DefaultValue("DSL") @QueryParam("format") @Parameter(description = "syntax format") String format,
            @DefaultValue("true") @QueryParam("hideDefaultParameters") @Parameter(description = "hide the configuration parameters having the default value") boolean hideDefaultParameters,
            @Parameter(description = "things data", required = true) ThingDTO[] thingsData) {
        ThingSyntaxGenerator generator = thingSyntaxGenerators.get(format);
        if (generator == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("No syntax generator available for format " + format + "!").build();
        }

        List<Thing> things = new ArrayList<>();
        for (ThingDTO thingBean : thingsData) {
            ThingUID thingUID = thingBean.UID == null ? null : new ThingUID(thingBean.UID);
            ThingTypeUID thingTypeUID = new ThingTypeUID(thingBean.thingTypeUID);

            ThingUID bridgeUID = null;

            if (thingBean.bridgeUID != null) {
                bridgeUID = new ThingUID(thingBean.bridgeUID);
                if (thingUID != null && (!thingUID.getBindingId().equals(bridgeUID.getBindingId())
                        || !thingUID.getBridgeIds().contains(bridgeUID.getId()))) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("Thing UID '" + thingUID + "' does not match bridge UID '" + bridgeUID + "'")
                            .build();
                }
            }

            // turn the ThingDTO's configuration into a Configuration
            Configuration configuration = new Configuration(
                    normalizeConfiguration(thingBean.configuration, thingTypeUID, thingUID));
            if (thingUID != null) {
                normalizeChannels(thingBean, thingUID);
            }

            Thing thing = thingRegistry.createThingOfType(thingTypeUID, thingUID, bridgeUID, thingBean.label,
                    configuration);

            if (thing != null) {
                if (thingBean.properties != null) {
                    for (Entry<String, String> entry : thingBean.properties.entrySet()) {
                        thing.setProperty(entry.getKey(), entry.getValue());
                    }
                }
                if (thingBean.location != null) {
                    thing.setLocation(thingBean.location);
                }
                if (thingBean.channels != null && !thingBean.channels.isEmpty()) {
                    ThingDTO thingChannels = new ThingDTO();
                    thingChannels.channels = thingBean.channels;
                    thing = ThingHelper.merge(thing, thingChannels);
                }
            } else if (thingUID != null) {
                // if there wasn't any ThingFactory capable of creating the thing,
                // we create the Thing exactly the way we received it, i.e. we
                // cannot take its thing type into account for automatically
                // populating channels and properties.
                thing = ThingDTOMapper.map(thingBean,
                        thingTypeRegistry.getThingType(thingTypeUID) instanceof BridgeType);
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("A thing UID must be provided, since no binding can create the thing!").build();
            }

            things.add(thing);
        }

        return Response.ok(generator.generateSyntax(things, hideDefaultParameters)).build();
    }

    @GET
    @Path("/existing/thing-from-inbox/{thingUID}")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(operationId = "generateSyntaxForDiscoveryResult", summary = "Create file format for an existing thing in the discovey registry.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Unsupported syntax generator."),
            @ApiResponse(responseCode = "404", description = "Discovery result not found in the inbox or thing type not found.") })
    public Response generateSyntaxForDiscoveryResult(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @PathParam("thingUID") @Parameter(description = "thingUID") String thingUID,
            @DefaultValue("DSL") @QueryParam("format") @Parameter(description = "syntax format") String format,
            @DefaultValue("true") @QueryParam("hideDefaultParameters") @Parameter(description = "hide the configuration parameters having the default value") boolean hideDefaultParameters) {
        ThingSyntaxGenerator generator = thingSyntaxGenerators.get(format);
        if (generator == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("No syntax generator available for format " + format + "!").build();
        }

        List<DiscoveryResult> results = inbox.getAll().stream().filter(forThingUID(new ThingUID(thingUID))).toList();
        if (results.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Discovery result for thing with UID " + thingUID + " not found in the inbox!").build();
        }
        DiscoveryResult result = results.get(0);
        ThingType thingType = thingTypeRegistry.getThingType(result.getThingTypeUID());
        if (thingType == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Thing type with UID " + result.getThingTypeUID() + " does not exist!").build();
        }

        return Response.ok(generator.generateSyntax(List.of(simulateThing(result, thingType)), hideDefaultParameters))
                .build();
    }

    /*
     * Get all the metadata for a list of items including channel links mapped to metadata in the namespace "channel"
     */
    private Collection<Metadata> getMetadata(Collection<Item> items) {
        Collection<Metadata> metadata = new ArrayList<>();
        for (Item item : items) {
            String itemName = item.getName();
            metadataRegistry.getAll().stream().filter(md -> md.getUID().getItemName().equals(itemName)).forEach(md -> {
                metadata.add(md);
            });
            itemChannelLinkRegistry.getLinks(itemName).forEach(link -> {
                MetadataKey key = new MetadataKey("channel", itemName);
                Metadata md = new Metadata(key, link.getLinkedUID().getAsString(),
                        link.getConfiguration().getProperties());
                metadata.add(md);
            });
        }
        return metadata;
    }

    /*
     * Sort the items in such a way:
     * - group items are before non group items
     * - group items are sorted to have as much as possible ancestors before their children
     * - items not linked to a channel are before items linked to a channel
     * - items linked to a channel are grouped by thing UID
     * - items linked to the same thing UID are sorted by item name
     */
    private List<Item> sortItems(Collection<Item> items) {
        List<Item> groups = items.stream().filter(item -> item instanceof GroupItem).sorted((item1, item2) -> {
            return item1.getName().compareTo(item2.getName());
        }).collect(Collectors.toList());

        List<Item> topGroups = groups.stream().filter(group -> group.getGroupNames().isEmpty())
                .sorted((group1, group2) -> {
                    return group1.getName().compareTo(group2.getName());
                }).collect(Collectors.toList());

        List<Item> groupTree = new ArrayList<>();
        for (Item group : topGroups) {
            fillGroupTree(groupTree, group);
        }

        if (groupTree.size() != groups.size()) {
            logger.warn("Something want wrong when sorting groups; failback to a sort by name.");
            groupTree = groups;
        }

        List<Item> nonGroups = items.stream().filter(item -> !(item instanceof GroupItem)).sorted((item1, item2) -> {
            Set<ItemChannelLink> channelLinks1 = itemChannelLinkRegistry.getLinks(item1.getName());
            String thingUID1 = channelLinks1.isEmpty() ? null
                    : channelLinks1.iterator().next().getLinkedUID().getThingUID().getAsString();
            Set<ItemChannelLink> channelLinks2 = itemChannelLinkRegistry.getLinks(item2.getName());
            String thingUID2 = channelLinks2.isEmpty() ? null
                    : channelLinks2.iterator().next().getLinkedUID().getThingUID().getAsString();

            if (thingUID1 == null && thingUID2 != null) {
                return -1;
            } else if (thingUID1 != null && thingUID2 == null) {
                return 1;
            } else if (thingUID1 != null && thingUID2 != null && !thingUID1.equals(thingUID2)) {
                return thingUID1.compareTo(thingUID2);
            }
            return item1.getName().compareTo(item2.getName());
        }).collect(Collectors.toList());

        return Stream.of(groupTree, nonGroups).flatMap(List::stream).collect(Collectors.toList());
    }

    private void fillGroupTree(List<Item> groups, Item item) {
        if (item instanceof GroupItem group && !groups.contains(group)) {
            groups.add(group);
            List<Item> members = group.getMembers().stream().sorted((member1, member2) -> {
                return member1.getName().compareTo(member2.getName());
            }).collect(Collectors.toList());
            for (Item member : members) {
                fillGroupTree(groups, member);
            }
        }
    }

    /*
     * Sort the things in such a way:
     * - things are grouped by binding, sorted by natural order of binding name
     * - all things of a binding are sorted to follow the tree, that is bridge thing is before its sub-things
     * - all things of a binding at a certain tree depth are sorted by thing UID
     */
    private List<Thing> sortThings(Collection<Thing> things) {
        List<Thing> thingTree = new ArrayList<>();
        Set<String> bindings = things.stream().map(thing -> thing.getUID().getBindingId()).collect(Collectors.toSet());
        for (String binding : bindings.stream().sorted().collect(Collectors.toList())) {
            List<Thing> topThings = things.stream()
                    .filter(thing -> thing.getUID().getBindingId().equals(binding) && thing.getBridgeUID() == null)
                    .sorted((thing1, thing2) -> {
                        return thing1.getUID().getAsString().compareTo(thing2.getUID().getAsString());
                    }).collect(Collectors.toList());
            for (Thing thing : topThings) {
                fillThingTree(thingTree, thing);
            }
        }
        return thingTree;
    }

    private void fillThingTree(List<Thing> things, Thing thing) {
        if (!things.contains(thing)) {
            things.add(thing);
            if (thing instanceof Bridge bridge) {
                List<Thing> subThings = bridge.getThings().stream().sorted((thing1, thing2) -> {
                    return thing1.getUID().getAsString().compareTo(thing2.getUID().getAsString());
                }).collect(Collectors.toList());
                for (Thing subThing : subThings) {
                    fillThingTree(things, subThing);
                }
            }
        }
    }

    private @Nullable Map<String, Object> normalizeConfiguration(@Nullable Map<String, Object> properties,
            ThingTypeUID thingTypeUID, @Nullable ThingUID thingUID) {
        if (properties == null || properties.isEmpty()) {
            return properties;
        }

        ThingType thingType = thingTypeRegistry.getThingType(thingTypeUID);
        if (thingType == null) {
            return properties;
        }

        List<ConfigDescription> configDescriptions = new ArrayList<>(2);

        URI descURI = thingType.getConfigDescriptionURI();
        if (descURI != null) {
            ConfigDescription typeConfigDesc = configDescRegistry.getConfigDescription(descURI);
            if (typeConfigDesc != null) {
                configDescriptions.add(typeConfigDesc);
            }
        }

        if (thingUID != null) {
            ConfigDescription thingConfigDesc = configDescRegistry
                    .getConfigDescription(getConfigDescriptionURI(thingUID));
            if (thingConfigDesc != null) {
                configDescriptions.add(thingConfigDesc);
            }
        }

        if (configDescriptions.isEmpty()) {
            return properties;
        }

        return ConfigUtil.normalizeTypes(properties, configDescriptions);
    }

    private @Nullable Map<String, Object> normalizeConfiguration(Map<String, Object> properties,
            ChannelTypeUID channelTypeUID, ChannelUID channelUID) {
        if (properties == null || properties.isEmpty()) {
            return properties;
        }

        ChannelType channelType = channelTypeRegistry.getChannelType(channelTypeUID);
        if (channelType == null) {
            return properties;
        }

        List<ConfigDescription> configDescriptions = new ArrayList<>(2);
        URI descURI = channelType.getConfigDescriptionURI();
        if (descURI != null) {
            ConfigDescription typeConfigDesc = configDescRegistry.getConfigDescription(descURI);
            if (typeConfigDesc != null) {
                configDescriptions.add(typeConfigDesc);
            }
        }
        if (getConfigDescriptionURI(channelUID) != null) {
            ConfigDescription channelConfigDesc = configDescRegistry
                    .getConfigDescription(getConfigDescriptionURI(channelUID));
            if (channelConfigDesc != null) {
                configDescriptions.add(channelConfigDesc);
            }
        }

        if (configDescriptions.isEmpty()) {
            return properties;
        }

        return ConfigUtil.normalizeTypes(properties, configDescriptions);
    }

    private void normalizeChannels(ThingDTO thingBean, ThingUID thingUID) {
        if (thingBean.channels != null) {
            for (ChannelDTO channelBean : thingBean.channels) {
                if (channelBean.channelTypeUID != null) {
                    channelBean.configuration = normalizeConfiguration(channelBean.configuration,
                            new ChannelTypeUID(channelBean.channelTypeUID), new ChannelUID(thingUID, channelBean.id));
                }
            }
        }
    }

    private URI getConfigDescriptionURI(ThingUID thingUID) {
        String uriString = "thing:" + thingUID;
        try {
            return new URI(uriString);
        } catch (URISyntaxException e) {
            throw new BadRequestException("Invalid URI syntax: " + uriString);
        }
    }

    private URI getConfigDescriptionURI(ChannelUID channelUID) {
        String uriString = "channel:" + channelUID;
        try {
            return new URI(uriString);
        } catch (URISyntaxException e) {
            throw new BadRequestException("Invalid URI syntax: " + uriString);
        }
    }

    /*
     * Create a thing from a discovery result without inserting it in the thing registry
     */
    private Thing simulateThing(DiscoveryResult result, ThingType thingType) {
        Map<String, Object> configParams = new HashMap<>();
        List<ConfigDescriptionParameter> configDescriptionParameters = List.of();
        URI descURI = thingType.getConfigDescriptionURI();
        if (descURI != null) {
            ConfigDescription desc = configDescRegistry.getConfigDescription(descURI);
            if (desc != null) {
                configDescriptionParameters = desc.getParameters();
            }
        }
        for (ConfigDescriptionParameter param : configDescriptionParameters) {
            Object value = result.getProperties().get(param.getName());
            if (value != null) {
                configParams.put(param.getName(), ConfigUtil.normalizeType(value, param));
            }
        }
        Configuration config = new Configuration(configParams);
        return ThingFactory.createThing(thingType, result.getThingUID(), config, result.getBridgeUID(),
                configDescRegistry);
    }
}
