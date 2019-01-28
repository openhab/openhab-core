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
package org.eclipse.smarthome.model.script.internal.actions;

import org.eclipse.xtext.xbase.lib.Procedures.Procedure0;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a Quartz {@link Job} which executes the code of a closure that is passed
 * to the createTimer() extension method.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
public class TimerExecutionJob implements Job {

    private final Logger logger = LoggerFactory.getLogger(TimerExecutionJob.class);

    /**
     * Runs the configured closure of this job
     *
     * @param context the execution context of the job
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.debug("Executing timer '{}'", context.getJobDetail().getKey().toString());
        Procedure0 procedure = (Procedure0) context.getJobDetail().getJobDataMap().get("procedure");
        Procedure1<Object> procedure1 = (Procedure1<Object>) context.getJobDetail().getJobDataMap().get("procedure1");
        TimerImpl timer = (TimerImpl) context.getJobDetail().getJobDataMap().get("timer");
        Object argument1 = context.getJobDetail().getJobDataMap().get("argument1");
        if (argument1 != null) {
            procedure1.apply(argument1);
        } else {
            procedure.apply();
        }
        timer.setTerminated(true);
    }

}
