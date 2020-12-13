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
package org.openhab.core.model.thing.internal;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.core.common.registry.AbstractProvider;

/**
 * This class can be used as workaround if you want to create a sub-class of an abstract provider but you are not able
 * to annotate the generic type argument with a non-null annotation. This is for example the case if you implement the
 * class by Xtend.
 *
 * @author Markus Rathgeb - Initial contribution
 *
 * @param <E> type of the provided elements
 */
public abstract class AbstractProviderLazyNullness<E> extends AbstractProvider<@NonNull E> {

}
