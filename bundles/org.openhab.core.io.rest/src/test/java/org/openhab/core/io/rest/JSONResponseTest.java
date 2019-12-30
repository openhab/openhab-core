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
package org.openhab.core.io.rest;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.object.IsCompatibleType.typeCompatibleWith;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.google.gson.JsonObject;

/**
 * Tests {@link JSONResponse}.
 *
 * @author Henning Treu - Initial contribution
 */
public class JSONResponseTest {

    private static final String ENTITY_VALUE = "entityValue";
    private static final String ENTITY_JSON_VALUE = "\"" + ENTITY_VALUE + "\"";

    @Test
    public void creatErrorShouldCreateErrorResponse() {
        Response errorResponse = JSONResponse.createErrorResponse(Status.INTERNAL_SERVER_ERROR, "error");

        assertThat(errorResponse.getMediaType(), is(equalTo(MediaType.APPLICATION_JSON_TYPE)));
        assertThat(errorResponse.getStatus(), is(500));

        JsonObject entity = ((JsonObject) errorResponse.getEntity()).get(JSONResponse.JSON_KEY_ERROR).getAsJsonObject();
        assertThat(entity.get(JSONResponse.JSON_KEY_ERROR_MESSAGE).getAsString(), is("error"));
        assertThat(entity.get(JSONResponse.JSON_KEY_HTTPCODE).getAsInt(), is(500));
    }

    @Test
    public void createMessageWithErrorStatusShouldCreateErrorResponse() {
        Response errorResponse = JSONResponse.createResponse(Status.INTERNAL_SERVER_ERROR, null, "error");

        assertThat(errorResponse.getMediaType(), is(equalTo(MediaType.APPLICATION_JSON_TYPE)));
        assertThat(errorResponse.getStatus(), is(500));

        JsonObject entity = ((JsonObject) errorResponse.getEntity()).get(JSONResponse.JSON_KEY_ERROR).getAsJsonObject();
        assertThat(entity.get(JSONResponse.JSON_KEY_ERROR_MESSAGE).getAsString(), is("error"));
        assertThat(entity.get(JSONResponse.JSON_KEY_HTTPCODE).getAsInt(), is(500));
        assertThat(entity.get(JSONResponse.JSON_KEY_ENTITY), is(nullValue()));
    }

    @Test
    public void createMessageWithErrorStatusShouldCreateErrorResponseWithEntity() {
        Response errorResponse = JSONResponse.createResponse(Status.INTERNAL_SERVER_ERROR, ENTITY_VALUE, "error");

        assertThat(errorResponse.getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
        assertThat(errorResponse.getStatus(), is(500));

        JsonObject resultJson = (JsonObject) errorResponse.getEntity();
        assertThat(resultJson.get(JSONResponse.JSON_KEY_ENTITY).getAsString(), is(ENTITY_VALUE));

        JsonObject errorJson = resultJson.get(JSONResponse.JSON_KEY_ERROR).getAsJsonObject();
        assertThat(errorJson.get(JSONResponse.JSON_KEY_ERROR_MESSAGE).getAsString(), is("error"));
        assertThat(errorJson.get(JSONResponse.JSON_KEY_HTTPCODE).getAsInt(), is(500));
    }

    @Test
    public void shouldCreateSuccessResponseWithStreamEntity() throws IOException {
        Response response = JSONResponse.createResponse(Status.OK, ENTITY_VALUE, null);

        assertThat(response.getStatus(), is(200));
        assertThat(response.getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));

        Object entity = response.getEntity();
        assertThat(entity.getClass(), is(typeCompatibleWith(InputStream.class)));

        try (InputStream entityInStream = (InputStream) entity) {
            byte[] entityValue = new byte[ENTITY_JSON_VALUE.length()];
            entityInStream.read(entityValue);
            assertThat(new String(entityValue), is(ENTITY_JSON_VALUE));
        }
    }

    @Test
    public void shouldCreateSuccessResponseWithNullEntity() throws Exception {
        Response response = JSONResponse.createResponse(Status.ACCEPTED, null, null);

        assertThat(response.getStatus(), is(202));
        assertThat(response.getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));

        Object entity = response.getEntity();
        assertThat(entity, is(nullValue()));
    }

    @Test
    public void shouldCreateSuccessResponseWithLargeStreamEntity() throws IOException {
        Response response = JSONResponse.createResponse(Status.OK, new LargeEntity(), null);

        assertThat(response.getStatus(), is(200));
        assertThat(response.getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));

        Object entity = response.getEntity();
        assertThat(entity.getClass(), is(typeCompatibleWith(InputStream.class)));

        try (InputStream entityInStream = (InputStream) entity) {
            String largeEntityJSON = IOUtils.toString(entityInStream);
            assertThat(largeEntityJSON, is(notNullValue()));
            assertTrue(largeEntityJSON.startsWith("{"));
            assertTrue(largeEntityJSON.endsWith("}"));
        }
    }

    @SuppressWarnings("unused")
    private final class LargeEntity {

        private List<BigDecimal> randoms = getRandoms();

        private List<BigDecimal> getRandoms() {
            List<BigDecimal> randoms = new ArrayList<>();
            for (int i = 0; i < 100000; i++) {
                randoms.add(new BigDecimal(Math.random()));
            }

            return randoms;
        }
    }
}
