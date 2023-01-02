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
package org.openhab.core.io.transport.modbus.test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * @author Sami Salonen - Initial contribution
 *
 * @param <T>
 */
@NonNullByDefault
public class ResultCaptor<T> implements Answer<T> {

    private List<T> results = new ArrayList<>();
    private LongSupplier longSupplier;

    public ResultCaptor(LongSupplier longSupplier) {
        this.longSupplier = longSupplier;
    }

    public List<T> getAllReturnValues() {
        return results;
    }

    @SuppressWarnings("unchecked")
    @Override
    public @Nullable T answer(InvocationOnMock invocationOnMock) throws Throwable {
        T result = (T) invocationOnMock.callRealMethod();
        synchronized (this.results) {
            results.add(result);
        }
        long wait = longSupplier.getAsLong();
        if (wait > 0) {
            Thread.sleep(wait);
        }
        return result;
    }
}
