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
package org.openhab.core.addon.marketplace.internal.community;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.openhab.core.addon.marketplace.internal.community.CommunityMarketplaceAddonService.YAML_CONTENT_PROPERTY;

import java.util.Calendar;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.addon.Addon;
import org.openhab.core.addon.marketplace.MarketplaceHandlerException;
import org.openhab.core.ui.components.RootUIComponent;
import org.openhab.core.ui.components.UIComponentRegistry;
import org.openhab.core.ui.components.UIComponentRegistryFactory;

/**
 * Tests for the legacy (non-versioned) YAML format handled by {@link CommunityUIWidgetAddonHandler}.
 *
 * @author Holger Friedrich - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
public class CommunityUIWidgetAddonHandlerTest {

    private static final String ADDON_UID = "marketplace:123456";

    private static final String LEGACY_WIDGET_YAML = """
            uid: my_widget
            tags:
              - tag1
            props:
              parameters:
                - name: title
                  type: TEXT
              parameterGroups: []
            timestamp: Mar 8, 2021, 07:41:03 PM
            component: f7-card
            config:
              title: =props.title
              zip: 09123
            """;

    private @Mock @NonNullByDefault({}) UIComponentRegistryFactory registryFactory;
    private @Mock @NonNullByDefault({}) UIComponentRegistry widgetRegistry;

    private @NonNullByDefault({}) CommunityUIWidgetAddonHandler handler;

    @BeforeEach
    public void setUp() {
        when(registryFactory.getRegistry("ui:widget")).thenReturn(widgetRegistry);
        handler = new CommunityUIWidgetAddonHandler(registryFactory);
    }

    @Test
    public void legacyYamlWidgetIsInstalledWithAllProperties() throws MarketplaceHandlerException {
        Addon addon = Addon.create(ADDON_UID).withType("ui").withId("123456")
                .withProperty(YAML_CONTENT_PROPERTY, LEGACY_WIDGET_YAML).build();

        handler.install(addon);

        ArgumentCaptor<RootUIComponent> captor = ArgumentCaptor.forClass(RootUIComponent.class);
        verify(widgetRegistry).add(captor.capture());
        RootUIComponent widget = captor.getValue();

        assertThat(widget.getUID(), is("my_widget"));
        assertThat(widget.getType(), is("f7-card"));
        assertThat(widget.getTags(), containsInAnyOrder("tag1", ADDON_UID));
        assertThat(widget.getConfig(), hasEntry("title", "=props.title"));
        assertThat(widget.getConfig(), hasEntry("zip", "09123"));
        assertThat(widget.getProps().parameters, hasSize(1));

        Calendar timestamp = Calendar.getInstance();
        timestamp.setTime(Objects.requireNonNull(widget.getTimestamp()));
        assertThat(timestamp.get(Calendar.YEAR), is(2021));
        assertThat(timestamp.get(Calendar.MONTH), is(Calendar.MARCH));
        assertThat(timestamp.get(Calendar.DAY_OF_MONTH), is(8));
    }
}
