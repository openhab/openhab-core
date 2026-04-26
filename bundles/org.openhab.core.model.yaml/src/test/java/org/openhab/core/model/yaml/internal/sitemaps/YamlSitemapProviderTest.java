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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.common.registry.Provider;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.model.yaml.YamlModelUtils;
import org.openhab.core.model.yaml.internal.YamlModelRepositoryImpl;
import org.openhab.core.service.WatchService;
import org.openhab.core.sitemap.LinkableWidget;
import org.openhab.core.sitemap.Sitemap;
import org.openhab.core.sitemap.Widget;
import org.openhab.core.sitemap.internal.FrameImpl;
import org.openhab.core.sitemap.internal.GroupImpl;
import org.openhab.core.sitemap.internal.SitemapImpl;
import org.openhab.core.sitemap.internal.TextImpl;
import org.openhab.core.sitemap.registry.SitemapFactory;

/**
 * The {@link YamlSitemapProviderTest} contains tests for the {@link YamlSitemapProvider} class.
 *
 * @author Laurent Garnier - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class YamlSitemapProviderTest {
    private static final Path SOURCE_PATH = Path.of("src/test/resources/model/sitemaps");
    private static final String MODEL_NAME = "model.yaml";
    private static final Path MODEL_PATH = Path.of(MODEL_NAME);

    private @Mock @NonNullByDefault({}) WatchService watchServiceMock;
    private @TempDir @NonNullByDefault({}) Path watchPath;
    private @NonNullByDefault({}) Path fullModelPath;

    private @Mock @NonNullByDefault({}) SitemapFactory sitemapFactory;

    private @NonNullByDefault({}) YamlModelRepositoryImpl modelRepository;
    private @NonNullByDefault({}) YamlSitemapProvider sitemapProvider;
    private @NonNullByDefault({}) TestSitemapChangeListener sitemapListener;

    @BeforeEach
    public void setup() {
        fullModelPath = watchPath.resolve(MODEL_PATH);
        when(watchServiceMock.getWatchPath()).thenReturn(watchPath);

        when(sitemapFactory.createSitemap(anyString())).thenAnswer(i -> {
            return new SitemapImpl(i.getArgument(0));
        });
        when(sitemapFactory.createWidget(anyString(), any())).thenAnswer(i -> {
            return switch ((String) i.getArgument(0)) {
                case "Frame" -> {
                    yield new FrameImpl(i.getArgument(1));
                }
                case "Text" -> {
                    yield new TextImpl(i.getArgument(1));
                }
                case "Group" -> {
                    yield new GroupImpl(i.getArgument(1));
                }
                default -> {
                    yield null;
                }
            };
        });

        sitemapProvider = new YamlSitemapProvider(sitemapFactory);

        sitemapListener = new TestSitemapChangeListener();
        sitemapProvider.addProviderChangeListener(sitemapListener);

        modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(sitemapProvider);
    }

    @Test
    public void testLoadModelWithSitemap() throws IOException {
        Files.copy(SOURCE_PATH.resolve("sitemap.yaml"), fullModelPath);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);

        assertFalse(YamlModelUtils.isIsolatedModel(MODEL_NAME));
        assertThat(sitemapListener.sitemaps, is(aMapWithSize(1)));
        assertThat(sitemapListener.sitemaps, hasKey("demo"));
        assertThat(sitemapProvider.getAllFromModel(MODEL_NAME), hasSize(1));
        Collection<Sitemap> sitemaps = sitemapProvider.getAll();
        assertThat(sitemaps, hasSize(1));
        Sitemap sitemap = sitemaps.iterator().next();
        assertEquals("demo", sitemap.getName());
        assertEquals("Demo Sitemap", sitemap.getLabel());
        assertNull(sitemap.getIcon());
        List<Widget> widgets = sitemap.getWidgets();
        assertThat(widgets, hasSize(1));
        Widget widget = widgets.getFirst();
        assertEquals("Frame", widget.getWidgetType());
        assertEquals("Demo Items", widget.getLabel());
        assertTrue(widget instanceof LinkableWidget);
        widgets = ((LinkableWidget) widget).getWidgets();
        assertThat(widgets, hasSize(2));
        widget = widgets.getFirst();
        assertEquals("Text", widget.getWidgetType());
        assertEquals("DemoContact", widget.getItem());
        assertEquals("Contact [MAP(en.map):%s]", widget.getLabel());
        widget = widgets.get(1);
        assertEquals("Group", widget.getWidgetType());
        assertEquals("DemoSwitchGroup", widget.getItem());
    }

    @Test
    public void testCreateIsolatedModelWithSitemap() throws IOException {
        Files.copy(SOURCE_PATH.resolve("sitemap.yaml"), fullModelPath);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        try (FileInputStream inputStream = new FileInputStream(fullModelPath.toFile())) {
            String name = modelRepository.createIsolatedModel(inputStream, errors, warnings);
            assertNotNull(name);
            assertEquals(0, errors.size());
            assertEquals(0, warnings.size());

            assertTrue(YamlModelUtils.isIsolatedModel(name));
            assertThat(sitemapListener.sitemaps, is(aMapWithSize(0)));
            assertThat(sitemapProvider.getAll(), hasSize(0)); // No sitemap for the registry
            Collection<Sitemap> sitemaps = sitemapProvider.getAllFromModel(name);
            assertThat(sitemaps, hasSize(1));
            Sitemap sitemap = sitemaps.iterator().next();
            assertEquals("demo", sitemap.getName());
            assertEquals("Demo Sitemap", sitemap.getLabel());
            assertNull(sitemap.getIcon());
            List<Widget> widgets = sitemap.getWidgets();
            assertThat(widgets, hasSize(1));
            Widget widget = widgets.getFirst();
            assertEquals("Frame", widget.getWidgetType());
            assertEquals("Demo Items", widget.getLabel());
            assertTrue(widget instanceof LinkableWidget);
            widgets = ((LinkableWidget) widget).getWidgets();
            assertThat(widgets, hasSize(2));
            widget = widgets.getFirst();
            assertEquals("Text", widget.getWidgetType());
            assertEquals("DemoContact", widget.getItem());
            assertEquals("Contact [MAP(en.map):%s]", widget.getLabel());
            widget = widgets.get(1);
            assertEquals("Group", widget.getWidgetType());
            assertEquals("DemoSwitchGroup", widget.getItem());
        }
    }

    private static class TestSitemapChangeListener implements ProviderChangeListener<Sitemap> {

        public final Map<String, Sitemap> sitemaps = new HashMap<>();

        @Override
        public void added(Provider<Sitemap> provider, Sitemap element) {
            sitemaps.put(element.getName(), element);
        }

        @Override
        public void removed(Provider<Sitemap> provider, Sitemap element) {
            sitemaps.remove(element.getName());
        }

        @Override
        public void updated(Provider<Sitemap> provider, Sitemap oldelement, Sitemap element) {
            sitemaps.put(element.getName(), element);
        }
    }
}
