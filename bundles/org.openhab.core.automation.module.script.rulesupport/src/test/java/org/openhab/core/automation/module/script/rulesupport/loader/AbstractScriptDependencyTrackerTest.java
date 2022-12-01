/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.core.automation.module.script.rulesupport.loader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.nio.file.Path;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openhab.core.automation.module.script.ScriptDependencyTracker;
import org.openhab.core.service.AbstractWatchService;

/**
 * The {@link AbstractScriptDependencyTrackerTest} contains tests for the {@link AbstractScriptDependencyTracker}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class AbstractScriptDependencyTrackerTest {

    private static final String WATCH_DIR = "test";

    private @Nullable AbstractWatchService dependencyWatchService;

    private @NonNullByDefault({}) AbstractScriptDependencyTracker scriptDependencyTracker;

    @BeforeEach
    public void setup() {
        scriptDependencyTracker = new AbstractScriptDependencyTracker(WATCH_DIR) {

            @Override
            protected AbstractWatchService createDependencyWatchService() {
                AbstractWatchService dependencyWatchService = Mockito.spy(super.createDependencyWatchService());
                AbstractScriptDependencyTrackerTest.this.dependencyWatchService = dependencyWatchService;
                return dependencyWatchService;
            }

            @Override
            public void dependencyChanged(String dependency) {
                super.dependencyChanged(dependency);
            }
        };

        scriptDependencyTracker.activate();
    }

    @AfterEach
    public void tearDown() {
        scriptDependencyTracker.deactivate();
    }

    @Test
    public void testScriptLibraryWatcherIsCreatedAndActivated() {
        assertThat(dependencyWatchService, is(notNullValue()));

        assertThat(dependencyWatchService.getSourcePath(), is(Path.of(WATCH_DIR)));

        verify(dependencyWatchService).activate();
    }

    @Test
    public void testScriptLibraryWatchersIsDeactivatedOnShutdown() {
        scriptDependencyTracker.deactivate();

        verify(dependencyWatchService).deactivate();
    }

    @Test
    public void testDependencyChangeIsForwardedToMultipleListeners() {
        ScriptDependencyTracker.Listener listener1 = mock(ScriptDependencyTracker.Listener.class);
        ScriptDependencyTracker.Listener listener2 = mock(ScriptDependencyTracker.Listener.class);

        scriptDependencyTracker.addChangeTracker(listener1);
        scriptDependencyTracker.addChangeTracker(listener2);

        scriptDependencyTracker.startTracking("scriptId", "depPath");
        scriptDependencyTracker.dependencyChanged("depPath");

        verify(listener1).onDependencyChange(eq("scriptId"));
        verify(listener2).onDependencyChange(eq("scriptId"));
        verifyNoMoreInteractions(listener1);
        verifyNoMoreInteractions(listener2);
    }

    @Test
    public void testDependencyChangeIsForwardedForMultipleScriptIds() {
        ScriptDependencyTracker.Listener listener = mock(ScriptDependencyTracker.Listener.class);

        scriptDependencyTracker.addChangeTracker(listener);

        scriptDependencyTracker.startTracking("scriptId1", "depPath");
        scriptDependencyTracker.startTracking("scriptId2", "depPath");

        scriptDependencyTracker.dependencyChanged("depPath");

        verify(listener).onDependencyChange(eq("scriptId1"));
        verify(listener).onDependencyChange(eq("scriptId2"));
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testDependencyChangeIsForwardedForMultipleDependencies() {
        ScriptDependencyTracker.Listener listener = mock(ScriptDependencyTracker.Listener.class);

        scriptDependencyTracker.addChangeTracker(listener);

        scriptDependencyTracker.startTracking("scriptId", "depPath1");
        scriptDependencyTracker.startTracking("scriptId", "depPath2");

        scriptDependencyTracker.dependencyChanged("depPath1");
        scriptDependencyTracker.dependencyChanged("depPath2");

        verify(listener, times(2)).onDependencyChange(eq("scriptId"));
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testDependencyChangeIsForwardedForCorrectDependencies() {
        ScriptDependencyTracker.Listener listener = mock(ScriptDependencyTracker.Listener.class);

        scriptDependencyTracker.addChangeTracker(listener);

        scriptDependencyTracker.startTracking("scriptId1", "depPath1");
        scriptDependencyTracker.startTracking("scriptId2", "depPath2");

        scriptDependencyTracker.dependencyChanged("depPath1");

        verify(listener).onDependencyChange(eq("scriptId1"));
        verifyNoMoreInteractions(listener);
    }
}
