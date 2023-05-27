/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
@Grab('com.xlson.groovycsv:groovycsv:1.1')
import static com.xlson.groovycsv.CsvParser.parseCsv
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Year
import java.util.stream.Collectors


baseDir = Paths.get(getClass().protectionDomain.codeSource.location.toURI()).getParent().getParent().toAbsolutePath()
header = header()

def tagSets = new TreeMap<String, String>()
def locations = new TreeSet<String>()
def equipments = new TreeSet<String>()
def points = new TreeSet<String>()
def properties = new TreeSet<String>()

def labelsFile = new FileWriter("${baseDir}/src/main/resources/tags.properties")
labelsFile.write("# Generated content - do not edit!\n")

for (line in parseCsv(new FileReader("${baseDir}/model/SemanticTags.csv"), separator: ',')) {
    println "Processing Tag $line.Tag"

    def tagSet = (line.Parent ? tagSets.get(line.Parent) : line.Type) + "_" + line.Tag
    tagSets.put(line.Tag,tagSet)

    createTagSetClass(line, tagSet)
    appendLabelsFile(labelsFile, line, tagSet)

    switch(line.Type) {
        case "Location"            : locations.add(line.Tag); break;
        case "Equipment"           : equipments.add(line.Tag); break;
        case "Point"               : points.add(line.Tag); break;
        case "Property"            : properties.add(line.Tag); break;
        default : println "Unrecognized type " + line.Type
    }
}

labelsFile.close()

createLocationsFile(locations)
createEquipmentsFile(equipments)
createPointsFile(points)
createPropertiesFile(properties)
createDefaultProviderFile(tagSets)

println "\n\nTagSets:"
for (String tagSet : tagSets) {
    println tagSet
}

def createTagSetClass(def line, String tagSet) {
    def tag = line.Tag
    def type = line.Type
    def label = line.Label
    def synonyms = line.Synonyms
    def desc = line.Description
    def parent = line.Parent
    def parentClass = parent ? parent : type
    def pkg = type.toLowerCase()
    def ch = label.toLowerCase().charAt(0)
    def article = ch == 'a' || ch == 'e' || ch == 'i' || ch == 'o' || ch == 'u' ? "an" : "a"
    def file = new FileWriter("${baseDir}/src/main/java/org/openhab/core/semantics/model/${pkg}/${tag}.java")
    file.write(header)
    file.write("package org.openhab.core.semantics.model." + pkg + ";\n\n")
    file.write("import org.eclipse.jdt.annotation.NonNullByDefault;\n")
    if (!parent) {
            file.write("import org.openhab.core.semantics." + type + ";\n")
    }
    file.write("""import org.openhab.core.semantics.TagInfo;

/**
 * This class defines ${article} ${label}.
 *
 * @author Generated from generateTagClasses.groovy - Initial contribution
 */
@NonNullByDefault
@TagInfo(id = "${tagSet}")
public interface ${tag} extends ${parentClass} {
}
""")
    file.close()
}

def appendLabelsFile(FileWriter file, def line, String tagSet) {
    file.write(tagSet + "=" + line.Label)
    if (line.Synonyms) {
        file.write("," + line.Synonyms.replaceAll(", ", ","))
    }
    file.write("\n")
}

def createLocationsFile(Set<String> locations) {
    def file = new FileWriter("${baseDir}/src/main/java/org/openhab/core/semantics/model/location/Locations.java")
    file.write(header)
    file.write("""package org.openhab.core.semantics.model.location;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.semantics.Location;

/**
 * This class provides a stream of all defined locations.
 *
 * @author Generated from generateTagClasses.groovy - Initial contribution
 */
@NonNullByDefault
public class Locations {

    static final Set<Class<? extends Location>> LOCATIONS = ConcurrentHashMap.newKeySet();

    static {
        LOCATIONS.add(Location.class);
""")    
    for (String location : locations) {
        file.write("        LOCATIONS.add(${location}.class);\n")
    }
    file.write("""    }

    public static Stream<Class<? extends Location>> stream() {
        return LOCATIONS.stream();
    }

    public static boolean add(Class<? extends Location> tag) {
        return LOCATIONS.add(tag);
    }

    public static boolean remove(Class<? extends Location> tag) {
        return LOCATIONS.remove(tag);
    }
}
""")
    file.close()
}

def createEquipmentsFile(Set<String> equipments) {
    def file = new FileWriter("${baseDir}/src/main/java/org/openhab/core/semantics/model/equipment/Equipments.java")
    file.write(header)
    file.write("""package org.openhab.core.semantics.model.equipment;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.semantics.Equipment;

/**
 * This class provides a stream of all defined equipments.
 *
 * @author Generated from generateTagClasses.groovy - Initial contribution
 */
@NonNullByDefault
public class Equipments {

    static final Set<Class<? extends Equipment>> EQUIPMENTS = ConcurrentHashMap.newKeySet();

    static {
        EQUIPMENTS.add(Equipment.class);
""")    
    for (String equipment : equipments) {
        file.write("        EQUIPMENTS.add(${equipment}.class);\n")
    }
    file.write("""    }

    public static Stream<Class<? extends Equipment>> stream() {
        return EQUIPMENTS.stream();
    }

    public static boolean add(Class<? extends Equipment> tag) {
        return EQUIPMENTS.add(tag);
    }

    public static boolean remove(Class<? extends Equipment> tag) {
        return EQUIPMENTS.remove(tag);
    }
}
""")
    file.close()
}

def createPointsFile(Set<String> points) {
    def file = new FileWriter("${baseDir}/src/main/java/org/openhab/core/semantics/model/point/Points.java")
    file.write(header)
    file.write("""package org.openhab.core.semantics.model.point;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.semantics.Point;

/**
 * This class provides a stream of all defined points.
 *
 * @author Generated from generateTagClasses.groovy - Initial contribution
 */
@NonNullByDefault
public class Points {

    static final Set<Class<? extends Point>> POINTS = ConcurrentHashMap.newKeySet();

    static {
        POINTS.add(Point.class);
""")    
    for (String point : points) {
        file.write("        POINTS.add(${point}.class);\n")
    }
    file.write("""    }

    public static Stream<Class<? extends Point>> stream() {
        return POINTS.stream();
    }

    public static boolean add(Class<? extends Point> tag) {
        return POINTS.add(tag);
    }

    public static boolean remove(Class<? extends Point> tag) {
        return POINTS.remove(tag);
    }
}
""")
    file.close()
}

def createPropertiesFile(Set<String> properties) {
    def file = new FileWriter("${baseDir}/src/main/java/org/openhab/core/semantics/model/property/Properties.java")
    file.write(header)
    file.write("""package org.openhab.core.semantics.model.property;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.semantics.Property;

/**
 * This class provides a stream of all defined properties.
 *
 * @author Generated from generateTagClasses.groovy - Initial contribution
 */
@NonNullByDefault
public class Properties {

    static final Set<Class<? extends Property>> PROPERTIES = ConcurrentHashMap.newKeySet();

    static {
        PROPERTIES.add(Property.class);
""")    
    for (String property : properties) {
        file.write("        PROPERTIES.add(${property}.class);\n")
    }
    file.write("""    }

    public static Stream<Class<? extends Property>> stream() {
        return PROPERTIES.stream();
    }

    public static boolean add(Class<? extends Property> tag) {
        return PROPERTIES.add(tag);
    }

    public static boolean remove(Class<? extends Property> tag) {
        return PROPERTIES.remove(tag);
    }
}
""")
    file.close()
}

def createDefaultProviderFile(def tagSets) {
    def file = new FileWriter("${baseDir}/src/main/java/org/openhab/core/semantics/model/DefaultSemanticTagProvider.java")
    file.write(header)
    file.write("""package org.openhab.core.semantics.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.semantics.SemanticTag;
import org.openhab.core.semantics.SemanticTagImpl;
import org.openhab.core.semantics.SemanticTagProvider;
import org.osgi.service.component.annotations.Component;

/**
 * This class defines a provider of all default semantic tags.
 *
 * @author Generated from generateTagClasses.groovy - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { SemanticTagProvider.class, DefaultSemanticTagProvider.class })
public class DefaultSemanticTagProvider implements SemanticTagProvider {

    private List<SemanticTag> defaultTags;

    public DefaultSemanticTagProvider() {
        this.defaultTags = new ArrayList<>();
        defaultTags.add(new SemanticTagImpl("Equipment", "", "", ""));
        defaultTags.add(new SemanticTagImpl("Location", "", "", ""));
        defaultTags.add(new SemanticTagImpl("Point", "", "", ""));
        defaultTags.add(new SemanticTagImpl("Property", "", "", ""));
""")    
    for (line in parseCsv(new FileReader("${baseDir}/model/SemanticTags.csv"), separator: ',')) {
        def tagId = (line.Parent ? tagSets.get(line.Parent) : line.Type) + "_" + line.Tag
        file.write("""        defaultTags.add(new SemanticTagImpl("${tagId}", //
                "${line.Label}", "${line.Description}", "${line.Synonyms}"));
""")
    }
    file.write("""    }

    @Override
    public Collection<SemanticTag> getAll() {
        return defaultTags;
    }

    @Override
    public void addProviderChangeListener(ProviderChangeListener<SemanticTag> listener) {
    }

    @Override
    public void removeProviderChangeListener(ProviderChangeListener<SemanticTag> listener) {
    }
}
""")
    file.close()
}

def header() {
    def headerPath = baseDir.resolve("../../licenses/epl-2.0/header.txt")
    def year = String.valueOf(Year.now().getValue())

    def headerLines = Files.readAllLines(headerPath)

    headerLines = headerLines.stream().map(line -> {
        line.isBlank() ? " *" : " * " + line.replace("\${year}", year)
    }).collect(Collectors.toList())

    headerLines.add(0, "/**")
    headerLines.add(" */")
    headerLines.add("")

    headerLines.stream().collect(Collectors.joining(System.lineSeparator()))
}
