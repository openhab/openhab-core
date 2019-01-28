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
package org.eclipse.smarthome.model.script.scoping;

import java.util.List;

import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.scoping.impl.ImportNormalizer;
import org.eclipse.xtext.xbase.scoping.XImportSectionNamespaceScopeProvider;

public class ScriptImportSectionNamespaceScopeProvider extends XImportSectionNamespaceScopeProvider {

    public static final QualifiedName CORE_LIBRARY_TYPES_PACKAGE = QualifiedName.create("org", "eclipse", "smarthome",
            "core", "library", "types");
    public static final QualifiedName CORE_LIBRARY_ITEMS_PACKAGE = QualifiedName.create("org", "eclipse", "smarthome",
            "core", "library", "items");
    public static final QualifiedName CORE_ITEMS_PACKAGE = QualifiedName.create("org", "eclipse", "smarthome", "core",
            "items");
    public static final QualifiedName CORE_PERSISTENCE_PACKAGE = QualifiedName.create("org", "eclipse", "smarthome",
            "core", "persistence");
    public static final QualifiedName MODEL_SCRIPT_ACTIONS_PACKAGE = QualifiedName.create("org", "eclipse", "smarthome",
            "model", "script", "actions");
    public static final QualifiedName JODA_TIME_PACKAGE = QualifiedName.create("org", "joda", "time");

    @Override
    protected List<ImportNormalizer> getImplicitImports(boolean ignoreCase) {
        List<ImportNormalizer> implicitImports = super.getImplicitImports(ignoreCase);
        implicitImports.add(doCreateImportNormalizer(CORE_LIBRARY_TYPES_PACKAGE, true, false));
        implicitImports.add(doCreateImportNormalizer(CORE_LIBRARY_ITEMS_PACKAGE, true, false));
        implicitImports.add(doCreateImportNormalizer(CORE_ITEMS_PACKAGE, true, false));
        implicitImports.add(doCreateImportNormalizer(CORE_PERSISTENCE_PACKAGE, true, false));
        implicitImports.add(doCreateImportNormalizer(MODEL_SCRIPT_ACTIONS_PACKAGE, true, false));
        implicitImports.add(doCreateImportNormalizer(JODA_TIME_PACKAGE, true, false));
        return implicitImports;
    }

}
