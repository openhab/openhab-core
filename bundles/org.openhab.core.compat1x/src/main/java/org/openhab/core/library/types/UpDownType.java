/**
 * Copyright (c) 2015-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.core.library.types;

import org.openhab.core.types.Command;
import org.openhab.core.types.PrimitiveType;
import org.openhab.core.types.State;

/**
 *
 * @author Kai Kreuzer - Initial contribution
 */
public enum UpDownType implements PrimitiveType, State, Command {
    UP,
    DOWN;

    @Override
    public String format(String pattern) {
        return String.format(pattern, this.toString());
    }

}
