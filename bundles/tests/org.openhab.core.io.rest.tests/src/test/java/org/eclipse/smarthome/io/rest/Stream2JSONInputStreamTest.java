/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.io.rest;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Stream2JSONInputStreamTest {

    private Stream2JSONInputStream collection2InputStream;

    private final Gson GSON = new GsonBuilder().create();

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailForNullSource() throws IOException {
        new Stream2JSONInputStream(null).close();
    }

    @Test
    public void shouldReturnForEmptyStream() throws Exception {
        List<Object> emptyList = Collections.emptyList();
        collection2InputStream = new Stream2JSONInputStream(emptyList.stream());

        assertThat(inputStreamToString(collection2InputStream), is(GSON.toJson(emptyList)));
    }

    @Test
    public void shouldStreamSingleObjectToJSON() throws Exception {
        DummyObject dummyObject = new DummyObject("demoKey", "demoValue");
        List<DummyObject> dummyList = Arrays.asList(dummyObject);
        collection2InputStream = new Stream2JSONInputStream(Stream.of(dummyObject));

        assertThat(inputStreamToString(collection2InputStream), is(GSON.toJson(dummyList)));
    }

    @Test
    public void shouldStreamCollectionStreamToJSON() throws Exception {
        DummyObject dummyObject1 = new DummyObject("demoKey1", "demoValue1");
        DummyObject dummyObject2 = new DummyObject("demoKey2", "demoValue2");
        List<DummyObject> dummyCollection = Arrays.asList(dummyObject1, dummyObject2);
        collection2InputStream = new Stream2JSONInputStream(dummyCollection.stream());

        assertThat(inputStreamToString(collection2InputStream), is(GSON.toJson(dummyCollection)));
    }

    private String inputStreamToString(InputStream in) throws IOException {
        return IOUtils.toString(in);
    }

    @SuppressWarnings("unused")
    private class DummyObject {
        private final String key;
        private final String value;

        DummyObject(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
}
