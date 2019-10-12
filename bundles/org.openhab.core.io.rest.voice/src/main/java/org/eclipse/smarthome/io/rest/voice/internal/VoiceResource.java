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
package org.eclipse.smarthome.io.rest.voice.internal;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
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
import javax.ws.rs.core.UriInfo;

import org.eclipse.smarthome.core.auth.Role;
import org.eclipse.smarthome.core.voice.Voice;
import org.eclipse.smarthome.core.voice.VoiceManager;
import org.eclipse.smarthome.core.voice.chat.Card;
import org.eclipse.smarthome.core.voice.chat.CardRegistry;
import org.eclipse.smarthome.core.voice.text.HumanLanguageInterpreter;
import org.eclipse.smarthome.core.voice.text.InterpretationException;
import org.eclipse.smarthome.core.voice.text.InterpretationResult;
import org.eclipse.smarthome.core.voice.text.ItemNamedAttribute;
import org.eclipse.smarthome.core.voice.text.ItemResolver;
import org.eclipse.smarthome.io.rest.JSONResponse;
import org.eclipse.smarthome.io.rest.LocaleService;
import org.eclipse.smarthome.io.rest.RESTResource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * This class acts as a REST resource for voice features.
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Laurent Garnier - add TTS feature to the REST API
 * @author Laurent Garnier - new API for chat interpretation, cards management and item named attributes
 */
@Component
@Path(VoiceResource.PATH_SITEMAPS)
@RolesAllowed({ Role.USER, Role.ADMIN })
@Api(value = VoiceResource.PATH_SITEMAPS)
public class VoiceResource implements RESTResource {

    static final String PATH_SITEMAPS = "voice";

    @Context
    UriInfo uriInfo;

    private VoiceManager voiceManager;
    private LocaleService localeService;
    private CardRegistry cardRegistry;
    private ItemResolver itemResolver;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    public void setVoiceManager(VoiceManager voiceManager) {
        this.voiceManager = voiceManager;
    }

    public void unsetVoiceManager(VoiceManager voiceManager) {
        this.voiceManager = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setLocaleService(LocaleService localeService) {
        this.localeService = localeService;
    }

    protected void unsetLocaleService(LocaleService localeService) {
        this.localeService = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    public void setCardRegistry(CardRegistry cardRegistry) {
        this.cardRegistry = cardRegistry;
    }

    public void unsetCardRegistry(CardRegistry cardRegistry) {
        this.cardRegistry = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setItemNamedAttributesResolver(ItemResolver itemResolver) {
        this.itemResolver = itemResolver;
    }

    protected void unsetItemNamedAttributesResolver(ItemResolver itemResolver) {
        this.itemResolver = null;
    }

    @GET
    @Path("/interpreters")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get the list of all interpreters.", response = HumanLanguageInterpreterDTO.class, responseContainer = "List")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK") })
    public Response getInterpreters(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") String language) {
        final Locale locale = localeService.getLocale(language);
        Collection<HumanLanguageInterpreter> hlis = voiceManager.getHLIs();
        List<HumanLanguageInterpreterDTO> dtos = new ArrayList<>(hlis.size());
        for (HumanLanguageInterpreter hli : hlis) {
            dtos.add(HLIMapper.map(hli, locale));
        }
        return Response.ok(dtos).build();
    }

    @GET
    @Path("/interpreters/{id: [a-zA-Z_0-9]*}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets a single interpreter.", response = HumanLanguageInterpreterDTO.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Interpreter not found") })
    public Response getInterpreter(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") String language,
            @PathParam("id") @ApiParam(value = "interpreter id", required = true) String id) {
        final Locale locale = localeService.getLocale(language);
        HumanLanguageInterpreter hli = voiceManager.getHLI(id);
        if (hli != null) {
            HumanLanguageInterpreterDTO dto = HLIMapper.map(hli, locale);
            return Response.ok(dto).build();
        } else {
            return JSONResponse.createErrorResponse(Status.NOT_FOUND, "Interpreter not found");
        }
    }

    @POST
    @Path("/interpreters/{id: [a-zA-Z_0-9]*}")
    @Consumes(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Sends a text to a given human language interpreter for voice control.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "No human language interpreter was found."),
            @ApiResponse(code = 400, message = "interpretation exception occurs") })
    public Response interpret(@HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") String language,
            @ApiParam(value = "text to interpret", required = true) String text,
            @PathParam("id") @ApiParam(value = "interpreter id", required = true) String id) {
        final Locale locale = localeService.getLocale(language);
        HumanLanguageInterpreter hli = voiceManager.getHLI(id);
        if (hli != null) {
            try {
                hli.interpret(locale, text);
                return Response.ok(null, MediaType.TEXT_PLAIN).build();
            } catch (InterpretationException e) {
                return JSONResponse.createErrorResponse(Status.BAD_REQUEST, e.getMessage());
            }
        } else {
            return JSONResponse.createErrorResponse(Status.NOT_FOUND, "Interpreter not found");
        }
    }

    @POST
    @Path("/interpreters")
    @Consumes(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Sends a text to the default human language interpreter for voice control.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "No human language interpreter was found."),
            @ApiResponse(code = 400, message = "interpretation exception occurs") })
    public Response interpret(@HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") String language,
            @ApiParam(value = "text to interpret", required = true) String text) {
        final Locale locale = localeService.getLocale(language);
        HumanLanguageInterpreter hli = voiceManager.getHLI();
        if (hli != null) {
            try {
                hli.interpret(locale, text);
                return Response.ok(null, MediaType.TEXT_PLAIN).build();
            } catch (InterpretationException e) {
                return JSONResponse.createErrorResponse(Status.BAD_REQUEST, e.getMessage());
            }
        } else {
            return JSONResponse.createErrorResponse(Status.NOT_FOUND, "No interpreter found");
        }
    }

    @POST
    @Path("/chat/interpreters/{id: [a-zA-Z_0-9]*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Sends a text to a given human language interpreter for chat dialog.", response = InterpretationResultDTO.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "No human language interpreter was found."),
            @ApiResponse(code = 400, message = "interpretation exception occurs") })
    public Response interpretForChat(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") String language,
            @ApiParam(value = "text to interpret", required = true) String text,
            @PathParam("id") @ApiParam(value = "interpreter id", required = true) String id) {
        final Locale locale = localeService.getLocale(language);
        HumanLanguageInterpreter hli = voiceManager.getHLI(id);
        if (hli != null) {
            try {
                InterpretationResult result = hli.interpretForChat(locale, text);
                InterpretationResultDTO dto = (result == null) ? null : HLIMapper.map(result);
                return Response.ok(dto).build();
            } catch (InterpretationException e) {
                return JSONResponse.createErrorResponse(Status.BAD_REQUEST, e.getMessage());
            }
        } else {
            return JSONResponse.createErrorResponse(Status.NOT_FOUND, "Interpreter not found");
        }
    }

    @POST
    @Path("/chat/interpreters")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Sends a text to the default human language interpreter for chat dialog.", response = InterpretationResultDTO.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "No human language interpreter was found."),
            @ApiResponse(code = 400, message = "interpretation exception occurs") })
    public Response interpretForChat(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") String language,
            @ApiParam(value = "text to interpret", required = true) String text) {
        final Locale locale = localeService.getLocale(language);
        HumanLanguageInterpreter hli = voiceManager.getHLI();
        if (hli != null) {
            try {
                InterpretationResult result = hli.interpretForChat(locale, text);
                InterpretationResultDTO dto = (result == null) ? null : HLIMapper.map(result);
                return Response.ok(dto).build();
            } catch (InterpretationException e) {
                return JSONResponse.createErrorResponse(Status.BAD_REQUEST, e.getMessage());
            }
        } else {
            return JSONResponse.createErrorResponse(Status.NOT_FOUND, "Interpreter not found");
        }
    }

    @GET
    @Path("/voices")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get the list of all voices.", response = VoiceDTO.class, responseContainer = "List")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK") })
    public Response getVoices() {
        Collection<Voice> voices = voiceManager.getAllVoices();
        List<VoiceDTO> dtos = new ArrayList<>(voices.size());
        for (Voice voice : voices) {
            dtos.add(VoiceMapper.map(voice));
        }
        return Response.ok(dtos).build();
    }

    @GET
    @Path("/defaultvoice")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets the default voice.", response = VoiceDTO.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "No default voice was found.") })
    public Response getDefaultVoice() {
        Voice voice = voiceManager.getDefaultVoice();
        if (voice != null) {
            VoiceDTO dto = VoiceMapper.map(voice);
            return Response.ok(dto).build();
        } else {
            return JSONResponse.createErrorResponse(Status.NOT_FOUND, "Default voice not found");
        }
    }

    @POST
    @Path("/say")
    @Consumes(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Speaks a given text with a given voice through the given audio sink.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK") })
    public Response say(@ApiParam(value = "text to speak", required = true) String text,
            @ApiParam(value = "voice id", required = false) @QueryParam("voiceid") String voiceId,
            @ApiParam(value = "audio sink id", required = false) @QueryParam("sinkid") String sinkId) {
        voiceManager.say(text, voiceId, sinkId);
        return Response.ok(null, MediaType.TEXT_PLAIN).build();
    }

    @GET
    @Path("/items/attributes")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets all item named attributes.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 500, message = "An error occurred") })
    public Response getAttributes(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") String language) throws Exception {
        final Locale locale = this.localeService.getLocale(null);

        this.itemResolver.setLocale(locale);
        Map<String, Set<ItemNamedAttribute>> attributesByItemName = new HashMap<String, Set<ItemNamedAttribute>>();
        this.itemResolver.getAllItemNamedAttributes().entrySet().stream()
                .forEach(entry -> attributesByItemName.put(entry.getKey().getName(), entry.getValue()));

        return Response.ok(attributesByItemName).build();
    }

    @GET
    @Path("/cards")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets all cards of the card deck.", response = Card.class, responseContainer = "List")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 500, message = "An error occured") })
    public Response getAllCards() {
        Collection<Card> cards = this.cardRegistry.getNonEphemeral();

        return Response.ok(cards).build();
    }

    @GET
    @Path("/cards/{cardUID}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets a card from the card deck by its UID.", response = Card.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "The card with the provided UID doesn't exist"),
            @ApiResponse(code = 500, message = "An error occured") })
    public Response getCardByUid(@PathParam("cardUID") @ApiParam(value = "cardUID", required = true) String cardUID) {
        Card card = this.cardRegistry.get(cardUID);
        if (card == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        return Response.ok(card).build();
    }

    @POST
    @Path("/cards")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Creates a new card in the card deck.", response = Card.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "The card was created"),
            @ApiResponse(code = 500, message = "An error occured") })
    public Response createCard(@ApiParam(value = "card", required = true) Card card) {
        card.updateTimestamp();
        card.setEphemeral(false);
        Card existingCard = this.cardRegistry.get(card.getUID());
        if (existingCard != null && existingCard.isEphemeral()) {
            this.cardRegistry.remove(card.getUID());
        }
        Card createdCard = this.cardRegistry.add(card);

        return Response.ok(createdCard).build();
    }

    @PUT
    @Path("/cards/{cardUID}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Updates a card in the card deck.", response = Card.class)
    public Response updateCard(@PathParam("cardUID") @ApiParam(value = "cardUID", required = true) String cardUID,
            @ApiParam(value = "card", required = true) Card card) {
        if (!card.getUID().equals(cardUID)) {
            throw new InvalidParameterException(
                    "The card UID in the body of the request should match the UID in the URL");
        }
        card.updateTimestamp();
        Card updatedCard = this.cardRegistry.update(card);

        return Response.ok(updatedCard).build();
    }

    @DELETE
    @Path("/cards/{cardUID}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Deletes a card from the card deck.")
    public Response deleteCard(@PathParam("cardUID") @ApiParam(value = "cardUID", required = true) String cardUID) {
        this.cardRegistry.remove(cardUID);

        return Response.ok().build();
    }

    @PUT
    @Path("/cards/{cardUID}/bookmark")
    @ApiOperation(value = "Sets a bookmark on a card.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "The card with the provided UID doesn't exist"),
            @ApiResponse(code = 500, message = "An error occured") })
    public Response setCardBookmark(
            @PathParam("cardUID") @ApiParam(value = "cardUID", required = true) String cardUID) {
        Card card = this.cardRegistry.get(cardUID);
        if (card == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        card.setBookmark(true);
        this.cardRegistry.update(card);

        return Response.ok().build();
    }

    @GET
    @Path("/cards/recent")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Creates a new card in the card deck.", response = Card.class, responseContainer = "List")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "The card was created"),
            @ApiResponse(code = 500, message = "An error occured") })
    public Response createCard(@QueryParam(value = "skip") int skip, @QueryParam(value = "count") int count) {
        Collection<Card> cards = this.cardRegistry.getRecent(skip, count);

        return Response.ok(cards).build();
    }

    @DELETE
    @Path("/cards/{cardUID}/bookmark")
    @ApiOperation(value = "Removes the bookmark on a card.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "The card with the provided UID doesn't exist"),
            @ApiResponse(code = 500, message = "An error occured") })
    public Response unsetCardBookmark(
            @PathParam("cardUID") @ApiParam(value = "cardUID", required = true) String cardUID) {
        Card card = this.cardRegistry.get(cardUID);
        if (card == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        card.setBookmark(false);
        this.cardRegistry.update(card);

        return Response.ok().build();
    }

    @PUT
    @Path("/cards/{cardUID}/timestamp")
    @ApiOperation(value = "Updates the timestamp on a card to the current time")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "The card with the provided UID doesn't exist"),
            @ApiResponse(code = 500, message = "An error occured") })
    public Response updateCardTimestamp(
            @PathParam("cardUID") @ApiParam(value = "cardUID", required = true) String cardUID) {
        Card card = this.cardRegistry.get(cardUID);
        if (card == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        card.updateTimestamp();
        this.cardRegistry.update(card);

        return Response.ok().build();
    }

    @Override
    public boolean isSatisfied() {
        return voiceManager != null && localeService != null && cardRegistry != null && itemResolver != null;
    }
}
