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
package org.openhab.core.automation.module.script.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.automation.module.script.AbstractScriptEngineFactory;
import org.openhab.core.automation.module.script.ScriptEngineFactory;
import org.osgi.service.component.annotations.Component;

/**
 * An implementation of {@link ScriptEngineFactory} for ScriptEngines that do not require customizations.
 *
 * @author Simon Merschjohann - Initial contribution
 * @author Scott Rushworth - added service and removed methods now inherited from AbstractScriptEngineFactory
 */
@NonNullByDefault
@Component(service = ScriptEngineFactory.class)
public class GenericScriptEngineFactory extends AbstractScriptEngineFactory {

}
