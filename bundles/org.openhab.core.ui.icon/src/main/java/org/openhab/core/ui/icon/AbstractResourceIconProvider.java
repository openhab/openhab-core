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
package org.openhab.core.ui.icon;

import java.io.InputStream;
import java.util.Set;

import org.openhab.core.i18n.TranslationProvider;
import org.openhab.core.ui.icon.IconSet.Format;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an abstract base class for implementing icon providers that serve icons from file resources.
 * These files could be read from the file system, directly from the bundle itself or from somewhere else that can
 * provide an {@link InputStream}.
 *
 * The resources are expected to follow the naming convention "<category>[-<state>].<format>", e.g. "alarm.png" or
 * "alarm-on.svg".
 * Resource names must be all lower case. Whether an icon is provided or not is determined by the existence of a
 * resource without a state postfix.
 * If a specific resource for a state is available, it will be used. If not, the default icon without a state postfix is
 * used. If the state is a decimal number between 0 and 100, the implementation will look for a resource with the next
 * smaller state postfix available. Example: For category "DimmableLight" and state 84, it will check for the resources
 * dimmablelight-82.png, dimmablelight-81.png, dimmablelight-80.png and return the first one it can find.
 *
 * @author Kai Kreuzer - Initial contribution
 */
public abstract class AbstractResourceIconProvider implements IconProvider {

    private final Logger logger = LoggerFactory.getLogger(AbstractResourceIconProvider.class);

    /**
     * The OSGi bundle context
     */
    protected BundleContext context;

    /**
     * An TranslationProvider service
     */
    protected TranslationProvider i18nProvider;

    /**
     * When activating the service, we need to keep the bundle context.
     *
     * @param context the bundle context provided through OSGi DS.
     */
    protected void activate(BundleContext context) {
        this.context = context;
    }

    protected void setTranslationProvider(TranslationProvider i18nProvider) {
        this.i18nProvider = i18nProvider;
    }

    protected void unsetTranslationProvider(TranslationProvider i18nProvider) {
        this.i18nProvider = null;
    }

    @Override
    public Set<IconSet> getIconSets() {
        return getIconSets(null);
    }

    @Override
    public Integer hasIcon(String category, String iconSetId, Format format) {
        return hasResource(iconSetId, category.toLowerCase() + "." + format.toString().toLowerCase()) ? getPriority()
                : null;
    }

    @Override
    public InputStream getIcon(String category, String iconSetId, String state, Format format) {
        String resourceWithoutState = category.toLowerCase() + "." + format.toString().toLowerCase();
        if (state == null) {
            return getResource(iconSetId, resourceWithoutState);
        }

        String iconState;
        if (state.contains(" ")) {
            try {
                String firstPart = state.substring(0, state.indexOf(" "));
                Double.valueOf(firstPart);
                iconState = firstPart;
            } catch (NumberFormatException e) {
                // firstPart is not a number, pass on the full state
                iconState = state;
            }
        } else {
            iconState = state;
        }

        String resourceWithState = category.toLowerCase() + "-" + iconState.toLowerCase() + "."
                + format.toString().toLowerCase();
        if (hasResource(iconSetId, resourceWithState)) {
            return getResource(iconSetId, resourceWithState);
        } else {
            // let's treat all percentage-based categories
            try {
                Double stateAsDouble = Double.valueOf(iconState);
                if (stateAsDouble >= 0 && stateAsDouble <= 100) {
                    for (int i = stateAsDouble.intValue(); i >= 0; i--) {
                        String resourceWithNumberState = category.toLowerCase() + "-" + i + "."
                                + format.toString().toLowerCase();
                        if (hasResource(iconSetId, resourceWithNumberState)) {
                            return getResource(iconSetId, resourceWithNumberState);
                        }
                    }
                }
            } catch (NumberFormatException e) {
                // does not seem to be a number, so ignore it
            }
            logger.debug("Use icon {} as {} is not found", resourceWithoutState, resourceWithState);
            return getResource(iconSetId, resourceWithoutState);
        }
    }

    /**
     * Provides the priority of this provider. A higher value will give this provider a precedence over others.
     *
     * @return the priority as a positive integer
     */
    protected abstract Integer getPriority();

    /**
     * Provides the content of a resource for a certain icon set as a stream or null, if the resource does not exist.
     *
     * @param iconSetId the id of the icon set for which the resource is requested
     * @param resourceName the name of the resource
     * @return the content as a stream or null, if the resource does not exist
     */
    protected abstract InputStream getResource(String iconSetId, String resourceName);

    /**
     * Checks whether a certain resource exists for a given icon set.
     *
     * @param iconSetId the id of the icon set for which the resource is requested
     * @param resourceName the name of the resource
     * @return true, if the resource exists, false otherwise
     */
    protected abstract boolean hasResource(String iconSetId, String resourceName);

}
