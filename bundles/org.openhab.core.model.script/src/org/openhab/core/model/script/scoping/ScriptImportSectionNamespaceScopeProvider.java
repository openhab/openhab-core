/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.model.script.scoping;

import java.util.List;

import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.scoping.impl.ImportNormalizer;
import org.eclipse.xtext.xbase.scoping.XImportSectionNamespaceScopeProvider;

/**
 * @author Oliver Libutzki - Initial contribution
 */
public class ScriptImportSectionNamespaceScopeProvider extends XImportSectionNamespaceScopeProvider {

    public static final QualifiedName CORE_LIBRARY_UNITS_PACKAGE = QualifiedName.create("org", "openhab", "core",
            "library", "unit");
    public static final QualifiedName CORE_LIBRARY_TYPES_PACKAGE = QualifiedName.create("org", "openhab", "core",
            "library", "types");
    public static final QualifiedName CORE_LIBRARY_ITEMS_PACKAGE = QualifiedName.create("org", "openhab", "core",
            "library", "items");
    public static final QualifiedName CORE_TYPES_TIMESERIES_CLASS = QualifiedName.create("org", "openhab", "core",
            "types", "TimeSeries");
    public static final QualifiedName CORE_ITEMS_PACKAGE = QualifiedName.create("org", "openhab", "core", "items");
    public static final QualifiedName CORE_THING_PACKAGE = QualifiedName.create("org", "openhab", "core", "thing");
    public static final QualifiedName CORE_THING_LINK_ITEMCHANNELLINK_CLASS = QualifiedName.create("org", "openhab",
            "core", "thing", "link", "ItemChannelLink");
    public static final QualifiedName CORE_THING_LINK_ITEMCHANNELLINKREGISTRY_CLASS = QualifiedName.create("org",
            "openhab", "core", "thing", "link", "ItemChannelLinkRegistry");
    public static final QualifiedName CORE_PERSISTENCE_PACKAGE = QualifiedName.create("org", "openhab", "core",
            "persistence");
    public static final QualifiedName CORE_PERSISTENCE_RIEMANNTYPE_CLASS = QualifiedName.create("org", "openhab",
            "core", "persistence", "extensions", "PersistenceExtensions", "RiemannType");
    public static final QualifiedName MODEL_SCRIPT_ACTIONS_PACKAGE = QualifiedName.create("org", "openhab", "core",
            "model", "script", "actions");
    public static final QualifiedName TIME_PACKAGE = QualifiedName.create("java", "time");
    public static final QualifiedName TIME_FORMAT_PACKAGE = QualifiedName.create("java", "time", "format");
    public static final QualifiedName TIME_TEMPORAL_PACKAGE = QualifiedName.create("java", "time", "temporal");
    public static final QualifiedName UTIL_REGEX_PACKAGE = QualifiedName.create("java", "util", "regex");
    public static final QualifiedName QUANTITY_PACKAGE = QualifiedName.create("javax", "measure", "quantity");
    public static final QualifiedName CHANNELS_CLASS = QualifiedName.create("org", "openhab", "core", "model", "script",
            "helper", "Channels");
    public static final QualifiedName ITEMS_CLASS = QualifiedName.create("org", "openhab", "core", "model", "script",
            "helper", "Items");
    public static final QualifiedName RULES_CLASS = QualifiedName.create("org", "openhab", "core", "model", "script",
            "helper", "Rules");

    @Override
    protected List<ImportNormalizer> getImplicitImports(boolean ignoreCase) {
        List<ImportNormalizer> implicitImports = super.getImplicitImports(ignoreCase);
        implicitImports.add(doCreateImportNormalizer(CORE_LIBRARY_UNITS_PACKAGE, true, false));
        implicitImports.add(doCreateImportNormalizer(CORE_LIBRARY_TYPES_PACKAGE, true, false));
        implicitImports.add(doCreateImportNormalizer(CORE_LIBRARY_ITEMS_PACKAGE, true, false));
        implicitImports.add(doCreateImportNormalizer(CORE_TYPES_TIMESERIES_CLASS, false, false));
        implicitImports.add(doCreateImportNormalizer(CORE_ITEMS_PACKAGE, true, false));
        implicitImports.add(doCreateImportNormalizer(CORE_THING_PACKAGE, true, false));
        implicitImports.add(doCreateImportNormalizer(CORE_THING_LINK_ITEMCHANNELLINK_CLASS, false, false));
        implicitImports.add(doCreateImportNormalizer(CORE_THING_LINK_ITEMCHANNELLINKREGISTRY_CLASS, false, false));
        implicitImports.add(doCreateImportNormalizer(CORE_PERSISTENCE_PACKAGE, true, false));
        implicitImports.add(doCreateImportNormalizer(CORE_PERSISTENCE_RIEMANNTYPE_CLASS, false, false));
        implicitImports.add(doCreateImportNormalizer(MODEL_SCRIPT_ACTIONS_PACKAGE, true, false));
        implicitImports.add(doCreateImportNormalizer(TIME_PACKAGE, true, false));
        implicitImports.add(doCreateImportNormalizer(TIME_FORMAT_PACKAGE, true, false));
        implicitImports.add(doCreateImportNormalizer(TIME_TEMPORAL_PACKAGE, true, false));
        implicitImports.add(doCreateImportNormalizer(UTIL_REGEX_PACKAGE, true, false));
        implicitImports.add(doCreateImportNormalizer(QUANTITY_PACKAGE, true, false));
        implicitImports.add(doCreateImportNormalizer(CHANNELS_CLASS, false, false));
        implicitImports.add(doCreateImportNormalizer(ITEMS_CLASS, false, false));
        implicitImports.add(doCreateImportNormalizer(RULES_CLASS, false, false));
        return implicitImports;
    }
}
