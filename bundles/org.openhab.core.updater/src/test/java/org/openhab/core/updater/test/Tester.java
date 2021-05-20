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
package org.openhab.core.updater.test;

import static org.junit.Assert.*;

import org.junit.jupiter.api.Test;
import org.openhab.core.updater.dto.StatusDTO;
import org.openhab.core.updater.updaterclasses.BaseUpdater;
import org.openhab.core.updater.updaterclasses.WindowsUpdater;

/**
 * Tests
 *
 * @author AndrewFG - Initial contribution
 */
class Tester {

    @Test
    void testDto() throws Exception {
        BaseUpdater uu = new WindowsUpdater();
        StatusDTO dto = uu.getStatusDTO();
        assertNotNull(dto);
        assertEquals("VERSION_NOT_DEFINED", dto.actualVersion.versionName);
        assertEquals(3, dto.latestVersionCount.intValue());
        assertEquals("SNAPSHOT", dto.latestVersions[2].versionType);
        assertEquals("no", dto.newVersionAvailable);
        assertEquals("UNKNOWN", dto.targetNewVersionType);
    }
}
