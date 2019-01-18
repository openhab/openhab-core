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
package org.eclipse.smarthome.ui.icon.internal;

import static org.eclipse.smarthome.ui.icon.internal.IconServlet.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.smarthome.ui.icon.IconProvider;
import org.eclipse.smarthome.ui.icon.IconSet.Format;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * Tests for {@link IconServlet}.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Wouter Born - Migrate tests from Groovy to Java and use Mockito
 */
public class IconServletTest {

    private class ByteArrayServletOutputStream extends ServletOutputStream {

        private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        @Override
        public void write(int b) throws IOException {
            outputStream.write(b);
        }

        @Override
        public void setWriteListener(WriteListener arg0) {
        }

        @Override
        public boolean isReady() {
            return true;
        }

        public String getOutput() {
            return new String(outputStream.toByteArray());
        }

        public void reset() {
            outputStream.reset();
        }

    };

    private IconServlet servlet;
    private ByteArrayServletOutputStream responseOutputStream = new ByteArrayServletOutputStream();

    private @Mock HttpServletRequest request;
    private @Mock HttpServletResponse response;

    private @Mock IconProvider provider1;
    private @Mock IconProvider provider2;

    public @Rule MockitoRule mockitoRule = MockitoJUnit.rule();

    @Before
    public void before() throws IOException {
        servlet = new IconServlet();
        responseOutputStream.reset();
    }

    @Test
    public void testOldUrlStyle() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/y-34.png");

        when(response.getOutputStream()).thenReturn(responseOutputStream);

        when(provider1.hasIcon("y", "classic", Format.PNG)).thenReturn(0);
        when(provider1.getIcon("y", "classic", "34", Format.PNG))
                .thenReturn(new ByteArrayInputStream("provider 1 icon: y classic 34 png".getBytes()));

        servlet.addIconProvider(provider1);
        servlet.doGet(request, response);

        assertEquals("provider 1 icon: y classic 34 png", responseOutputStream.getOutput());
        verify(response, never()).sendError(anyInt());
    }

    @Test
    public void testPriority() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/x");
        when(request.getParameter(PARAM_FORMAT)).thenReturn("svg");
        when(request.getParameter(PARAM_ICONSET)).thenReturn("test");
        when(request.getParameter(PARAM_STATE)).thenReturn("34");

        when(response.getOutputStream()).thenReturn(responseOutputStream);

        when(provider1.hasIcon("x", "test", Format.SVG)).thenReturn(0);
        when(provider1.getIcon("x", "test", "34", Format.SVG))
                .thenReturn(new ByteArrayInputStream("provider 1 icon: x test 34 svg".getBytes()));

        servlet.addIconProvider(provider1);
        servlet.doGet(request, response);

        assertEquals("provider 1 icon: x test 34 svg", responseOutputStream.getOutput());
        verify(response, never()).sendError(anyInt());

        responseOutputStream.reset();

        when(provider2.hasIcon("x", "test", Format.SVG)).thenReturn(1);
        when(provider2.getIcon("x", "test", "34", Format.SVG))
                .thenReturn(new ByteArrayInputStream("provider 2 icon: x test 34 svg".getBytes()));

        servlet.addIconProvider(provider2);
        servlet.doGet(request, response);

        assertEquals("provider 2 icon: x test 34 svg", responseOutputStream.getOutput());
        verify(response, never()).sendError(anyInt());
    }

    @Test
    public void testMissingIcon() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/icon/missing_for_test.png");

        when(provider1.hasIcon(anyString(), anyString(), isA(Format.class))).thenReturn(null);

        servlet.addIconProvider(provider1);
        servlet.doGet(request, response);

        assertEquals("", responseOutputStream.getOutput());
        verify(response).sendError(404);
    }

}
