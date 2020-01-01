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
package org.openhab.core.model.script.scheduler.test;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.quartz.Calendar;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.ListenerManager;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.quartz.SchedulerMetaData;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerKey;
import org.quartz.UnableToInterruptJobException;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.JobFactory;

/**
 * Abstract Quartz Scheduler.
 *
 * This hides all of the methods that we don't need to implement,
 * to make the {@see MockScheduler} class easier to read.
 *
 * @author Jon Evans - Initial contribution
 */
public abstract class AbstractScheduler implements Scheduler {

    @Override
    public boolean isShutdown() throws SchedulerException {
        return false;
    }

    // Anything below this point is unsupported, but we want to know
    // about it if tests break in the future (e.g. new version of
    // Quartz), so we throw UnsupportedException if any of these
    // methods are called

    @Override
    public String getSchedulerName() throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSchedulerInstanceId() throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SchedulerContext getContext() throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void start() throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void startDelayed(int seconds) throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isStarted() throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void standby() throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isInStandbyMode() throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void shutdown() throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void shutdown(boolean waitForJobsToComplete) throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SchedulerMetaData getMetaData() throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<JobExecutionContext> getCurrentlyExecutingJobs() throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setJobFactory(JobFactory factory) throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListenerManager getListenerManager() throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Date scheduleJob(JobDetail jobDetail, Trigger trigger) throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Date scheduleJob(Trigger trigger) throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void scheduleJobs(Map<JobDetail, Set<? extends Trigger>> triggersAndJobs, boolean replace)
            throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void scheduleJob(JobDetail jobDetail, Set<? extends Trigger> triggersForJob, boolean replace)
            throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean unscheduleJob(TriggerKey triggerKey) throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean unscheduleJobs(List<TriggerKey> triggerKeys) throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Date rescheduleJob(TriggerKey triggerKey, Trigger newTrigger) throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addJob(JobDetail jobDetail, boolean replace) throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addJob(JobDetail jobDetail, boolean replace, boolean storeNonDurableWhileAwaitingScheduling)
            throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean deleteJob(JobKey jobKey) throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean deleteJobs(List<JobKey> jobKeys) throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void triggerJob(JobKey jobKey) throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void triggerJob(JobKey jobKey, JobDataMap data) throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void pauseJob(JobKey jobKey) throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void pauseJobs(GroupMatcher<JobKey> matcher) throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void pauseTrigger(TriggerKey triggerKey) throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void pauseTriggers(GroupMatcher<TriggerKey> matcher) throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void resumeJob(JobKey jobKey) throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void resumeJobs(GroupMatcher<JobKey> matcher) throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void resumeTrigger(TriggerKey triggerKey) throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void resumeTriggers(GroupMatcher<TriggerKey> matcher) throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void pauseAll() throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void resumeAll() throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getJobGroupNames() throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<JobKey> getJobKeys(GroupMatcher<JobKey> matcher) throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<? extends Trigger> getTriggersOfJob(JobKey jobKey) throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getTriggerGroupNames() throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<TriggerKey> getTriggerKeys(GroupMatcher<TriggerKey> matcher) throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getPausedTriggerGroups() throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public JobDetail getJobDetail(JobKey jobKey) throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Trigger getTrigger(TriggerKey triggerKey) throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public TriggerState getTriggerState(TriggerKey triggerKey) throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addCalendar(String calName, Calendar calendar, boolean replace, boolean updateTriggers)
            throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean deleteCalendar(String calName) throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Calendar getCalendar(String calName) throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getCalendarNames() throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean interrupt(JobKey jobKey) throws UnableToInterruptJobException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean interrupt(String fireInstanceId) throws UnableToInterruptJobException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean checkExists(JobKey jobKey) throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean checkExists(TriggerKey triggerKey) throws SchedulerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() throws SchedulerException {
        throw new UnsupportedOperationException();
    }
}
