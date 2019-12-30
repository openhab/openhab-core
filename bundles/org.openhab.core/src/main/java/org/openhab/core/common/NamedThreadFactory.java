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
package org.openhab.core.common;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread factory that applies a thread name constructed by a supplied identifier.
 *
 * <p>
 * The thread name will look similar to: OH-id-counter
 * The value of "id" will be replaced with the given ID.
 * The value of "counter" will start from one and increased for every newly created thread.
 *
 * @author Markus Rathgeb - Initial contribution
 */
public class NamedThreadFactory implements ThreadFactory {

    private final boolean daemonize;
    private final int priority;

    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;

    /**
     * Creates a new named thread factory.
     *
     * <p>
     * This constructor will create a new named thread factory using the following parameters:
     * <ul>
     * <li>daemonize: false
     * <li>priority: normale
     * </ul>
     *
     * @param id the identifier used for the thread name creation
     */
    public NamedThreadFactory(final String id) {
        this(id, false);
    }

    /**
     * Creates a new named thread factory.
     *
     * <p>
     * This constructor will create a new named thread factory using the following parameters:
     * <ul>
     * <li>daemonize: false
     * </ul>
     *
     * @param id the identifier used for the thread name creation
     * @param daemonize flag if the created thread should be daemonized
     */
    public NamedThreadFactory(final String id, final boolean daemonize) {
        this(id, daemonize, Thread.NORM_PRIORITY);
    }

    /**
     * Creates a new named thread factory.
     *
     * @param id the identifier used for the thread name creation
     * @param daemonize flag if the created threads should be daemonized
     * @param priority the priority of the created threads
     */
    public NamedThreadFactory(final String id, final boolean daemonize, final int priority) {
        this.daemonize = daemonize;
        this.priority = priority;
        this.namePrefix = "OH-" + id + "-";
        final SecurityManager securityManager = System.getSecurityManager();
        this.group = securityManager != null ? securityManager.getThreadGroup()
                : Thread.currentThread().getThreadGroup();
    }

    @Override
    public Thread newThread(Runnable runnable) {
        final Thread thread = new Thread(group, runnable, namePrefix + threadNumber.getAndIncrement(), 0);
        if (thread.isDaemon() != daemonize) {
            thread.setDaemon(daemonize);
        }
        if (thread.getPriority() != priority) {
            thread.setPriority(priority);
        }

        return thread;
    }

}
