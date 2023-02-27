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
package org.openhab.core.cache.lru;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test the wrapper stream in the cache system
 *
 * @author Gwendal Roulleau - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class InputStreamCacheWrapperTest {

    private @Mock @NonNullByDefault({}) LRUMediaCacheEntry<?> cacheEntry;

    @Test
    public void cacheWrapperStreamReadTest() throws IOException {
        when(cacheEntry.read(0, 1)).thenReturn(new byte[] { 1 });
        when(cacheEntry.read(1, 1)).thenReturn(new byte[] { 2 });
        when(cacheEntry.read(2, 1)).thenReturn(new byte[] { 3 });
        when(cacheEntry.read(3, 1)).thenReturn(new byte[0]);

        try (InputStreamCacheWrapper audioStreamCacheWrapper = new InputStreamCacheWrapper(cacheEntry)) {
            assertEquals(1, audioStreamCacheWrapper.read());
            assertEquals(2, audioStreamCacheWrapper.read());
            assertEquals(3, audioStreamCacheWrapper.read());
            assertEquals(-1, audioStreamCacheWrapper.read());
        }

        verify(cacheEntry, times(4)).read(anyInt(), anyInt());
        verify(cacheEntry).closeStreamClient();
        verifyNoMoreInteractions(cacheEntry);
    }

    @Test
    public void cacheWrapperStreamReadBunchTest() throws IOException {
        when(cacheEntry.read(anyInt(), anyInt())).thenReturn(new byte[] { 1 }, new byte[] { 2, 3 }, new byte[0]);

        try (InputStreamCacheWrapper audioStreamCacheWrapper = new InputStreamCacheWrapper(cacheEntry)) {
            assertArrayEquals(new byte[] { 1, 2, 3 }, audioStreamCacheWrapper.readAllBytes());
        }
        verify(cacheEntry, times(3)).read(anyInt(), anyInt());
        verify(cacheEntry).closeStreamClient();
        verifyNoMoreInteractions(cacheEntry);
    }
}
