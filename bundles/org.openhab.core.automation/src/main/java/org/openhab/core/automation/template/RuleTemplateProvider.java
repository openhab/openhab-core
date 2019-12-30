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
package org.openhab.core.automation.template;

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Provider;
import org.openhab.core.common.registry.ProviderChangeListener;

/**
 * This interface provides basic functionality for managing {@link RuleTemplate}s. It can be used for
 * <ul>
 * <li>Get the existing {@link RuleTemplate}s with the {@link Provider#getAll()},
 * {@link TemplateProvider#getTemplates(Locale)} and {@link #getTemplate(String, Locale)} methods.</li>
 * </ul>
 * Listers that are listening for adding removing or updating can be added with the
 * {@link #addProviderChangeListener(ProviderChangeListener)}
 * and removed with the {@link #removeProviderChangeListener(ProviderChangeListener)} methods.
 *
 * @author Ana Dimova - Initial contribution
 */
@NonNullByDefault
public interface RuleTemplateProvider extends TemplateProvider<RuleTemplate> {

}
