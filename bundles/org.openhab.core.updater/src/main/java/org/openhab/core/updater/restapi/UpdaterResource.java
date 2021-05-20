/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.updater.restapi;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.Role;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.updater.dto.DoUpdateDTO;
import org.openhab.core.updater.dto.StatusDTO;
import org.openhab.core.updater.updaterclasses.BaseUpdater;
import org.openhab.core.updater.updaterfactory.UpdaterFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JSONRequired;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * This is a REST resource for OpenHAB self updating features.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@Component
@JaxrsResource
@JaxrsName(UpdaterResource.URL_BASE_PATH)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(UpdaterResource.URL_BASE_PATH)
@RolesAllowed({ Role.USER, Role.ADMIN })
@Tag(name = UpdaterResource.URL_BASE_PATH)
@NonNullByDefault
public class UpdaterResource implements RESTResource {

    // HTML paramaters
    private static final String HTML_SELECTED = " selected";
    private static final String HTML_VER_NAME = "%id";
    private static final String HTML_VER_TYPE = "%type";
    private static final String HTML_OPTION_FIELD = "<option%s>%s</option>";
    private static final String HTML_VERSION_FIELD = "<p>&ensp;&bull;&ensp;" + HTML_VER_NAME + "&emsp;(" + HTML_VER_TYPE
            + ")</p>";

    // HTML pages
    private static final String HTML_ROOT_PAGE =
    // @formatter:off
            "<html>"
            + "<body>"
            + "    <h2>OpenHAB Self- Updater</h2>"
            + "    <p><a href=\"updater/getStatus\">Get updater status</a></p>"
            + "    <p><a href=\"updater/doUpdate\">Start updating OpenHAB</a></p>"
            + "</body>"
            + "</html>";
    // @formatter:on

    private static final String HTML_DO_UPDATE_PAGE =
    // @formatter:off
            "<html>"
            + "<body>"
            + "    <h2>OpenHAB Self- Updater</h2>"
            + "    <form action=\"doUpdate\" method=\"post\">"
            + "        <p>Current version..</p>"
            + HTML_VERSION_FIELD
            + "        <p></p>"
            + "        <p>Available versions..</p>"
            + HTML_VERSION_FIELD
            + HTML_VERSION_FIELD
            + HTML_VERSION_FIELD
            + "        <p></p>"
            + "         <p><label for=\"targetNewVersionType\">Choose new target version type:&emsp;</label>"
            + "        <select id=\"targetNewVersionType\" name=\"targetNewVersionType\">"
            + HTML_OPTION_FIELD
            + HTML_OPTION_FIELD
            + HTML_OPTION_FIELD
            + "        </select></p>"
            + "        <p></p>"
            + "        <p>Enter the system credentials (if needed)..</p>"
            + "        <p>&emsp;User name:&emsp;<input type=\"text\" name=\"user\"></p>"
            + "        <p>&emsp;Password:&emsp;<input type=\"password\" name=\"password\"></p>"
            + "        <p>&emsp;Note: some systems (e.g. Windows) do not require user credentials.</p>"
            + "        <p></p>"
            + "        <p>Start updating OpenHAB now:&emsp;<input type=\"submit\" value=\"Execute\"></p>"
            + "    </form>"
            + "</body>"
            + "</html>";
    // @formatter:on

    // HTTP 200 OK messages
    private static final String OK_SUCCESS = "Request succeeded!";
    private static final String OK_UPDATE_STARTED = "Started OpenHAB self update process";

    // HTTP 400 BAD REQUEST messages
    private static final String BAD_REQUEST = "Bad request!";
    private static final String BAD_PASSWORD = "Invalid 'password' parameter (must be without white space and not longer than 20 characters).";
    private static final String BAD_USER = "Invalid 'user' parameter (must be aplha-numeric characters only and not longer than 20 characters).";
    private static final String BAD_VERSION = "Invalid 'targetVersion' parameter (must be STABLE / MILSETONE/ SNAPSHOT).";
    private static final String BAD_DTO = "Missing JSON DTO.";

    // HTTP 500 INTERNAL SERVER ERROR messages
    private static final String INT_SERV_ERR_UPDATER_NULL = "Updater class not initialized.";

    public static final String URL_BASE_PATH = "updater";

    private @Nullable BaseUpdater updater;

    /**
     * We use a <strong>static</strong> and <strong>single thread</strong> executor here, in order to synchronise
     * multiple doUpdate method calls both a) within the same class instance, and b) across multiple class instances.
     */
    private static final ExecutorService UPDATE_EXECUTOR = Executors.newSingleThreadExecutor();

    @Activate
    public UpdaterResource() {
        this.updater = UpdaterFactory.newUpdater();
    }

    /**
     * This method handles HTTP GET on the updater resource's root '/' path.
     * It serves an HTML page with links to its two sub- paths.
     *
     * @return HTTP 200 OK, or HTTP 500 INTERNAL SERVER ERROR if the updater is null.
     */
    @GET
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    @Operation(operationId = "rootGet", summary = "Root page.", responses = {
            @ApiResponse(responseCode = "200", description = OK_SUCCESS),
            @ApiResponse(responseCode = "500", description = INT_SERV_ERR_UPDATER_NULL) })
    public Response rootGet() {
        BaseUpdater updater = this.updater;

        // return server error if updater is null
        if (updater == null) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(INT_SERV_ERR_UPDATER_NULL).build();
        }

        // return the HTML form
        return Response.status(Response.Status.OK).entity(HTML_ROOT_PAGE).type(MediaType.TEXT_HTML).build();
    }

    /**
     * This method handles HTTP GET on the updater resource's getStatus path.
     * It serves a JSON DTO containing the updater status.
     *
     * @return HTTP 200 OK, or HTTP 500 INTERNAL SERVER ERROR if the updater is null.
     */
    @GET
    @Path("/getStatus")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getStatusGet", summary = "Get the updater status.", responses = {
            @ApiResponse(responseCode = "200", description = OK_SUCCESS),
            @ApiResponse(responseCode = "500", description = INT_SERV_ERR_UPDATER_NULL) })
    public Response getStatusGet() {
        BaseUpdater updater = this.updater;

        // return server error if updater is null
        if (updater == null) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(INT_SERV_ERR_UPDATER_NULL).build();
        }

        // populate and return the DTO
        return Response.ok(updater.getStatusDTO()).build();
    }

    /**
     * This method handles HTTP GET on the updater resource's '/doUpdate' path.
     * It serves an HTML page containing an HTML FORM for submitting a request to the updater for starting the update
     * process.
     *
     * @return HTTP 200 OK, or HTTP 500 INTERNAL SERVER ERROR if the updater is null.
     */
    @GET
    @Path("/doUpdate")
    @Produces(MediaType.TEXT_HTML)
    @Operation(operationId = "doUpdateGET", summary = "Initiate update of OpenHAB.", responses = {
            @ApiResponse(responseCode = "200", description = OK_SUCCESS),
            @ApiResponse(responseCode = "500", description = INT_SERV_ERR_UPDATER_NULL) })
    public Response doUpdateGET() {
        BaseUpdater updater = this.updater;

        // return server error if updater is null
        if (updater == null) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(INT_SERV_ERR_UPDATER_NULL).build();
        }

        // construct the html form
        StatusDTO dto = updater.getStatusDTO();
        String html = HTML_DO_UPDATE_PAGE;
        html = html.replaceFirst(HTML_VER_NAME, dto.actualVersion.versionName);
        html = html.replaceFirst(HTML_VER_TYPE, dto.actualVersion.versionType);
        String newVerType = dto.targetNewVersionType;
        for (int i = 0; i < dto.latestVersionCount; i++) {
            html = html.replaceFirst(HTML_VER_NAME, dto.latestVersions[i].versionName);
            String verType = dto.latestVersions[i].versionType;
            html = html.replaceFirst(HTML_VER_TYPE, verType);
            html = html.replaceFirst(HTML_OPTION_FIELD,
                    String.format(HTML_OPTION_FIELD, newVerType.equals(verType) ? HTML_SELECTED : "", verType));
        }

        // serve the HTML form
        return Response.status(Response.Status.OK).entity(html).type(MediaType.TEXT_HTML).build();
    }

    /**
     * This method handles HTTP POST on the updater resource's '/doUpdate' path.
     * It specifically handles the case where the posted content is type 'APPLICATION_FORM_URLENCODED'.
     * It accepts the respective Form Parameters, and if they are OK starts the updater's update process.
     * It serves a PLAIN TEXT response indicating success or failure.
     *
     * @return HTTP 200 OK, HTTP 400 BAD REQUEST if the Form Parameters have errors, or HTTP 500 INTERNAL SERVER ERROR
     *         if the updater is null.
     */
    @POST
    // FOR RELEASE BUILD: un-comment the following line
    // @ RolesAllowed({ Role.ADMIN })
    @Path("/doUpdate")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(operationId = "doUpdatePostForm", summary = "Initiate update of OpenHAB.", responses = {
            @ApiResponse(responseCode = "200", description = OK_SUCCESS),
            @ApiResponse(responseCode = "400", description = BAD_REQUEST),
            @ApiResponse(responseCode = "500", description = INT_SERV_ERR_UPDATER_NULL) })
    public Response doUpdatePostForm(@FormParam("targetNewVersionType") @Nullable String targetNewVersionType,
            @FormParam("user") @Nullable String user, @FormParam("password") @Nullable String password) {
        BaseUpdater updater = this.updater;

        // return server error if updater is null
        if (updater == null) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(INT_SERV_ERR_UPDATER_NULL).build();
        }

        // check user name e.g. on Linux
        if (targetNewVersionType != null) {
            try {
                updater.setNewVersionType(targetNewVersionType);
            } catch (IllegalArgumentException e) {
                return Response.status(Status.BAD_REQUEST).entity(BAD_VERSION).build();
            }
        }

        // check user name
        if (user != null) {
            try {
                updater.setUserName(user);
            } catch (IllegalArgumentException e) {
                return Response.status(Status.BAD_REQUEST).entity(BAD_USER).build();
            }
        }

        // check password
        if (password != null) {
            try {
                updater.setPassword(password);
            } catch (IllegalArgumentException e) {
                return Response.status(Status.BAD_REQUEST).entity(BAD_PASSWORD).build();
            }
        }

        // submit the updater's run() method for execution
        UPDATE_EXECUTOR.submit(updater);
        return Response.ok(OK_UPDATE_STARTED).build();
    }

    /**
     * This method handles HTTP POST on the updater resource's '/doUpdate' path.
     * It specifically handles the case where the posted content is type 'APPLICATION_JSON'.
     * It accepts the respective JSON DTO, and if the parameters are OK starts the updater's update process.
     * It serves a PLAIN TEXT response indicating success or failure.
     *
     * @return HTTP 200 OK, HTTP 400 BAD REQUEST if the JSON DTO parameters have errors, or HTTP 500 INTERNAL SERVER
     *         ERROR if the updater is null.
     */
    @POST
    // FOR RELEASE BUILD: un-comment the following line
    // @ RolesAllowed({ Role.ADMIN })
    @Path("/doUpdate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(operationId = "doUpdatePostJson", summary = "Initiate update of OpenHAB.", responses = {
            @ApiResponse(responseCode = "200", description = OK_SUCCESS),
            @ApiResponse(responseCode = "400", description = BAD_REQUEST),
            @ApiResponse(responseCode = "500", description = INT_SERV_ERR_UPDATER_NULL) })
    public Response doUpdatePostJson(@Nullable DoUpdateDTO dto) {
        BaseUpdater updater = this.updater;

        // return server error if updater is null
        if (updater == null) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(INT_SERV_ERR_UPDATER_NULL).build();
        }

        // return bad request error if JSON DTO is null
        if (dto == null) {
            return Response.status(Status.BAD_REQUEST).entity(BAD_DTO).build();
        }

        // check targetNewVersionType
        if (dto.targetNewVersionType != null) {
            try {
                updater.setNewVersionType(dto.targetNewVersionType);
            } catch (IllegalArgumentException e) {
                return Response.status(Status.BAD_REQUEST).entity(BAD_VERSION).build();
            }
        }

        // check user name
        if (dto.user != null) {
            try {
                updater.setUserName(dto.user);
            } catch (IllegalArgumentException e) {
                return Response.status(Status.BAD_REQUEST).entity(BAD_USER).build();
            }
        }

        // check password
        if (dto.password != null) {
            try {
                updater.setPassword(dto.password);
            } catch (IllegalArgumentException e) {
                return Response.status(Status.BAD_REQUEST).entity(BAD_PASSWORD).build();
            }
        }

        // submit the updater's run() method for execution
        UPDATE_EXECUTOR.submit(updater);
        return Response.ok(OK_UPDATE_STARTED).build();
    }
}
