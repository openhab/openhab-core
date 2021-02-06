/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.openhab.core.automation.RuleStatus;
import org.openhab.core.automation.RuleStatusInfo;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.handler.TriggerHandlerCallback;
import org.openhab.core.common.NamedThreadFactory;
import org.openhab.core.common.ThreadPoolManager;
import org.eclipse.jdt.annotation.Nullable;
import org.osgi.service.component.annotations.Modified;

/**
 * This class is implementation of {@link TriggerHandlerCallback} used by the {@link Trigger}s to notify rule engine
 * about appearing of new triggered data. There is one and only one {@link TriggerHandlerCallback} per Rule and
 * it is used by all rule's {@link Trigger}s.
 *
 * @author Yordan Mihaylov - Initial contribution
 * @author Kai Kreuzer - improved stability
 */
public class TriggerHandlerCallbackImpl implements TriggerHandlerCallback {

    private static final String AUTOMATION_THREADPOOL_NAME = "automation";

    private boolean useThreadPool = false;

    private final String ruleUID;

    private ExecutorService executor;

    private Future<?> future;

    private final RuleEngineImpl re;

    protected TriggerHandlerCallbackImpl(RuleEngineImpl re, String ruleUID) {
        this.re = re;
        this.ruleUID = ruleUID;
	if ( useThreadPool == false ) {
            re.logger.debug("Firing rule {} using single thread",ruleUID);
            executor = Executors.newSingleThreadExecutor(new NamedThreadFactory("rule-" + ruleUID));
        } else {
	    re.logger.debug("Firing rule {} using threadpool",ruleUID);
	    executor = ThreadPoolManager.getPool(AUTOMATION_THREADPOOL_NAME);
	}
    }

    @Override
    public void triggered(Trigger trigger, Map<String, ?> outputs) {
        synchronized (this) {
            if (executor == null) {
                return;
            }
            future = executor.submit(new TriggerData(trigger, outputs));
        }
        re.logger.debug("The trigger '{}' of rule '{}' is triggered.  ThreadPool set to {}", trigger.getId(), ruleUID, useThreadPool);
    }

    public boolean isRunning() {
        Future<?> future = this.future;
        return future == null || !future.isDone();
    }

    class TriggerData implements Runnable {

        private final Trigger trigger;

        public Trigger getTrigger() {
            return trigger;
        }

        public Map<String, ?> getOutputs() {
            return outputs;
        }

        private final Map<String, ?> outputs;

        public TriggerData(Trigger t, Map<String, ?> outputs) {
            this.trigger = t;
            this.outputs = outputs;
        }

        @Override
        public void run() {
            re.runRule(ruleUID, this);
        }
    }

    @Modified
    public void modified(@Nullable Map<String, Object> properties) {
        if (properties != null) {
            Object prop = properties.get("rulesThreadPool");
            if (prop instanceof String) {
                try {
                    useThreadPool = Boolean.valueOf((String) prop);
                    re.logger.debug("Configured rulesThreadPool as '{}'",useThreadPool);
		} catch (NumberFormatException e) {
                    re.logger.error("Invalid value '{}' for rulesThreadPool - using default value '{}'", prop, useThreadPool);
                }
            }
        }
    }

    public void dispose() {
        if ( useThreadPool == false ) {
	    synchronized (this) {
                AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                    executor.shutdownNow();
                    return null;
                });
                executor = null;
            }
	} else {
	    executor = null;
        }
    }

    @Override
    public Boolean isEnabled(String ruleUID) {
        return re.isEnabled(ruleUID);
    }

    @Override
    public void setEnabled(String uid, boolean isEnabled) {
        re.setEnabled(uid, isEnabled);
    }

    @Override
    public RuleStatusInfo getStatusInfo(String ruleUID) {
        return re.getStatusInfo(ruleUID);
    }

    @Override
    public RuleStatus getStatus(String ruleUID) {
        return re.getStatus(ruleUID);
    }

    @Override
    public void runNow(String uid) {
        re.runNow(uid);
    }

    @Override
    public void runNow(String uid, boolean considerConditions, Map<String, Object> context) {
        re.runNow(uid, considerConditions, context);
    }
}
