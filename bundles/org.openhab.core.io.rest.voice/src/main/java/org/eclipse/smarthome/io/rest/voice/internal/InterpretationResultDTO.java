/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.io.rest.voice.internal;

import org.eclipse.smarthome.core.voice.chat.Card;
import org.eclipse.smarthome.core.voice.text.Intent;
import org.eclipse.smarthome.core.voice.text.InterpretationResult;

/**
 * A DTO that is used on the REST API to provide infos about {@link InterpretationResult} to UIs.
 *
 * @author Laurent Garnier - Initial contribution
 */
public class InterpretationResultDTO {

    String language;
    String answer;
    String hint;
    Intent intent;
    String[] matchedItemNames;
    Card card;
}
