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
package org.openhab.core.ui.internal.tiles;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Stream;

import org.openhab.core.ui.tiles.ExternalServiceTile;
import org.openhab.core.ui.tiles.Tile;
import org.openhab.core.ui.tiles.TileProvider;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This component registers the UI tiles.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Laurent Garnier - internationalization
 * @author Hilbrand Bouwkamp - internationalization
 * @author Yannick Schaus - refactor into tile service, remove dashboard components
 */
@Component(immediate = true, name = "org.openhab.core.ui.tiles")
public class TileService implements TileProvider {

    private final Logger logger = LoggerFactory.getLogger(TileService.class);

    protected ConfigurationAdmin configurationAdmin;

    protected Set<Tile> tiles = new CopyOnWriteArraySet<>();

    private static final String LINK_NAME = "link-name";
    private static final String LINK_URL = "link-url";
    private static final String LINK_IMAGEURL = "link-imageurl";

    @Activate
    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        addTilesForExternalServices(properties);
    }

    @Deactivate
    protected void deactivate(ComponentContext componentContext) {
    }

    @Reference
    protected void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    protected void unsetConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = null;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addTile(Tile tile) {
        tiles.add(tile);
    }

    protected void removeTile(Tile tile) {
        tiles.remove(tile);
    }

    @Override
    public Stream<Tile> getTiles() {
        return tiles.stream();
    }

    private void addTilesForExternalServices(Map<String, Object> properties) {
        for (String key : properties.keySet()) {
            if (key.endsWith(LINK_NAME)) {
                if (key.length() > LINK_NAME.length()) {
                    // get prefix from link name
                    String linkname = key.substring(0, key.length() - LINK_NAME.length());

                    String name = (String) properties.get(linkname + LINK_NAME);
                    String url = (String) properties.get(linkname + LINK_URL);
                    String imageUrl = (String) properties.get(linkname + LINK_IMAGEURL);

                    Tile newTile = new ExternalServiceTile.TileBuilder().withName(name).withUrl(url)
                            .withImageUrl(imageUrl).build();

                    if (name != null && url != null && !name.isEmpty() && !url.isEmpty()) {
                        addTile(newTile);
                        logger.debug("Tile added: {}", newTile);
                    } else {
                        logger.warn("Ignore invalid tile '{}': {}", linkname, newTile);
                    }
                }
            }
        }
    }
}
