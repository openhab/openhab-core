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

import java.util.Collection;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.Registry;

/**
 * This interface provides functionality to get available {@link Template}s. The {@link Template} can be returned
 * localized depending on locale parameter. When the parameter is not specified or there is no such localization
 * resources the returned template is localized with default locale.
 *
 * @author Yordan Mihaylov - Initial contribution
 * @author Ana Dimova - Initial contribution
 * @author Vasil Ilchev - Initial contribution
 */
@NonNullByDefault
public interface TemplateRegistry<E extends Template> extends Registry<E, String> {

    /**
     * Gets a template specified by unique identifier.
     *
     * @param uid the unique identifier in scope of registered templates.
     * @param locale specifies the desired {@link Locale} to be used for localization of the returned element. If
     *            localization resources for this locale are not available or the passed locale is {@code null} the
     *            element is returned with the default localization.
     * @return the desired template instance or {@code null} if a template with such UID does not exist or the passed
     *         UID is {@code null}.
     */
    public @Nullable E get(String uid, @Nullable Locale locale);

    /**
     * Gets the templates filtered by tag.
     *
     * @param tag determines the tag that the templates must have, to be included in the returned result. If it is
     *            {@code null} then the result will contain all available templates.
     * @return a collection of templates, which correspond to the specified tag.
     */
    public Collection<E> getByTag(@Nullable String tag);

    /**
     * Gets the templates filtered by tag.
     *
     * @param tag determines the tag that the templates must have, to be included in the returned result. If it is
     *            {@code null} then the result will contain all available templates.
     * @param locale specifies the desired {@link Locale} to be used for localization of the returned elements. If
     *            localization resources for this locale are not available or the passed locale is {@code null} the
     *            elements are returned with the default localization.
     * @return a collection of localized templates, which correspond to the specified tag.
     */
    public Collection<E> getByTag(@Nullable String tag, @Nullable Locale locale);

    /**
     * Gets the templates filtered by tags.
     *
     * @param tags determines the set of tags that the templates must have, to be included in the returned result. If it
     *            is {@code null} then the result will contain all templates.
     * @return a collection of templates, which correspond to the specified set of tags.
     */
    public Collection<E> getByTags(String... tags);

    /**
     * Gets the templates filtered by tags.
     *
     * @param locale specifies the desired {@link Locale} to be used for localization of the returned elements. If
     *            localization resources for this locale are not available or the passed locale is {@code null} the
     *            elements are returned with the default localization.
     * @param tags determines the set of tags that the templates must have, to be included in the returned result. If
     *            it is {@code null} then the result will contain all templates.
     * @return the templates, which correspond to the specified set of tags.
     */
    public Collection<E> getByTags(@Nullable Locale locale, String... tags);

    /**
     * Gets all available templates, localized by specified locale.
     *
     * @param locale specifies the desired {@link Locale} to be used for localization of the returned elements. If
     *            localization resources for this locale are not available or the passed locale is {@code null} the
     *            elements are returned with the default localization.
     * @return a collection of localized templates, corresponding to the parameterized type.
     */
    public Collection<E> getAll(@Nullable Locale locale);
}
