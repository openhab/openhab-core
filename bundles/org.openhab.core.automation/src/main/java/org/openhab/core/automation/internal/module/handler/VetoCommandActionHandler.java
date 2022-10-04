/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.core.automation.internal.module.handler;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.automation.handler.BaseActionModuleHandler;
import org.openhab.core.items.events.ItemCommandEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an implementation of an ActionHandler. It vetos commands.
 *
 * @author Cody Cutrer - Initial contribution
 */
@NonNullByDefault
public class VetoCommandActionHandler extends BaseActionModuleHandler {

    public static final String VETO_COMMAND_ACTION = "core.VetoCommandAction";

    private final Logger logger = LoggerFactory.getLogger(VetoCommandActionHandler.class);

    private final String ruleUID;
    private final RuleRegistry ruleRegistry;

    /**
     * constructs a new VetoCommandActionHandler
     *
     * @param module
     */
    public VetoCommandActionHandler(Action module, String ruleUID, RuleRegistry ruleRegistry) {
        super(module);
        this.ruleUID = ruleUID;
        this.ruleRegistry = ruleRegistry;
    }

    @Override
    public @Nullable Map<String, Object> execute(Map<String, Object> inputs) {
        Object event = inputs.get("event");
        if (event instanceof ItemCommandEvent) {
            Rule rule = ruleRegistry.get(ruleUID);
            if (rule == null) {
                return null;
            }
            if (!rule.isSynchronous()) {
                logger.warn("VetoCommandActionHandler attached to rule {} that is not synchronous; ignoring.", ruleUID);
                return null;
            }
            ItemCommandEvent commandEvent = (ItemCommandEvent) event;
            logger.debug("Vetoing Command {} sent to Item {}", commandEvent.getItemCommand(),
                    commandEvent.getItemName());
            commandEvent.veto();
        }
        return null;
    }
}
