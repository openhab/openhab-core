/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.core.automation.internal.parser.gson;

import java.io.OutputStreamWriter;
import java.util.Set;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.config.core.ConfigurationDeserializer;
import org.eclipse.smarthome.config.core.ConfigurationSerializer;
import org.openhab.core.automation.parser.Parser;
import org.openhab.core.automation.type.CompositeActionType;
import org.openhab.core.automation.type.CompositeConditionType;
import org.openhab.core.automation.type.CompositeTriggerType;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Abstract class that can be used by the parsers for the different entity types.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Ana Dimova - add Instance Creators
 *
 * @param <T> the type of the entities to parse
 */
public abstract class AbstractGSONParser<T> implements Parser<T> {

    // A Gson instance to use by the parsers
    protected static Gson gson = new GsonBuilder() //
            .registerTypeAdapter(CompositeActionType.class, new ActionInstanceCreator()) //
            .registerTypeAdapter(CompositeConditionType.class, new ConditionInstanceCreator()) //
            .registerTypeAdapter(CompositeTriggerType.class, new TriggerInstanceCreator()) //
            .registerTypeAdapter(Configuration.class, new ConfigurationDeserializer()) //
            .registerTypeAdapter(Configuration.class, new ConfigurationSerializer()) //
            .create();

    @Override
    public void serialize(Set<T> dataObjects, OutputStreamWriter writer) throws Exception {
        gson.toJson(dataObjects, writer);
    }
}
