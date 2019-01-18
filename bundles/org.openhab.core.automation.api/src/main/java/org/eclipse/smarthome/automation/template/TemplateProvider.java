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

import java.util.Collection;
import java.util.Locale;

import org.eclipse.smarthome.core.common.registry.Provider;

/**
 * This interface has to be implemented by all providers of {@link Template}s.
 * The {@link TemplateRegistry} uses it to get access to available {@link Template}s.
 *
 * @author Yordan Mihaylov - Initial Contribution
 * @author Kai Kreuzer - refactored (managed) provider and registry implementation
 * @author Ana Dimova - add registration property - rule.templates
 */
public interface TemplateProvider<E extends Template> extends Provider<E> {

    /**
     * Gets the localized Templates defined by this provider. When the localization is not specified or it is not
     * supported a Template localized with default locale is returned.
     *
     * @param UID    unique identifier of the desired Template.
     * @param locale specifies the desired {@link Locale} to be used for localization of the returned element. If
     *               localization resources for this locale are not available or the passed locale is {@code null} the
     *               element is returned with the default localization.
     * @return the desired localized Template.
     */
    E getTemplate(String UID, Locale locale);

    /**
     * Gets the localized Templates defined by this provider. When localization is not specified or it is not supported
     * a Templates with default localization is returned.
     *
     * @param locale specifies the desired {@link Locale} to be used for localization of the returned elements. If
     *               localization resources for this locale are not available or the passed locale is {@code null} the
     *               elements are returned with the default localization.
     * @return a collection of localized {@link Template}s provided by this provider.
     */
    Collection<E> getTemplates(Locale locale);

}
