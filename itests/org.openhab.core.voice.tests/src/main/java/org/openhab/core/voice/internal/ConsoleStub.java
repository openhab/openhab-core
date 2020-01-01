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
package org.openhab.core.voice.internal;

import org.openhab.core.io.console.Console;

/**
 * A {@link Console} stub used for the tests. In order to keep it as simple as
 * possible, when some text is passed to the console for printing, it is saved
 * in an instance variable. In addition, when the {@link #printUsage(String)}
 * method is called, the corresponding boolean variable is set to true.
 *
 * @author Mihaela Memova - Initial contribution
 * @author Velin Yordanov - migrated from groovy to java
 */
public class ConsoleStub implements Console {

    private String printedText;
    private boolean isPrintUsagesMethodCalled;

    @Override
    public void print(String s) {
        printedText = s;
    }

    @Override
    public void println(String s) {
        printedText = s;
    }

    @Override
    public void printUsage(String s) {
        isPrintUsagesMethodCalled = true;
    }

    public String getPrintedText() {
        return printedText;
    }

    public boolean isPrintUsagesMethodCalled() {
        return isPrintUsagesMethodCalled;
    }

}
