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
package org.openhab.core.model.thing.serializer;

import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.serializer.analysis.ISyntacticSequencerPDAProvider.ISynNavigable;

/**
 *
 * @author Alex Tugarev - Initial contribution
 */
@SuppressWarnings("restriction")
public class ThingSyntacticSequencerExtension extends ThingSyntacticSequencer {

    @Override
    protected void emit_ModelThing_ThingKeyword_0_q(EObject semanticObject, ISynNavigable transition, List<INode> nodes) {
        ILeafNode node = nodes != null && nodes.size() == 1 && nodes.get(0) instanceof ILeafNode ? (ILeafNode) nodes
                .get(0) : null;
        Keyword keyword = grammarAccess.getModelThingAccess().getThingKeyword_0();
        acceptUnassignedKeyword(keyword, keyword.getValue(), node);
    }

}
