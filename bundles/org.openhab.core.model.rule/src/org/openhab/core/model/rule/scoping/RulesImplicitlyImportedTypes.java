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
package org.openhab.core.model.rule.scoping;

import org.openhab.core.model.script.scoping.ScriptImplicitlyImportedTypes;

import com.google.inject.Singleton;

/**
 * This class registers all statically available functions as well as the
 * extensions for specific jvm types, which should only be available in rules,
 * but not in scripts
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Oliver Libutzki - Xtext 2.5.0 migration
 */

@Singleton
public class RulesImplicitlyImportedTypes extends ScriptImplicitlyImportedTypes {

}
