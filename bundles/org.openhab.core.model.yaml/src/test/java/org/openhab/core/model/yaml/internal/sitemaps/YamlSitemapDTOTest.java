/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.model.yaml.internal.sitemaps;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * The {@link YamlSitemapDTOTest} contains tests for the {@link YamlSitemapDTO} class.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class YamlSitemapDTOTest {

    @Test
    public void testIsValid() throws IOException {
        List<String> err = new ArrayList<>();
        List<String> warn = new ArrayList<>();

        YamlWidgetDTO widgetText = new YamlWidgetDTO();
        widgetText.type = "Text";
        YamlWidgetDTO widgetImage = new YamlWidgetDTO();
        widgetImage.type = "Image";
        YamlWidgetDTO widgetFrame = new YamlWidgetDTO();
        widgetFrame.type = "Frame";
        YamlWidgetDTO widgetButton = new YamlWidgetDTO();
        widgetButton.type = "Button";
        widgetButton.item = "switchItem";
        widgetButton.row = 1;
        widgetButton.column = 1;
        widgetButton.command = "ON";

        YamlSitemapDTO sitemap = new YamlSitemapDTO();
        assertFalse(sitemap.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid sitemap: name missing while mandatory", err.getFirst());
        err.clear();
        sitemap.name = " ";
        assertFalse(sitemap.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals("invalid sitemap: name missing while mandatory", err.getFirst());
        err.clear();
        sitemap.name = "demo-sitemap";
        assertFalse(sitemap.isValid(err, warn));
        assertEquals(1, err.size());
        assertEquals(
                "invalid sitemap \"%s\": name must contain alphanumeric characters and underscores, and must not contain any other symbols."
                        .formatted(sitemap.name),
                err.getFirst());
        err.clear();
        sitemap.name = "demo";
        assertTrue(sitemap.isValid(err, warn));

        sitemap.widgets = List.of();
        assertTrue(sitemap.isValid(err, warn));
        sitemap.widgets = List.of(widgetText);
        assertTrue(sitemap.isValid(err, warn));
        sitemap.widgets = List.of(widgetText, widgetImage);
        assertTrue(sitemap.isValid(err, warn));
        assertEquals(0, warn.size());
        sitemap.widgets = List.of(widgetText, widgetFrame);
        assertTrue(sitemap.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("sitemap \"%s\": should contain either only Frames or none at all".formatted(sitemap.name),
                warn.getFirst());
        warn.clear();
        sitemap.widgets = List.of(widgetButton);
        assertTrue(sitemap.isValid(err, warn));
        assertEquals(1, warn.size());
        assertEquals("sitemap \"%s\": should not contain Button, Buttons are only allowed in Buttongrid"
                .formatted(sitemap.name), warn.getFirst());
        warn.clear();
        sitemap.widgets = List.of(widgetFrame);
        assertTrue(sitemap.isValid(err, warn));
        assertEquals(0, warn.size());

        sitemap.label = "Demo Sitemap";
        assertTrue(sitemap.isValid(err, warn));

        sitemap.icon = "icon";
        assertTrue(sitemap.isValid(err, warn));
        assertEquals(0, warn.size());
    }

    @Test
    public void testEquals() throws IOException {
        YamlSitemapDTO sitemap1 = new YamlSitemapDTO();
        YamlSitemapDTO sitemap2 = new YamlSitemapDTO();

        sitemap1.name = "demo";
        sitemap2.name = "demo2";
        assertFalse(sitemap1.equals(sitemap2));
        sitemap2.name = "demo";
        assertTrue(sitemap1.equals(sitemap2));
        assertEquals(sitemap1.hashCode(), sitemap2.hashCode());

        YamlWidgetDTO widget1 = new YamlWidgetDTO();
        widget1.type = "Text";
        YamlWidgetDTO widget2 = new YamlWidgetDTO();
        widget2.type = "Image";
        YamlWidgetDTO widget3 = new YamlWidgetDTO();
        widget3.type = "Text";
        YamlWidgetDTO widget4 = new YamlWidgetDTO();
        widget4.type = "Image";

        sitemap1.widgets = List.of();
        sitemap2.widgets = List.of();
        assertTrue(sitemap1.equals(sitemap2));
        assertEquals(sitemap1.hashCode(), sitemap2.hashCode());

        sitemap1.widgets = List.of(widget1, widget2);
        sitemap2.widgets = null;
        assertFalse(sitemap1.equals(sitemap2));
        sitemap2.widgets = List.of();
        assertFalse(sitemap1.equals(sitemap2));
        sitemap2.widgets = List.of(widget3);
        assertFalse(sitemap1.equals(sitemap2));
        sitemap2.widgets = List.of(widget4, widget3);
        assertFalse(sitemap1.equals(sitemap2));
        sitemap2.widgets = List.of(widget3, widget4);
        assertTrue(sitemap1.equals(sitemap2));
        assertEquals(sitemap1.hashCode(), sitemap2.hashCode());

        sitemap1.label = "Demo Sitemap";
        assertFalse(sitemap1.equals(sitemap2));
        sitemap2.label = "Demo";
        assertFalse(sitemap1.equals(sitemap2));
        sitemap2.label = "Demo Sitemap";
        assertTrue(sitemap1.equals(sitemap2));
        assertEquals(sitemap1.hashCode(), sitemap2.hashCode());

        sitemap1.icon = "icon";
        assertFalse(sitemap1.equals(sitemap2));
        sitemap2.icon = "icon2";
        assertFalse(sitemap1.equals(sitemap2));
        sitemap2.icon = "icon";
        assertTrue(sitemap1.equals(sitemap2));
        assertEquals(sitemap1.hashCode(), sitemap2.hashCode());
    }
}
