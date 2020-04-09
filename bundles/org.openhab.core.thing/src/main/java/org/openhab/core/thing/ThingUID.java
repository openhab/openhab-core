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
package org.openhab.core.thing;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * {@link ThingUID} represents a unique identifier for things.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Jochen Hiller - Bugfix 455434: added default constructor
 */
@NonNullByDefault
public class ThingUID extends UID {

    private static final String NO_THING_TYPE = "";

    /**
     * Default constructor in package scope only. Will allow to instantiate this
     * class by reflection. Not intended to be used for normal instantiation.
     */
    ThingUID() {
        super();
    }

    /**
     * Instantiates a new thing UID.
     *
     * @param thingType the thing type
     * @param id the id
     */
    public ThingUID(ThingTypeUID thingTypeUID, String id) {
        super(thingTypeUID.getBindingId(), thingTypeUID.getId(), id);
    }

    /**
     * Instantiates a new thing UID.
     *
     * @param thingType the thing type
     * @param bridgeUID the bridge UID through which the thing is accessed
     * @param id the id of the thing
     */
    public ThingUID(ThingTypeUID thingTypeUID, ThingUID bridgeUID, String id) {
        super(getArray(thingTypeUID.getBindingId(), thingTypeUID.getId(), id, bridgeUID.getBridgeIds(),
                bridgeUID.getId()));
    }

    /**
     * Instantiates a new thing UID.
     *
     * @param thingType the thing type
     * @param id the id
     */
    public ThingUID(ThingTypeUID thingTypeUID, String id, String... bridgeIds) {
        super(getArray(thingTypeUID.getBindingId(), thingTypeUID.getId(), id, bridgeIds));
    }

    /**
     * Instantiates a new thing UID.
     *
     * @param bindingId the binding id
     * @param id the id
     */
    public ThingUID(String bindingId, String id) {
        super(bindingId, NO_THING_TYPE, id);
    }

    /**
     * Instantiates a new thing UID.
     *
     * @param bindingId the binding id
     * @param bridgeUID the bridge UID through which the thing is accessed
     * @param id the id
     */
    public ThingUID(String bindingId, ThingUID bridgeUID, String id) {
        super(getArray(bindingId, NO_THING_TYPE, id, bridgeUID.getBridgeIds(), bridgeUID.getId()));
    }

    private static String[] getArray(String bindingId, String thingTypeId, String id, @Nullable String... bridgeIds) {
        if (bridgeIds == null || bridgeIds.length == 0) {
            return new String[] { bindingId, thingTypeId, id };
        }

        String[] result = new String[3 + bridgeIds.length];
        result[0] = bindingId;
        result[1] = thingTypeId;
        System.arraycopy(bridgeIds, 0, result, 2, bridgeIds.length);
        result[result.length - 1] = id;
        return result;
    }

    private static String[] getArray(String bindingId, String thingTypeId, String id, List<String> bridgeIds,
            String bridgeId) {
        List<String> allBridgeIds = new ArrayList<>(bridgeIds);
        allBridgeIds.add(bridgeId);
        return getArray(bindingId, thingTypeId, id, allBridgeIds.toArray(new String[0]));
    }

    /**
     * Instantiates a new thing UID.
     *
     * @param bindingId the binding id
     * @param thingTypeId the thing type id
     * @param id the id
     */
    public ThingUID(String bindingId, String thingTypeId, String id) {
        super(bindingId, thingTypeId, id);
    }

    /**
     * Instantiates a new thing UID.
     *
     * @param thingUID the thing UID
     */
    public ThingUID(String thingUID) {
        super(thingUID);
    }

    /**
     * Instantiates a new thing UID.
     *
     * @param segments segments
     */
    public ThingUID(String... segments) {
        super(segments);
    }

    /**
     * Returns the bridge ids.
     *
     * @return list of bridge ids
     */
    public List<String> getBridgeIds() {
        List<String> allSegments = getAllSegments();
        return allSegments.subList(2, allSegments.size() - 1);
    }

    /**
     * Returns the id.
     *
     * @return id the id
     */
    public String getId() {
        List<String> segments = getAllSegments();
        return segments.get(segments.size() - 1);
    }

    @Override
    protected int getMinimalNumberOfSegments() {
        return 3;
    }
}
