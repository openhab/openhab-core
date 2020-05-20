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
package org.openhab.core.ui.icon.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.openhab.core.ui.icon.internal.IconServlet.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.openhab.core.ui.icon.IconProvider;
import org.openhab.core.ui.icon.IconSet.Format;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

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

    private @Mock HttpContext httpContextMock;
    private @Mock HttpService httpServiceMock;

    private @Mock HttpServletRequest requestMock;
    private @Mock HttpServletResponse responseMock;

    private @Mock IconProvider provider1Mock;
    private @Mock IconProvider provider2Mock;

    public @Rule MockitoRule mockitoRule = MockitoJUnit.rule();

    @Before
    public void before() throws IOException {
        servlet = new IconServlet(httpServiceMock, httpContextMock);
        responseOutputStream.reset();
    }

    @Test
    public void testOldUrlStyle() throws ServletException, IOException {
        when(requestMock.getRequestURI()).thenReturn("/y-34.png");

        when(responseMock.getOutputStream()).thenReturn(responseOutputStream);

        when(provider1Mock.hasIcon("y", "classic", Format.PNG)).thenReturn(0);
        when(provider1Mock.getIcon("y", "classic", "34", Format.PNG))
                .thenReturn(new ByteArrayInputStream("provider 1 icon: y classic 34 png".getBytes()));

        servlet.addIconProvider(provider1Mock);
        servlet.doGet(requestMock, responseMock);

        assertEquals("provider 1 icon: y classic 34 png", responseOutputStream.getOutput());
        verify(responseMock, never()).sendError(anyInt());
    }

    @Test
    public void testPriority() throws ServletException, IOException {
        when(requestMock.getRequestURI()).thenReturn("/x");
        when(requestMock.getParameter(PARAM_FORMAT)).thenReturn("svg");
        when(requestMock.getParameter(PARAM_ICONSET)).thenReturn("test");
        when(requestMock.getParameter(PARAM_STATE)).thenReturn("34");

        when(responseMock.getOutputStream()).thenReturn(responseOutputStream);

        when(provider1Mock.hasIcon("x", "test", Format.SVG)).thenReturn(0);
        when(provider1Mock.getIcon("x", "test", "34", Format.SVG))
                .thenReturn(new ByteArrayInputStream("provider 1 icon: x test 34 svg".getBytes()));

        servlet.addIconProvider(provider1Mock);
        servlet.doGet(requestMock, responseMock);

        assertEquals("provider 1 icon: x test 34 svg", responseOutputStream.getOutput());
        verify(responseMock, never()).sendError(anyInt());

        responseOutputStream.reset();

        when(provider2Mock.hasIcon("x", "test", Format.SVG)).thenReturn(1);
        when(provider2Mock.getIcon("x", "test", "34", Format.SVG))
                .thenReturn(new ByteArrayInputStream("provider 2 icon: x test 34 svg".getBytes()));

        servlet.addIconProvider(provider2Mock);
        servlet.doGet(requestMock, responseMock);

        assertEquals("provider 2 icon: x test 34 svg", responseOutputStream.getOutput());
        verify(responseMock, never()).sendError(anyInt());
    }

    @Test
    public void testMissingIcon() throws ServletException, IOException {
        when(requestMock.getRequestURI()).thenReturn("/icon/missing_for_test.png");

        when(provider1Mock.hasIcon(anyString(), anyString(), isA(Format.class))).thenReturn(null);

        servlet.addIconProvider(provider1Mock);
        servlet.doGet(requestMock, responseMock);

        assertEquals("", responseOutputStream.getOutput());
        verify(responseMock).sendError(404);
    }

    @Test
    public void testAnyFormatFalse() throws ServletException, IOException {
        when(requestMock.getRequestURI()).thenReturn("/z");
        when(requestMock.getParameter(PARAM_FORMAT)).thenReturn("svg");
        when(requestMock.getParameter(PARAM_ANY_FORMAT)).thenReturn("false");
        when(requestMock.getParameter(PARAM_ICONSET)).thenReturn("test");
        when(requestMock.getParameter(PARAM_STATE)).thenReturn("34");

        when(responseMock.getOutputStream()).thenReturn(responseOutputStream);

        when(provider1Mock.hasIcon("z", "test", Format.SVG)).thenReturn(0);
        when(provider1Mock.getIcon("z", "test", "34", Format.SVG))
                .thenReturn(new ByteArrayInputStream("provider 1 icon: z test 34 svg".getBytes()));

        servlet.addIconProvider(provider1Mock);
        servlet.doGet(requestMock, responseMock);

        assertEquals("provider 1 icon: z test 34 svg", responseOutputStream.getOutput());
        verify(responseMock, never()).sendError(anyInt());
        verify(provider1Mock, never()).hasIcon("z", "test", Format.PNG);
    }

    @Test
    public void testAnyFormatSameProviders() throws ServletException, IOException {
        when(requestMock.getRequestURI()).thenReturn("/z");
        when(requestMock.getParameter(PARAM_FORMAT)).thenReturn("svg");
        when(requestMock.getParameter(PARAM_ANY_FORMAT)).thenReturn("true");
        when(requestMock.getParameter(PARAM_ICONSET)).thenReturn("test");
        when(requestMock.getParameter(PARAM_STATE)).thenReturn("34");

        when(responseMock.getOutputStream()).thenReturn(responseOutputStream);

        when(provider1Mock.hasIcon("z", "test", Format.PNG)).thenReturn(0);
        when(provider1Mock.hasIcon("z", "test", Format.SVG)).thenReturn(0);
        when(provider1Mock.getIcon("z", "test", "34", Format.SVG))
                .thenReturn(new ByteArrayInputStream("provider 1 icon: z test 34 svg".getBytes()));

        servlet.addIconProvider(provider1Mock);
        servlet.doGet(requestMock, responseMock);

        assertEquals("provider 1 icon: z test 34 svg", responseOutputStream.getOutput());
        verify(responseMock, never()).sendError(anyInt());
        verify(provider1Mock, atLeastOnce()).hasIcon("z", "test", Format.PNG);
        verify(provider1Mock, atLeastOnce()).hasIcon("z", "test", Format.SVG);
    }

    @Test
    public void testAnyFormatHigherPriorityOtherFormat() throws ServletException, IOException {
        when(requestMock.getRequestURI()).thenReturn("/z");
        when(requestMock.getParameter(PARAM_FORMAT)).thenReturn("svg");
        when(requestMock.getParameter(PARAM_ANY_FORMAT)).thenReturn("true");
        when(requestMock.getParameter(PARAM_ICONSET)).thenReturn("test");
        when(requestMock.getParameter(PARAM_STATE)).thenReturn("34");

        when(responseMock.getOutputStream()).thenReturn(responseOutputStream);

        when(provider1Mock.hasIcon("z", "test", Format.PNG)).thenReturn(0);
        when(provider1Mock.hasIcon("z", "test", Format.SVG)).thenReturn(0);

        when(provider2Mock.hasIcon("z", "test", Format.PNG)).thenReturn(1);
        when(provider2Mock.hasIcon("z", "test", Format.SVG)).thenReturn(null);
        when(provider2Mock.getIcon("z", "test", "34", Format.PNG))
                .thenReturn(new ByteArrayInputStream("provider 2 icon: z test 34 png".getBytes()));

        servlet.addIconProvider(provider1Mock);
        servlet.addIconProvider(provider2Mock);
        servlet.doGet(requestMock, responseMock);

        assertEquals("provider 2 icon: z test 34 png", responseOutputStream.getOutput());
        verify(responseMock, never()).sendError(anyInt());
        verify(provider1Mock, atLeastOnce()).hasIcon("z", "test", Format.PNG);
        verify(provider1Mock, atLeastOnce()).hasIcon("z", "test", Format.SVG);
        verify(provider2Mock, atLeastOnce()).hasIcon("z", "test", Format.PNG);
        verify(provider2Mock, atLeastOnce()).hasIcon("z", "test", Format.SVG);
    }

    @Test
    public void testAnyFormatHigherPriorityRequestedFormat() throws ServletException, IOException {
        when(requestMock.getRequestURI()).thenReturn("/z");
        when(requestMock.getParameter(PARAM_FORMAT)).thenReturn("svg");
        when(requestMock.getParameter(PARAM_ANY_FORMAT)).thenReturn("true");
        when(requestMock.getParameter(PARAM_ICONSET)).thenReturn("test");
        when(requestMock.getParameter(PARAM_STATE)).thenReturn("34");

        when(responseMock.getOutputStream()).thenReturn(responseOutputStream);

        when(provider1Mock.hasIcon("z", "test", Format.PNG)).thenReturn(0);
        when(provider1Mock.hasIcon("z", "test", Format.SVG)).thenReturn(0);

        when(provider2Mock.hasIcon("z", "test", Format.PNG)).thenReturn(null);
        when(provider2Mock.hasIcon("z", "test", Format.SVG)).thenReturn(1);
        when(provider2Mock.getIcon("z", "test", "34", Format.SVG))
                .thenReturn(new ByteArrayInputStream("provider 2 icon: z test 34 svg".getBytes()));

        servlet.addIconProvider(provider1Mock);
        servlet.addIconProvider(provider2Mock);
        servlet.doGet(requestMock, responseMock);

        assertEquals("provider 2 icon: z test 34 svg", responseOutputStream.getOutput());
        verify(responseMock, never()).sendError(anyInt());
        verify(provider1Mock, atLeastOnce()).hasIcon("z", "test", Format.PNG);
        verify(provider1Mock, atLeastOnce()).hasIcon("z", "test", Format.SVG);
        verify(provider2Mock, atLeastOnce()).hasIcon("z", "test", Format.PNG);
        verify(provider2Mock, atLeastOnce()).hasIcon("z", "test", Format.SVG);
    }

    @Test
    public void testAnyFormatNoOtherFormat() throws ServletException, IOException {
        when(requestMock.getRequestURI()).thenReturn("/z");
        when(requestMock.getParameter(PARAM_FORMAT)).thenReturn("svg");
        when(requestMock.getParameter(PARAM_ANY_FORMAT)).thenReturn("true");
        when(requestMock.getParameter(PARAM_ICONSET)).thenReturn("test");
        when(requestMock.getParameter(PARAM_STATE)).thenReturn("34");

        when(responseMock.getOutputStream()).thenReturn(responseOutputStream);

        when(provider1Mock.hasIcon("z", "test", Format.PNG)).thenReturn(null);
        when(provider1Mock.hasIcon("z", "test", Format.SVG)).thenReturn(0);
        when(provider1Mock.getIcon("z", "test", "34", Format.SVG))
                .thenReturn(new ByteArrayInputStream("provider 1 icon: z test 34 svg".getBytes()));

        servlet.addIconProvider(provider1Mock);
        servlet.doGet(requestMock, responseMock);

        assertEquals("provider 1 icon: z test 34 svg", responseOutputStream.getOutput());
        verify(responseMock, never()).sendError(anyInt());
        verify(provider1Mock, atLeastOnce()).hasIcon("z", "test", Format.PNG);
        verify(provider1Mock, atLeastOnce()).hasIcon("z", "test", Format.SVG);
    }

    @Test
    public void testAnyFormatNoRequestedFormat() throws ServletException, IOException {
        when(requestMock.getRequestURI()).thenReturn("/z");
        when(requestMock.getParameter(PARAM_FORMAT)).thenReturn("svg");
        when(requestMock.getParameter(PARAM_ANY_FORMAT)).thenReturn("true");
        when(requestMock.getParameter(PARAM_ICONSET)).thenReturn("test");
        when(requestMock.getParameter(PARAM_STATE)).thenReturn("34");

        when(responseMock.getOutputStream()).thenReturn(responseOutputStream);

        when(provider1Mock.hasIcon("z", "test", Format.PNG)).thenReturn(0);
        when(provider1Mock.hasIcon("z", "test", Format.SVG)).thenReturn(null);
        when(provider1Mock.getIcon("z", "test", "34", Format.PNG))
                .thenReturn(new ByteArrayInputStream("provider 1 icon: z test 34 png".getBytes()));

        servlet.addIconProvider(provider1Mock);
        servlet.doGet(requestMock, responseMock);

        assertEquals("provider 1 icon: z test 34 png", responseOutputStream.getOutput());
        verify(responseMock, never()).sendError(anyInt());
        verify(provider1Mock, atLeastOnce()).hasIcon("z", "test", Format.PNG);
        verify(provider1Mock, atLeastOnce()).hasIcon("z", "test", Format.SVG);
    }
}
