/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.core.automation.module.script.internal.defaultscope;

/**
 * @author Markus Rathgeb - Initial contribution
 */
public class FQCNAccess {

    /**
     * Ensure access.
     *
     * <p>
     * This method is used to ensure that a defined set of classes is known by the classloader.
     * The access of the class will ensure that the package imports are added.
     */
    public static void load() {
        load(org.eclipse.smarthome.config.core.Configuration.class);
        load(org.eclipse.smarthome.core.binding.BindingInfoProvider.class);
        load(org.eclipse.smarthome.core.items.Metadata.class);
        load(org.eclipse.smarthome.core.items.MetadataKey.class);
        load(org.eclipse.smarthome.core.library.types.DateTimeType.class);
        load(org.eclipse.smarthome.core.service.AbstractWatchService.class);
        load(org.eclipse.smarthome.core.thing.ChannelUID.class);
        load(org.eclipse.smarthome.core.thing.ThingProvider.class);
        load(org.eclipse.smarthome.core.thing.ThingStatus.class);
        load(org.eclipse.smarthome.core.thing.ThingUID.class);
        load(org.eclipse.smarthome.core.thing.binding.ThingTypeProvider.class);
        load(org.eclipse.smarthome.core.thing.link.ItemChannelLinkProvider.class);
        load(org.eclipse.smarthome.core.thing.type.ChannelKind.class);
        load(org.eclipse.smarthome.core.types.TypeParser.class);

        load(org.openhab.core.automation.Rule.class);
        load(org.openhab.core.automation.Trigger.class);
        load(org.openhab.core.automation.handler.TriggerHandler.class);
        load(org.openhab.core.automation.util.RuleBuilder.class);
        load(org.openhab.core.automation.util.TriggerBuilder.class);

        // The following are optional as we don't want to hard depend on the bundles.
        // Import the package optional in the manifest.

        /*
         * bundle:
         * package: org.joda.time
         * class: DateTime
         */
        /*
         * bundle: org.openhab.core.compat1x
         * package: org.openhab.core.library.types
         * class: DateTimeType
         */
        /*
         * bundle: org.openhab.core.compat1x
         * package: org.openhab.core.scriptengine.action
         * class ActionService
         */
        /*
         * bundle: org.openhab.core.model.script
         * package: org.eclipse.smarthome.model.script.engine.action
         * class: ActionService
         */
        /*
         * bundle: org.openhab.core.model.persistence
         * package: org.eclipse.smarthome.model.persistence.extensions
         * class: PersistenceExtensions
         */
        /*
         * bundle: org.openhab.core.model.script
         * package: org.eclipse.smarthome.model.script.actions
         * class: Exec
         * method: executeCommandLine
         */
        /*
         * bundle: org.openhab.core.transform
         * package: org.eclipse.smarthome.core.transform
         * class: TransformationService
         */
    }

    private static void load(final Class<?> clazz) {
    }

    private FQCNAccess() {
    }

}
