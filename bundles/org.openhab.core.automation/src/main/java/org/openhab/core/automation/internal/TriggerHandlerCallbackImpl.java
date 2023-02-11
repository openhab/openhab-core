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
package org.openhab.core.automation.internal;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.RuleStatus;
import org.openhab.core.automation.RuleStatusInfo;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.handler.TriggerHandlerCallback;
import org.openhab.core.common.NamedThreadFactory;

/**
 * This class is implementation of {@link TriggerHandlerCallback} used by the {@link Trigger}s to notify rule engine
 * about appearing of new triggered data. There is one and only one {@link TriggerHandlerCallback} per Rule and
 * it is used by all rule's {@link Trigger}s.
 *
 * @author Yordan Mihaylov - Initial contribution
 * @author Kai Kreuzer - improved stability
 * @author Fabian Wolter - Change executor to ScheduledExecutorService and expose it
 */
@NonNullByDefault
public class TriggerHandlerCallbackImpl implements TriggerHandlerCallback {

    private final RuleEngineImpl re;

    private final String ruleUID;

    private ScheduledExecutorService executor;

    private @Nullable Future<?> future;

    protected TriggerHandlerCallbackImpl(RuleEngineImpl re, String ruleUID) {
        this.re = re;
        this.ruleUID = ruleUID;
        executor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("rule-" + ruleUID));
    }

    @Override
    public void triggered(Trigger trigger, Map<String, ?> context) {
        synchronized (this) {
            future = executor.submit(new TriggerData(trigger, context));
        }
        re.logger.debug("The trigger '{}' of rule '{}' is triggered.", trigger.getId(), ruleUID);
    }

    public boolean isRunning() {
        Future<?> future = this.future;
        return future == null || !future.isDone();
    }

    class TriggerData implements Runnable {

        private final Trigger trigger;
        private @Nullable final Map<String, ?> outputs;

        public Trigger getTrigger() {
            return trigger;
        }

        public @Nullable Map<String, ?> getOutputs() {
            return outputs;
        }

        public TriggerData(Trigger t, @Nullable Map<String, ?> outputs) {
            this.trigger = t;
            this.outputs = outputs;
        }

        @Override
        public void run() {
            re.runRule(ruleUID, this);
        }
    }

    public void dispose() {
        synchronized (this) {
            executor.shutdownNow();
        }
    }

    @Override
    public @Nullable Boolean isEnabled(String ruleUID) {
        return re.isEnabled(ruleUID);
    }

    @Override
    public void setEnabled(String uid, boolean isEnabled) {
        re.setEnabled(uid, isEnabled);
    }

    @Override
    public @Nullable RuleStatusInfo getStatusInfo(String ruleUID) {
        return re.getStatusInfo(ruleUID);
    }

    @Override
    public @Nullable RuleStatus getStatus(String ruleUID) {
        return re.getStatus(ruleUID);
    }

    @Override
    public void runNow(String uid) {
        re.runNow(uid);
    }

    @Override
    public void runNow(String uid, boolean considerConditions, @Nullable Map<String, Object> context) {
        re.runNow(uid, considerConditions, context);
    }

    @Override
    public ScheduledExecutorService getScheduler() {
        return executor;
    }
}
