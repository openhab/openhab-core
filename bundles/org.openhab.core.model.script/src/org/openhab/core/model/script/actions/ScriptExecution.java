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
package org.openhab.core.model.script.actions;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.time.Instant;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure0;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.script.ScriptServiceUtil;
import org.openhab.core.model.script.engine.Script;
import org.openhab.core.model.script.engine.ScriptEngine;
import org.openhab.core.model.script.engine.ScriptExecutionException;
import org.openhab.core.model.script.internal.actions.TimerExecutionJob;
import org.openhab.core.model.script.internal.actions.TimerImpl;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The static methods of this class are made available as functions in the scripts.
 * This allows a script to call another script, which is available as a file.
 *
 * @author Kai Kreuzer - Initial contribution
 */
public class ScriptExecution {

    private static int timerCounter = 0;

    /**
     * Calls a script which must be located in the configurations/scripts folder.
     *
     * @param scriptName the name of the script (if the name does not end with
     *            the .script file extension it is added)
     *
     * @return the return value of the script
     * @throws ScriptExecutionException if an error occurs during the execution
     */
    public static Object callScript(String scriptName) throws ScriptExecutionException {
        ModelRepository repo = ScriptServiceUtil.getModelRepository();
        if (repo != null) {
            String scriptNameWithExt = scriptName;
            if (!StringUtils.endsWith(scriptName, Script.SCRIPT_FILEEXT)) {
                scriptNameWithExt = scriptName + "." + Script.SCRIPT_FILEEXT;
            }
            XExpression expr = (XExpression) repo.getModel(scriptNameWithExt);
            if (expr != null) {
                ScriptEngine scriptEngine = ScriptServiceUtil.getScriptEngine();
                if (scriptEngine != null) {
                    Script script = scriptEngine.newScriptFromXExpression(expr);
                    return script.execute();
                } else {
                    throw new ScriptExecutionException("Script engine is not available.");
                }
            } else {
                throw new ScriptExecutionException("Script '" + scriptName + "' cannot be found.");
            }
        } else {
            throw new ScriptExecutionException("Model repository is not available.");
        }
    }

    /**
     * Schedules a block of code for later execution.
     *
     * @param instant the point in time when the code should be executed
     * @param closure the code block to execute
     *
     * @return a handle to the created timer, so that it can be canceled or rescheduled
     * @throws ScriptExecutionException if an error occurs during the execution
     */
    public static Timer createTimer(Instant instant, Procedure0 closure) {
        JobDataMap dataMap = new JobDataMap();
        dataMap.put("procedure", closure);
        return makeTimer(instant, closure.toString(), dataMap);
    }

    /**
     * Schedules a block of code (with argument) for later execution
     *
     * @param instant the point in time when the code should be executed
     * @param arg1 the argument to pass to the code block
     * @param closure the code block to execute
     *
     * @return a handle to the created timer, so that it can be canceled or rescheduled
     * @throws ScriptExecutionException if an error occurs during the execution
     */
    public static Timer createTimerWithArgument(Instant instant, Object arg1, Procedure1<Object> closure) {
        JobDataMap dataMap = new JobDataMap();
        dataMap.put("procedure1", closure);
        dataMap.put("argument1", arg1);
        return makeTimer(instant, closure.toString(), dataMap);
    }

    /**
     * helper function to create the timer
     *
     * @param instant the point in time when the code should be executed
     * @param closure string for job id
     * @param dataMap job data map, preconfigured with arguments
     * @return
     */
    private static Timer makeTimer(Instant instant, String closure, JobDataMap dataMap) {

        Logger logger = LoggerFactory.getLogger(ScriptExecution.class);
        JobKey jobKey = new JobKey(getTimerId() + " " + instant.toString() + ": " + closure.toString());
        Trigger trigger = newTrigger().startAt(Date.from(instant)).build();
        Timer timer = new TimerImpl(jobKey, trigger.getKey(), dataMap, instant);
        try {
            JobDetail job = newJob(TimerExecutionJob.class).withIdentity(jobKey).usingJobData(dataMap).build();
            if (TimerImpl.scheduler.checkExists(job.getKey())) {
                TimerImpl.scheduler.deleteJob(job.getKey());
                logger.debug("Deleted existing Job {}", job.getKey().toString());
            }
            TimerImpl.scheduler.scheduleJob(job, trigger);
            logger.debug("Scheduled code for execution at {}", instant.toString());
            return timer;
        } catch (SchedulerException e) {
            logger.error("Failed to schedule code for execution.", e);
            return null;
        }
    }

    private static synchronized String getTimerId() {
        return String.format("Timer %d", ++timerCounter);
    }
}
