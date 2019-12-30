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
package org.openhab.core.model.persistence.scoping;

import org.openhab.core.model.persistence.persistence.Strategy;
import org.openhab.core.model.persistence.persistence.impl.StrategyImpl;

/**
 * This class defines a few persistence strategies that are globally available to
 * all persistence models.
 *
 * @author Kai Kreuzer - Initial contribution
 */
public class GlobalStrategies {

    static final public Strategy UPDATE = new StrategyImpl() {
        @Override
        public String getName() {
            return "everyUpdate";
        };
    };

    static final public Strategy CHANGE = new StrategyImpl() {
        @Override
        public String getName() {
            return "everyChange";
        };
    };

    static final public Strategy RESTORE = new StrategyImpl() {
        @Override
        public String getName() {
            return "restoreOnStartup";
        };
    };
}
