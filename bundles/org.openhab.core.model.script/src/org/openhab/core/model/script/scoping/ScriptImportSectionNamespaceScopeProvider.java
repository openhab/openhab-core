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
    public static final QualifiedName LANG_RUNNABLE_IF = QualifiedName.create("java", "lang", "Runnable");
    public static final QualifiedName TIME_PACKAGE = QualifiedName.create("java", "time");
    public static final QualifiedName TIME_FORMAT_PACKAGE = QualifiedName.create("java", "time", "format");
    public static final QualifiedName TIME_TEMPORAL_PACKAGE = QualifiedName.create("java", "time", "temporal");
    public static final QualifiedName UTIL_CALLABLE_IF = QualifiedName.create("java", "util", "concurrent", "Callable");
    public static final QualifiedName UTIL_FUTURE_IF = QualifiedName.create("java", "util", "concurrent", "Future");
    public static final QualifiedName UTIL_LOCK_IF = QualifiedName.create("java", "util", "concurrent", "locks",
            "Lock");
    public static final QualifiedName UTIL_REENTRANTLOCK_CLASS = QualifiedName.create("java", "util", "concurrent",
            "locks", "ReentrantLock");
    public static final QualifiedName UTIL_REENTRANTREADWRITELOCK_CLASS = QualifiedName.create("java", "util",
            "concurrent", "locks", "ReentrantReadWriteLock");
    public static final QualifiedName UTIL_REGEX_PACKAGE = QualifiedName.create("java", "util", "regex");
    public static final QualifiedName UTIL_ARRAYLIST_CLASS = QualifiedName.create("java", "util", "ArrayList");
    public static final QualifiedName UTIL_COLLECTION_IF = QualifiedName.create("java", "util", "Collection");
    public static final QualifiedName UTIL_HASHMAP_CLASS = QualifiedName.create("java", "util", "HashMap");
    public static final QualifiedName UTIL_HASHSET_CLASS = QualifiedName.create("java", "util", "HashSet");
    public static final QualifiedName UTIL_LINKEDHASHMAP_CLASS = QualifiedName.create("java", "util", "LinkedHashMap");
    public static final QualifiedName UTIL_LIST_IF = QualifiedName.create("java", "util", "List");
    public static final QualifiedName UTIL_LOCALE_CLASS = QualifiedName.create("java", "util", "Locale");
    public static final QualifiedName UTIL_MAP_IF = QualifiedName.create("java", "util", "Map");
    public static final QualifiedName UTIL_SET_IF = QualifiedName.create("java", "util", "Set");
    public static final QualifiedName UTIL_TIMEZONE_CLASS = QualifiedName.create("java", "util", "TimeZone");
    public static final QualifiedName UTIL_TREEMAP_CLASS = QualifiedName.create("java", "util", "TreeMap");
    public static final QualifiedName QUANTITY_PACKAGE = QualifiedName.create("javax", "measure", "quantity");
    public static final QualifiedName CHANNELS_CLASS = QualifiedName.create("org", "openhab", "core", "model", "script",
            "lib", "Channels");
    public static final QualifiedName ITEMS_CLASS = QualifiedName.create("org", "openhab", "core", "model", "script",
            "lib", "Items");
    public static final QualifiedName RULES_CLASS = QualifiedName.create("org", "openhab", "core", "model", "script",
            "lib", "Rules");

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
        implicitImports.add(doCreateImportNormalizer(LANG_RUNNABLE_IF, false, false));
        implicitImports.add(doCreateImportNormalizer(TIME_PACKAGE, true, false));
        implicitImports.add(doCreateImportNormalizer(TIME_FORMAT_PACKAGE, true, false));
        implicitImports.add(doCreateImportNormalizer(TIME_TEMPORAL_PACKAGE, true, false));
        implicitImports.add(doCreateImportNormalizer(UTIL_CALLABLE_IF, false, false));
        implicitImports.add(doCreateImportNormalizer(UTIL_FUTURE_IF, false, false));
        implicitImports.add(doCreateImportNormalizer(UTIL_LOCK_IF, false, false));
        implicitImports.add(doCreateImportNormalizer(UTIL_REENTRANTLOCK_CLASS, false, false));
        implicitImports.add(doCreateImportNormalizer(UTIL_REENTRANTREADWRITELOCK_CLASS, false, false));
        implicitImports.add(doCreateImportNormalizer(UTIL_REGEX_PACKAGE, true, false));
        implicitImports.add(doCreateImportNormalizer(UTIL_ARRAYLIST_CLASS, false, false));
        implicitImports.add(doCreateImportNormalizer(UTIL_COLLECTION_IF, false, false));
        implicitImports.add(doCreateImportNormalizer(UTIL_HASHMAP_CLASS, false, false));
        implicitImports.add(doCreateImportNormalizer(UTIL_HASHSET_CLASS, false, false));
        implicitImports.add(doCreateImportNormalizer(UTIL_LINKEDHASHMAP_CLASS, false, false));
        implicitImports.add(doCreateImportNormalizer(UTIL_LIST_IF, false, false));
        implicitImports.add(doCreateImportNormalizer(UTIL_LOCALE_CLASS, false, false));
        implicitImports.add(doCreateImportNormalizer(UTIL_MAP_IF, false, false));
        implicitImports.add(doCreateImportNormalizer(UTIL_SET_IF, false, false));
        implicitImports.add(doCreateImportNormalizer(UTIL_TIMEZONE_CLASS, false, false));
        implicitImports.add(doCreateImportNormalizer(UTIL_TREEMAP_CLASS, false, false));
        implicitImports.add(doCreateImportNormalizer(QUANTITY_PACKAGE, true, false));
        implicitImports.add(doCreateImportNormalizer(CHANNELS_CLASS, false, false));
        implicitImports.add(doCreateImportNormalizer(ITEMS_CLASS, false, false));
        implicitImports.add(doCreateImportNormalizer(RULES_CLASS, false, false));
        return implicitImports;
    }
}
