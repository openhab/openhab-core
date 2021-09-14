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
package org.openhab.core.addon.sample.internal;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.openhab.core.addon.Addon;
import org.openhab.core.addon.AddonEventFactory;
import org.openhab.core.addon.AddonService;
import org.openhab.core.addon.AddonType;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * This is an implementation of an {@link AddonService} that can be used as a dummy service for testing the
 * functionality.
 * It is not meant to be used anywhere productively.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@Component
public class SampleAddonService implements AddonService {

    private static final String LOREM_IPSUM = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.";

    private static final String[] COLOR_VALUES = new String[] { "80", "C8", "FF" };

    private EventPublisher eventPublisher;

    List<AddonType> types = new ArrayList<>(3);
    Map<String, Addon> extensions = new HashMap<>(30);

    @Reference
    protected void setEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    protected void unsetEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = null;
    }

    @Activate
    protected void activate() {
        types.add(new AddonType("binding", "Bindings"));
        types.add(new AddonType("ui", "User Interfaces"));
        types.add(new AddonType("persistence", "Persistence Services"));

        for (AddonType type : types) {
            for (int i = 0; i < 10; i++) {
                String id = type.getId() + Integer.toString(i);
                boolean installed = Math.random() > 0.5;
                byte[] array = new byte[5];
                new Random().nextBytes(array);
                String name = new String(array, StandardCharsets.UTF_8);
                String typeId = type.getId();
                String label = name + " " + typeId.substring(0, 1).toUpperCase() + typeId.substring(1).toLowerCase();
                String version = "1.0";
                String link = (Math.random() < 0.5) ? null : "http://lmgtfy.com/?q=" + name;
                String description = createDescription();
                String imageLink = null;
                String backgroundColor = createRandomColor();
                Addon extension = new Addon(id, typeId, label, version, "example/vnd.openhab.addon", link, "John Doe",
                        false, installed, description, backgroundColor, imageLink, null, null, null, null, null, null);
                extensions.put(extension.getId(), extension);
            }
        }
    }

    private static final Random RANDOM = new Random();

    private String createRandomColor() {
        StringBuilder ret = new StringBuilder("#");
        for (int i = 0; i < 3; i++) {
            ret.append(COLOR_VALUES[RANDOM.nextInt(COLOR_VALUES.length)]);
        }
        return ret.toString();
    }

    private String createDescription() {
        int index = LOREM_IPSUM.indexOf(" ", RANDOM.nextInt(LOREM_IPSUM.length()));
        if (index < 0) {
            index = LOREM_IPSUM.length();
        }
        return LOREM_IPSUM.substring(0, index);
    }

    @Deactivate
    protected void deactivate() {
        types.clear();
        extensions.clear();
    }

    @Override
    public String getId() {
        return "sample";
    }

    @Override
    public String getName() {
        return "Sample Add-on Service";
    }

    @Override
    public void refreshSource() {
    }

    @Override
    public void install(String id) {
        try {
            Thread.sleep((long) (Math.random() * 10000));
            Addon extension = getAddon(id, null);
            extension.setInstalled(true);
            postInstalledEvent(id);
        } catch (InterruptedException e) {
        }
    }

    @Override
    public void uninstall(String id) {
        try {
            Thread.sleep((long) (Math.random() * 5000));
            Addon extension = getAddon(id, null);
            extension.setInstalled(false);
            postUninstalledEvent(id);
        } catch (InterruptedException e) {
        }
    }

    @Override
    public List<Addon> getAddons(Locale locale) {
        return new ArrayList<>(extensions.values());
    }

    @Override
    public Addon getAddon(String id, Locale locale) {
        return extensions.get(id);
    }

    @Override
    public List<AddonType> getTypes(Locale locale) {
        return types;
    }

    @Override
    public String getAddonId(URI extensionURI) {
        return null;
    }

    private void postInstalledEvent(String extensionId) {
        if (eventPublisher != null) {
            Event event = AddonEventFactory.createAddonInstalledEvent(extensionId);
            eventPublisher.post(event);
        }
    }

    private void postUninstalledEvent(String extensionId) {
        if (eventPublisher != null) {
            Event event = AddonEventFactory.createAddonUninstalledEvent(extensionId);
            eventPublisher.post(event);
        }
    }
}
