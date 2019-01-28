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

import java.util.function.Predicate;

import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.internal.AutomaticInboxProcessor;
import org.osgi.service.component.annotations.Component;

/**
 * {@link Component}s implementing this interface participate in the {@link AutomaticInboxProcessor}'s decision whether
 * to automatically approve an inbox result or not.
 * <p/>
 * If this {@link Predicate} returns <code>true</code> the {@link DiscoveryResult} will be automatically approved by the
 * {@link AutomaticInboxProcessor}.
 * <p/>
 * Note that if this {@link Predicate} returns <code>false</code> the {@link DiscoveryResult} might still be
 * automatically approved (e.g., because another such {@link Predicate} returned <code>true</code>) - i.e., it is not
 * possible to veto the automatic approval of a {@link DiscoveryResult}.
 * <p/>
 * Please note that this interface is intended to be implemented by solutions integrating Eclipse SmartHome. This
 * interface is <em>not</em> intended to be implemented by Eclipse SmartHome addons (like, e.g., bindings).
 *
 * @author Henning Sudbrock - initial contribution
 */
public interface InboxAutoApprovePredicate extends Predicate<DiscoveryResult> {

}
