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
package org.openhab.core.thing.xml.internal;

import org.openhab.core.config.xml.util.GenericUnmarshaller;
import org.openhab.core.config.xml.util.NodeIterator;
import org.openhab.core.config.xml.util.NodeList;
import org.openhab.core.config.xml.util.NodeValue;
import org.openhab.core.types.CommandDescription;
import org.openhab.core.types.CommandDescriptionBuilder;
import org.openhab.core.types.CommandOption;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * The {@link CommandDescriptionConverter} is a concrete implementation of the {@code XStream} {@link Converter}
 * interface used to convert a command description within an XML document into a {@link CommandDescription} object.
 * <p>
 * This converter converts {@code command} XML tags.
 *
 * @author Henning Treu - Initial contribution
 */
public class CommandDescriptionConverter extends GenericUnmarshaller<CommandDescription> {

    public CommandDescriptionConverter() {
        super(CommandDescription.class);
    }

    @Override
    public final CommandDescription unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        NodeList nodes = (NodeList) context.convertAnother(context, NodeList.class);
        NodeIterator nodeIterator = new NodeIterator(nodes.getList());

        NodeList commandOptionsNode = (NodeList) nodeIterator.next();
        if (commandOptionsNode != null) {
            if ("options".equals(commandOptionsNode.getNodeName())) {
                CommandDescriptionBuilder commandDescriptionBuilder = CommandDescriptionBuilder.create();
                for (Object coNodeObject : commandOptionsNode.getList()) {
                    NodeValue optionsNode = (NodeValue) coNodeObject;

                    if ("option".equals(optionsNode.getNodeName())) {
                        String name = (String) optionsNode.getValue();
                        String command = optionsNode.getAttributes().get("value");

                        if (name != null && command != null) {
                            commandDescriptionBuilder.withCommandOption(new CommandOption(command, name));
                        }
                    } else {
                        throw new ConversionException("The 'options' node must only contain 'option' nodes!");
                    }
                }

                nodeIterator.assertEndOfType();
                return commandDescriptionBuilder.build();
            }
        }

        nodeIterator.assertEndOfType();
        return null;
    }

}
