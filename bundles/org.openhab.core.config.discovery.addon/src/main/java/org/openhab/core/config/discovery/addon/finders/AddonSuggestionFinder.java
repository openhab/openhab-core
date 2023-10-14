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
package org.openhab.core.config.discovery.addon.finders;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.addon.AddonInfo;

/**
 * This is a {@link AddonSuggestionFinder} interface for classes that find Addons that are suggested to be installed.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public interface AddonSuggestionFinder {

    /**
     * The framework calls this method to get a set of Addon UID strings (e.g. 'binding-hue') relating to Addons that
     * have been discovered via the {@code scanTask()} method.
     * <p>
     * The result is only expected to be valid if {@code scanDone()} has returned true.
     * 
     * @return a set of UID strings.
     */
    public Set<String> getAddonSuggestionUIDs();

    /**
     * The framework calls this method to reset the internal state of the finder.
     */
    public void reset();

    /**
     * The framework calls this method to check if {@code scanTask()} has completed.
     * 
     * @return true if {@code scanTask()} has completed.
     */
    public boolean scanDone();

    /**
     * The framework calls this method on a scheduler thread e.g. via
     * {@code Future<?> task = scheduler.submit(() -> scanTask())}
     * <p>
     * The task should scan through its candidate list of Addons, add those that it suggests to be installed to its
     * AddonSuggestionUIDs set, and finally set the result of {@code scanDone()} to true when the task has completed.
     * <p>
     * The task must be implemented so it can be externally cancelled via {@code task.cancel(true)}
     */
    public void scanTask();

    /**
     * The framework calls this method to provide a list of AddonInfo elements which contain potential Addon candidates
     * that this finder can iterate over in order to detect which ones to return via the getAddonSuggestionUIDs()
     * method.
     * <p>
     * It is expected that {@code getAddonSuggestionUIDs()} will return the UIDs of a filtered <u>subset</u> of the
     * Addons provided in this candidate list.
     * <p>
     * The framework will try to cancel any prior running {@code scanTask()} e.g. via {@code task.cancel(true)} before
     * calling {@code setAddonCandidates()} and it will start a new {@code scanTask()} afterwards.
     * 
     * @param candidates a list of AddonInfo candidates.
     */
    public void setAddonCandidates(List<AddonInfo> candidates);
}
