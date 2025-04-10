/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.config.discovery.inbox;

import java.util.function.Predicate;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.discovery.DiscoveryResult;

/**
 * {@link org.osgi.service.component.annotations.Component}s implementing this interface participate in the
 * {@link org.openhab.core.config.discovery.internal.AutomaticInboxProcessor}'s
 * decision whether to automatically approve an inbox result or not.
 * <p/>
 * If this {@link Predicate} returns <code>true</code> the {@link DiscoveryResult} will be automatically approved by the
 * {@link org.openhab.core.config.discovery.internal.AutomaticInboxProcessor}.
 * <p/>
 * Note that if this {@link Predicate} returns <code>false</code> the {@link DiscoveryResult} might still be
 * automatically approved (e.g., because another such {@link Predicate} returned <code>true</code>) - i.e., it is not
 * possible to veto the automatic approval of a {@link DiscoveryResult}.
 * <p/>
 * Please note that this interface is intended to be implemented by solutions integrating openHAB. This
 * interface is <em>not</em> intended to be implemented by openHAB addons (like, e.g., bindings).
 *
 * @author Henning Sudbrock - Initial contribution
 */
@NonNullByDefault
public interface InboxAutoApprovePredicate extends Predicate<DiscoveryResult> {

}
