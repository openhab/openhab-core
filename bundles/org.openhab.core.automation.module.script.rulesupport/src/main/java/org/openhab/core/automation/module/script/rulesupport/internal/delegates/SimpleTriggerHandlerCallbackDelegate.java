/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.automation.module.script.rulesupport.internal.delegates;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.RuleStatus;
import org.openhab.core.automation.RuleStatusInfo;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.handler.TriggerHandlerCallback;
import org.openhab.core.automation.module.script.rulesupport.shared.simple.SimpleTriggerHandlerCallback;

/**
 * The {@link SimpleTriggerHandlerCallbackDelegate} allows a script to define callbacks for triggers in different ways.
 *
 * @author Simon Merschjohann - Initial contribution
 */
@NonNullByDefault
public class SimpleTriggerHandlerCallbackDelegate implements SimpleTriggerHandlerCallback {
    private final Trigger trigger;
    private final TriggerHandlerCallback callback;

    public SimpleTriggerHandlerCallbackDelegate(Trigger trigger, TriggerHandlerCallback callback) {
        this.trigger = trigger;
        this.callback = callback;
    }

    @Override
    public void triggered(Trigger trigger, Map<String, ?> context) {
        callback.triggered(trigger, context);
    }

    @Override
    public void triggered(Map<String, ?> context) {
        callback.triggered(this.trigger, context);
    }

    @Override
    public ScheduledExecutorService getScheduler() {
        return callback.getScheduler();
    }

    @Override
    public @Nullable Boolean isEnabled(String ruleUID) {
        return callback.isEnabled(ruleUID);
    }

    @Override
    public void setEnabled(String uid, boolean isEnabled) {
        callback.setEnabled(uid, isEnabled);
    }

    @Override
    public @Nullable RuleStatusInfo getStatusInfo(String ruleUID) {
        return callback.getStatusInfo(ruleUID);
    }

    @Override
    public @Nullable RuleStatus getStatus(String ruleUID) {
        return callback.getStatus(ruleUID);
    }

    @Override
    public void runNow(String uid) {
        callback.runNow(uid);
    }

    @Override
    public void runNow(String uid, boolean considerConditions, @Nullable Map<String, Object> context) {
        callback.runNow(uid, considerConditions, context);
    }
}
