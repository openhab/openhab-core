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
package org.eclipse.smarthome.io.json.gson;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

import java.io.StringReader;
import java.io.StringWriter;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openhab.core.io.json.JsonBindingService;

/**
 * Test class for the Gson implementation of {@link JsonBindingService}. It tests basic data binding and Gson type
 * adapters.
 *
 * @author Flavio Costa - Initial implementation
 */
public class GsonBindingServiceTest extends JavaOSGiTest {

    /**
     * OSGi service being tested.
     */
    private @Nullable JsonBindingService<GsonBindingServiceTestBean> bindingService;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        registerVolatileStorageService();
        bindingService = getService(JsonBindingService.class);
    }

    @After
    public void teardown() {
        bindingService = null;
    }

    @Test
    public void assertThatSerializationWorks() {
        assertThat(bindingService, is(not(nullValue())));
        GsonBindingServiceTestBean bean = new GsonBindingServiceTestBean();
        bean.status = GsonBindingServiceTestBean.Active.YES;

        StringWriter writer = new StringWriter();
        if (bindingService != null) {
            bindingService.toJson(bean, bean.getClass(), writer);
            assertThat(writer.toString(), containsString("status"));
            assertThat(writer.toString(), containsString("active"));
        }
    }

    @Test
    public void assertThatDeserializationWorks() {
        assertThat(bindingService, is(not(nullValue())));
        StringReader reader = new StringReader("{\"status\": \"inactive\", \"text\": \"It works\"}");
        if (bindingService != null) {
            GsonBindingServiceTestBean bean = bindingService.fromJson(reader, GsonBindingServiceTestBean.class);
            assertThat(bean, is(not(nullValue())));
            assertThat(bean.status, is(GsonBindingServiceTestBean.Active.NO));
            assertThat(bean.getText(), is(not(nullValue())));
        }
    }
}
