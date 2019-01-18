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
package org.eclipse.smarthome.automation.template;

import java.util.Locale;

import org.eclipse.smarthome.core.common.registry.Provider;
import org.eclipse.smarthome.core.common.registry.ProviderChangeListener;

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
 * @author Ana Dimova - Initial Contribution
 */
public interface RuleTemplateProvider extends TemplateProvider<RuleTemplate> {

}
