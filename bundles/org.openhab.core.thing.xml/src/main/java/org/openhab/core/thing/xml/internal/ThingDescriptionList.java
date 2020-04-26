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
package org.openhab.core.thing.xml.internal;

import java.util.ArrayList;
import java.util.Collection;

/**
 * The {@link ThingDescriptionList} is the XML conversion result object which
 * is a list of {@link ThingTypeXmlResult}, {@link BridgeTypeXmlResult} and {@link ChannelTypeXmlResult} objects.
 *
 * @author Michael Grammling - Initial contribution
 */
@SuppressWarnings({ "serial", "rawtypes" })
public class ThingDescriptionList extends ArrayList {

    @SuppressWarnings("unchecked")
    public ThingDescriptionList(Collection list) {
        super(list);
    }
}
