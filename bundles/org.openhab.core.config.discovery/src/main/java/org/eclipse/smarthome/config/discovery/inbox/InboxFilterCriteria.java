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
package org.eclipse.smarthome.config.discovery.inbox;

import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultFlag;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;

/**
 * The {@link InboxFilterCriteria} specifies the filter for {@link Inbox} <i>GET</i> requests.
 * <p>
 * The according property is filtered in the {@link Inbox} if it's <i>NEITHER</i> {@code null} <i>NOR</i> empty. All
 * specified properties are filtered with an <i>AND</i> operator.
 *
 * @author Michael Grammling - Initial Contribution
 *
 * @see Inbox
 *
 * @deprecated use {@link InboxPredicates} to filter on streams of {@link DiscoveryResult}s
 */
@Deprecated
public final class InboxFilterCriteria {

    private final String bindingId;
    private final ThingTypeUID thingTypeUID;
    private final ThingUID thingUID;
    private final DiscoveryResultFlag flag;

    /**
     * Creates a new instance of this class with the specified parameters.
     *
     * @param bindingId the binding ID to be filtered (could be null or empty)
     * @param flag the discovery result flag to be filtered (could be null)
     */
    public InboxFilterCriteria(String bindingId, DiscoveryResultFlag flag) {
        this.bindingId = bindingId;
        this.thingTypeUID = null;
        this.thingUID = null;
        this.flag = flag;
    }

    /**
     * Creates a new instance of this class with the specified parameters.
     *
     * @param thingTypeUID the Thing type UID to be filtered (could be null or empty)
     * @param flag the discovery result flag to be filtered (could be null)
     */
    public InboxFilterCriteria(ThingTypeUID thingTypeUID, DiscoveryResultFlag flag) {
        this.bindingId = null;
        this.thingTypeUID = thingTypeUID;
        this.thingUID = null;
        this.flag = flag;
    }

    /**
     * Creates a new instance of this class with the specified parameters.
     *
     * @param thingUID the Thing UID to be filtered (could be null or empty)
     * @param flag the discovery result flag to be filtered (could be null)
     */
    public InboxFilterCriteria(ThingUID thingUID, DiscoveryResultFlag flag) {
        this.bindingId = null;
        this.thingTypeUID = null;
        this.thingUID = thingUID;
        this.flag = flag;
    }

    /**
     * Creates a new instance of this class with the specified parameters.
     *
     * @param flag the discovery result flag to be filtered (could be null)
     */
    public InboxFilterCriteria(DiscoveryResultFlag flag) {
        this.bindingId = null;
        this.thingTypeUID = null;
        this.thingUID = null;
        this.flag = flag;
    }

    /**
     * Returns the binding ID to be filtered.
     *
     * @return the binding ID to be filtered (could be null or empty)
     */
    public String getBindingId() {
        return this.bindingId;
    }

    /**
     * Returns the {@code Thing} type UID to be filtered.
     *
     * @return the Thing type UID to be filtered (could be null or empty)
     */
    public ThingTypeUID getThingTypeUID() {
        return this.thingTypeUID;
    }

    /**
     * Returns the {@code Thing} UID to be filtered.
     *
     * @return the Thing UID to be filtered (could be null or empty)
     */
    public ThingUID getThingUID() {
        return this.thingUID;
    }

    /**
     * Return the {@link DiscoveryResultFlag} to be filtered.
     *
     * @return the discovery result flag to be filtered (could be null)
     */
    public DiscoveryResultFlag getFlag() {
        return this.flag;
    }

}
