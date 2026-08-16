/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.storage.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulates the disabled-rules cleanup logic: tracking missing-since timestamps, scheduling
 * rescans and removing orphaned entries from storage.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
public class DisabledRulesCleaner {

    private final Logger logger = LoggerFactory.getLogger(DisabledRulesCleaner.class);

    private final RuleRegistry ruleRegistry;
    private final Storage<Boolean> disabledRulesStorage;
    private final Supplier<ScheduledExecutorService> executorSupplier;
    private static final long ORPHAN_THRESHOLD_MS = TimeUnit.MINUTES.toMillis(2);
    private static final long RECHECK_INTERVAL_MS = TimeUnit.MINUTES.toMillis(1);

    private final Object lock = new Object();

    // allocated when the cleaner itself is created
    private final Map<String, Long> missingSince = new ConcurrentHashMap<>();
    private volatile Future<?> scheduledFuture;
    private volatile Runnable onFinished;

    public DisabledRulesCleaner(RuleRegistry ruleRegistry, Storage<Boolean> disabledRulesStorage,
            Supplier<ScheduledExecutorService> executorSupplier) {
        this.ruleRegistry = ruleRegistry;
        this.disabledRulesStorage = disabledRulesStorage;
        this.executorSupplier = executorSupplier;
    }

    /**
     * Set a callback that will be invoked when the cleaner finishes its scan-rescan sequence
     * and has no remaining candidates. The callback is executed outside the cleaner lock.
     */
    public void setOnFinished(Runnable onFinished) {
        this.onFinished = onFinished;
    }

    public void start(long initialDelayMinutes) {
        synchronized (lock) {
            cancelInternal();
            if (initialDelayMinutes <= 0) {
                logger.debug("Disabled rules cleanup is disabled ({} minutes)", initialDelayMinutes);
                return;
            }
            scheduledFuture = executorSupplier.get().schedule(this::runCleanupScan, initialDelayMinutes,
                    TimeUnit.MINUTES);
        }
    }

    public void stop() {
        synchronized (lock) {
            cancelInternal();
            // clear tracked candidates to free memory
            missingSince.clear();
        }
    }

    public void onRuleAdded(String uid) {
        Map<String, Long> ms = missingSince;
        if (ms != null) {
            ms.remove(uid);
        }
    }

    private void cancelInternal() {
        Future<?> f = scheduledFuture;
        if (f != null && !f.isDone()) {
            f.cancel(true);
        }
        scheduledFuture = null;
    }

    private void runCleanupScan() {
        logger.debug("Starting cleanup scan of disabled rules in storage '{}'", "automation_rules_disabled");
        try {
            long now = System.currentTimeMillis();

            final Set<String> keys = new HashSet<>(disabledRulesStorage.getKeys());

            Map<String, Long> ms = missingSince;

            for (String uid : keys) {
                if (ruleRegistry.get(uid) == null) {
                    ms.putIfAbsent(uid, now);
                } else {
                    ms.remove(uid);
                }
            }

            int removed = 0;
            for (Iterator<Entry<String, Long>> it = ms.entrySet().iterator(); it.hasNext();) {
                Entry<String, Long> e = it.next();
                String uid = e.getKey();
                long since = e.getValue();
                if (now - since >= ORPHAN_THRESHOLD_MS) {
                    if (disabledRulesStorage.remove(uid) != null) {
                        removed++;
                        logger.debug("Removed disabled-rules entry for non-existing rule '{}'", uid);
                    }
                    it.remove();
                }
            }

            if (removed > 0) {
                logger.info("Disabled rules cleanup: removed {} orphan entries.", removed);
            } else {
                logger.debug("Disabled rules cleanup: no orphan entries removed in this scan.");
            }

            if (!ms.isEmpty()) {
                logger.debug("Remaining {} potential orphan entries; scheduling re-scan in {} ms", ms.size(),
                        RECHECK_INTERVAL_MS);
                synchronized (lock) {
                    scheduledFuture = executorSupplier.get().schedule(this::runCleanupScan, RECHECK_INTERVAL_MS,
                            TimeUnit.MILLISECONDS);
                }
            } else {
                Runnable notify = null;
                synchronized (lock) {
                    scheduledFuture = null;
                    ms.clear();
                    notify = this.onFinished;
                }
                if (notify != null) {
                    try {
                        notify.run();
                    } catch (Throwable t) {
                        logger.warn("DisabledRulesCleaner onFinished callback threw: {}", t.getMessage());
                    }
                }
            }
        } catch (Throwable t) {
            logger.warn("Exception during disabled rules cleanup: {}", t.getMessage());
        }
    }
}
