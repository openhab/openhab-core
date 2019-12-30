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
package org.openhab.core.model.rule.jvmmodel;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.script.engine.action.ActionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RulesRefresher} is responsible for reloading rules resources every time.
 *
 * @author Oliver Libutzki - Initial contribution
 * @author Kai Kreuzer - added delayed execution
 * @author Maoliang Huang - refactor
 */
public class RulesRefresher {

    // delay before rule resources are refreshed after items or services have changed
    private static final long REFRESH_DELAY = 2000;

    private final Logger logger = LoggerFactory.getLogger(RulesRefresher.class);

    ModelRepository modelRepository;
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> job;

    public void setModelRepository(ModelRepository modelRepository) {
        this.modelRepository = modelRepository;
    }

    public void unsetModelRepository(ModelRepository modelRepository) {
        this.modelRepository = null;
    }

    protected void addActionService(ActionService actionService) {
        scheduleRuleRefresh();
    }

    protected void removeActionService(ActionService actionService) {
        scheduleRuleRefresh();
    }

    protected synchronized void scheduleRuleRefresh() {
        if (job != null && !job.isDone()) {
            job.cancel(false);
        }
        job = scheduler.schedule(runnable, REFRESH_DELAY, TimeUnit.MILLISECONDS);
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (modelRepository != null) {
                    modelRepository.reloadAllModelsOfType("rules");
                }
            } catch (Exception e) {
                logger.debug("Exception occurred during execution: {}", e.getMessage(), e);
            }
        }
    };

}
