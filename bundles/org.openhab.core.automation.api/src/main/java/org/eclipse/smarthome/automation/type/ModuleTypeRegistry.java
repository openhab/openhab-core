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
package org.eclipse.smarthome.automation.type;

import java.util.Collection;
import java.util.Locale;

import org.eclipse.smarthome.core.common.registry.Registry;

/**
 * This interface provides functionality to get available {@link ModuleType}s. The module types can be returned
 * localized depending on locale parameter. When it is not specified or there is no such localization resources the
 * returned module type is localized with default locale.
 *
 * @author Yordan Mihaylov - Initial Contribution
 * @author Ana Dimova - Initial Contribution
 * @author Vasil Ilchev - Initial Contribution
 */
public interface ModuleTypeRegistry extends Registry<ModuleType, String> {

    /**
     * Gets the localized {@link ModuleType} by specified UID and locale.
     *
     * @param moduleTypeUID the an unique id in scope of registered {@link ModuleType}s.
     * @param locale        used for localization of the {@link ModuleType}.
     * @param               <T> the type of the required object.
     * @return the desired {@link ModuleType} instance or {@code null} if a module type with such UID does not exist or
     *         the passed UID is {@code null}.
     */
    public <T extends ModuleType> T get(String moduleTypeUID, Locale locale);

    /**
     * Gets the {@link ModuleType}s filtered by tag.
     *
     * @param moduleTypeTag specifies the filter for getting the {@link ModuleType}s, if it is {@code null} then returns
     *                      all {@link ModuleType}s.
     * @param               <T> the type of the required object.
     * @return the {@link ModuleType}s, which correspond to the specified filter.
     */
    public <T extends ModuleType> Collection<T> getByTag(String moduleTypeTag);

    /**
     * This method is used for getting the {@link ModuleType}s filtered by tag.
     *
     * @param moduleTypeTag specifies the filter for getting the {@link ModuleType}s, if it is {@code null} then returns
     *                      all {@link ModuleType}s.
     * @param locale        used for localization of the {@link ModuleType}.
     * @param               <T> the type of the required object.
     * @return the {@link ModuleType}s, which correspond to the specified filter.
     */
    public <T extends ModuleType> Collection<T> getByTag(String moduleTypeTag, Locale locale);

    /**
     * This method is used for getting the {@link ModuleType}s filtered by tags.
     *
     * @param tags specifies the filter for getting the {@link ModuleType}s, if it is {@code null} then returns all
     *             {@link ModuleType}s.
     * @param      <T> the type of the required object.
     * @return the {@link ModuleType}s, which correspond to the filter.
     */
    public <T extends ModuleType> Collection<T> getByTags(String... tags);

    /**
     * This method is used for getting the {@link ModuleType}s filtered by tags.
     *
     * @param locale        used for localization of the {@link ModuleType}.
     * @param moduleTypeTag specifies the filter for getting the {@link ModuleType}s, if it is {@code null} then returns
     *                      all {@link ModuleType}s.
     * @param               <T> the type of the required object.
     * @return the {@link ModuleType}s, which correspond to the filter.
     */
    public <T extends ModuleType> Collection<T> getByTags(Locale locale, String... tags);

    /**
     * This method is used for getting the {@link TriggerType}s. The returned {@link TriggerType}s are
     * localized by default locale.
     *
     * @param tags specifies the filter for getting the {@link TriggerType}s, if it is {@code null} then returns all
     *             {@link TriggerType}s.
     * @return collection of all available {@link TriggerType}s, localized by default locale.
     */
    public Collection<TriggerType> getTriggers(String... tags);

    /**
     * This method is used for getting the {@link TriggerType}s, localized depending on passed locale parameter.
     * When the locale parameter is not specified or such localization resources are not available the returned
     * {@link TriggerType}s are localized by default locale.
     *
     * @param locale defines the localization of returned {@link TriggerType}s.
     * @param tags   specifies the filter for getting the {@link TriggerType}s, if it is {@code null} then returns all
     *               {@link TriggerType}s.
     * @return a collection of all available {@link TriggerType}s, localized by default locale or the passed locale
     *         parameter.
     */
    public Collection<TriggerType> getTriggers(Locale locale, String... tags);

    /**
     * This method is used for getting the {@link ConditionType}s. The returned {@link ConditionType}s are
     * localized by default locale.
     *
     * @param tags specifies the filter for getting the {@link ConditionType}s, if it is {@code null} then returns all
     *             {@link ConditionType}s.
     * @return collection of all available {@link ConditionType}s, localized by default locale.
     */
    public Collection<ConditionType> getConditions(String... tags);

    /**
     * This method is used for getting the {@link ConditionType}s, localized depending on passed locale parameter.
     * When the locale parameter is not specified or such localization resources are not available the
     * returned {@link ConditionType}s are localized by default locale.
     *
     * @param locale defines the localization of returned {@link ConditionType}s.
     * @param tags   specifies the filter for getting the {@link ConditionType}s, if it is {@code null} then returns all
     *               {@link ConditionType}s.
     * @return a collection of all available {@link ConditionType}s, localized by default locale or the passed locale
     *         parameter.
     */
    public Collection<ConditionType> getConditions(Locale locale, String... tags);

    /**
     * This method is used for getting the {@link ActionType}s. The returned {@link ActionType}s are
     * localized by default locale.
     *
     * @param tags specifies the filter for getting the {@link ActionType}s, if it is {@code null} then returns all
     *             {@link ActionType}s.
     * @return collection of all available {@link ActionType}s, localized by default locale.
     */
    public Collection<ActionType> getActions(String... tags);

    /**
     * This method is used for getting the {@link ActionType}s, localized depending on passed locale parameter.
     * When the locale parameter is not specified or such localization resources are not available the returned
     * {@link ActionType}s are localized by default locale.
     *
     * @param locale defines the localization of returned {@link ActionType}s.
     * @param tags   specifies the filter for getting the {@link ActionType}s, if it is {@code null} then returns all
     *               {@link ActionType}s.
     * @return a collection of all available {@link ActionType}s, localized by default locale or the passed locale
     *         parameter.
     */
    public Collection<ActionType> getActions(Locale locale, String... tags);

}
