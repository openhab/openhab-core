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

import java.util.List;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultFlag;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingUID;

/**
 * The {@link Inbox} is a service interface providing a container for discovered {@code Thing}s
 * (e.g. found by a {@link DiscoveryService}) as {@link DiscoveryResult}s.
 * <p>
 * A {@link DiscoveryResult} entry in this container is not a full configured {@code Thing} and therefore no
 * {@code Thing} exists for it. A {@link DiscoveryResult} can be marked to be ignored, so that a specific {@code Thing}
 * is not considered to get part of the system.
 * <p>
 * This container offers a listener registry for {@link InboxListener}s which are notified if a {@link DiscoveryResult}
 * is added, updated or removed.
 *
 * @author Michael Grammling - Initial Contribution
 *
 * @see InboxListener
 */
@NonNullByDefault
public interface Inbox {

    /**
     * Adds the specified {@link DiscoveryResult} to this {@link Inbox} and sends an <i>ADDED</i>
     * event to any registered {@link InboxListener}.
     * <p>
     * If there is already a {@link DiscoveryResult} with the same {@code Thing} ID in this {@link Inbox}, the specified
     * {@link DiscoveryResult} is synchronized with the existing one, while keeping the {@link DiscoveryResultFlag} and
     * overriding the specific properties. In that case an <i>UPDATED</i> event is sent to any registered
     * {@link InboxListener}.
     * <p>
     * This method returns silently, if the specified {@link DiscoveryResult} is {@code null}.
     *
     * @param result the discovery result to be added to this inbox (could be null)
     * @return true if the specified discovery result could be added or updated, otherwise false
     */
    boolean add(@Nullable DiscoveryResult result);

    /**
     * Removes the {@link DiscoveryResult} associated with the specified {@code Thing} ID from
     * this {@link Inbox} and sends a <i>REMOVED</i> event to any registered {@link InboxListener}.
     * <p>
     * This method returns silently, if the specified {@code Thing} ID is {@code null}, empty, invalid, or no associated
     * {@link DiscoveryResult} exists in this {@link Inbox}.
     *
     * @param thingUID the Thing UID pointing to the discovery result to be removed from this inbox
     *            (could be null or invalid)
     * @return true if the specified discovery result could be removed, otherwise false
     */
    boolean remove(@Nullable ThingUID thingUID);

    /**
     * Returns all {@link DiscoveryResult}s in this {@link Inbox} which fit to the specified {@link InboxFilterCriteria}
     * .
     * <p>
     * If the specified {@link InboxFilterCriteria} is {@code null}, all {@link DiscoveryResult}s in this {@link Inbox}
     * are returned.
     *
     * @param criteria the filter criteria to be used for filtering all discovery results
     *            (could be null)
     * @return all discovery results in this inbox which fit to the specified filter criteria
     *         (not null, could be empty)
     *
     * @deprecated use {@link InboxPredicates} to filter on streams of {@link DiscoveryResult}s
     */
    @Deprecated
    List<DiscoveryResult> get(@Nullable InboxFilterCriteria criteria);

    /**
     * Returns all {@link DiscoveryResult}s in this {@link Inbox}.
     *
     * @return all discovery results in this inbox (not null, could be empty)
     */
    List<DiscoveryResult> getAll();

    /**
     * Returns a stream of all {@link DiscoveryResult}s in this {@link Inbox}.
     *
     * @return stream of all discovery results in this inbox
     */
    Stream<DiscoveryResult> stream();

    /**
     * Sets the flag for a given thingUID result.<br>
     * The flag signals e.g. if the result is {@link DiscoveryResultFlag#NEW} or has been marked as
     * {@link DiscoveryResultFlag#IGNORED}. In the latter
     * case the result object should be regarded as known by the system so that
     * further processing should be skipped.
     * <p>
     * If the specified flag is {@code null}, {@link DiscoveryResultFlag.NEW} is set by default.
     *
     * @param flag the flag of the given thingUID result to be set (could be null)
     */
    public void setFlag(ThingUID thingUID, @Nullable DiscoveryResultFlag flag);

    /**
     * Adds an {@link InboxListener} to the listeners' registry.
     * <p>
     * When a {@link DiscoveryResult} is <i>ADDED</i>, <i>UPDATED</i> or <i>REMOVED</i>, the specified listener is
     * notified.
     * <p>
     * This method returns silently if the specified listener is {@code null} or has already been registered before.
     *
     * @param listener the listener to be added (could be null)
     */
    void addInboxListener(@Nullable InboxListener listener);

    /**
     * Removes an {@link InboxListener} from the listeners' registry.
     * <p>
     * When this method returns, the specified listener is no longer notified about an <i>ADDED</i>, <i>UPDATED</i> or
     * <i>REMOVED</i> {@link DiscoveryResult}.
     * <p>
     * This method returns silently if the specified listener is {@code null} or has not been registered before.
     *
     * @param listener the listener to be removed (could be null)
     */
    void removeInboxListener(@Nullable InboxListener listener);

    /**
     * Creates new {@link Thing} and adds it to the {@link ThingRegistry}.
     *
     * @param thingUID the UID of the Thing
     * @param label the label of the Thing
     * @return the approved Thing
     */
    @Nullable
    Thing approve(ThingUID thingUID, @Nullable String label);

}
