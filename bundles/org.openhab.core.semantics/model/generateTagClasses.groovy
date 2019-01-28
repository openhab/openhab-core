/**
 * Copyright (c) 2014,2018 Contributors to the Eclipse Foundation
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
@Grab('com.xlson.groovycsv:groovycsv:1.1')
import static com.xlson.groovycsv.CsvParser.parseCsv

def tagSets = new TreeMap<String, String>()
def locations = new TreeSet<String>()
def equipments = new TreeSet<String>()
def points = new TreeSet<String>()
def properties = new TreeSet<String>()

def labelsFile = new FileWriter('../src/main/resources/tags.properties')
labelsFile.write("# Generated content - do not edit!\n")

for(line in parseCsv(new FileReader('SemanticTags.csv'), separator: ',')) {
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

println "\n\nTagSets:"
for(String tagSet : tagSets) {
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
    def file = new FileWriter("../src/main/java/org/eclipse/smarthome/core/semantics/model/" + pkg + "/" + tag + ".java")
    file.write(header())
    file.write("package org.eclipse.smarthome.core.semantics.model." + pkg + ";\n\n")
    if(!parent) {
            file.write("import org.eclipse.smarthome.core.semantics.model." + type + ";\n")
    }
    file.write("""import org.eclipse.smarthome.core.semantics.model.TagInfo;

/**
 * This class defines a ${label}.
 * 
 * @author Generated from generateTagClasses.groovy - Initial contribution
 *
 */
@TagInfo(id = "${tagSet}", label = "${label}", synonyms = "${synonyms}", description = "${desc}")
public interface ${tag} extends ${parentClass} {
}
""")
    file.close()
}

def appendLabelsFile(FileWriter file, def line, String tagSet) {
    file.write(tagSet + "=" + line.Label)
    if(line.Synonyms) {
        file.write("," + line.Synonyms.replaceAll(", ", ","))
    }
    file.write("\n")
}

def createLocationsFile(Set<String> locations) {
    def file = new FileWriter("../src/main/java/org/eclipse/smarthome/core/semantics/model/location/Locations.java")
    file.write(header())
    file.write("""package org.eclipse.smarthome.core.semantics.model.location;

import java.util.Set;
import java.util.HashSet;
import java.util.stream.Stream;

import org.eclipse.smarthome.core.semantics.model.Location;

/**
 * This class provides a stream of all defined locations.
 * 
 * @author Generated from generateTagClasses.groovy - Initial contribution
 *
 */
public class Locations {

    static final Set<Class<? extends Location>> LOCATIONS = new HashSet<>();

    static {
        LOCATIONS.add(Location.class);
""")    
    Iterator it = locations.iterator();
    while(it.hasNext() ) {
        String location = it.next();
        file.write("        LOCATIONS.add(" + location + ".class);\n")
    }
    file.write("""    }

    public static Stream<Class<? extends Location>> stream() {
        return LOCATIONS.stream();
    }
}
""")
    file.close()
}

def createEquipmentsFile(Set<String> equipments) {
    def file = new FileWriter("../src/main/java/org/eclipse/smarthome/core/semantics/model/equipment/Equipments.java")
    file.write(header())
    file.write("""package org.eclipse.smarthome.core.semantics.model.equipment;

import java.util.Set;
import java.util.HashSet;
import java.util.stream.Stream;

import org.eclipse.smarthome.core.semantics.model.Equipment;

/**
 * This class provides a stream of all defined equipments.
 * 
 * @author Generated from generateTagClasses.groovy - Initial contribution
 *
 */
public class Equipments {

    static final Set<Class<? extends Equipment>> EQUIPMENTS = new HashSet<>();

    static {
        EQUIPMENTS.add(Equipment.class);
""")    
    Iterator it = equipments.iterator();
    while(it.hasNext() ) {
        String equipment = it.next();
        file.write("        EQUIPMENTS.add(" + equipment + ".class);\n")
    }
    file.write("""    }

    public static Stream<Class<? extends Equipment>> stream() {
        return EQUIPMENTS.stream();
    }
}
""")
    file.close()
}

def createPointsFile(Set<String> points) {
    def file = new FileWriter("../src/main/java/org/eclipse/smarthome/core/semantics/model/point/Points.java")
    file.write(header())
    file.write("""package org.eclipse.smarthome.core.semantics.model.point;

import java.util.Set;
import java.util.HashSet;
import java.util.stream.Stream;

import org.eclipse.smarthome.core.semantics.model.Point;

/**
 * This class provides a stream of all defined points.
 * 
 * @author Generated from generateTagClasses.groovy - Initial contribution
 *
 */
public class Points {

    static final Set<Class<? extends Point>> POINTS = new HashSet<>();

    static {
        POINTS.add(Point.class);
""")    
    Iterator it = points.iterator();
    while(it.hasNext() ) {
        String point = it.next();
        file.write("        POINTS.add(" + point + ".class);\n")
    }
    file.write("""    }

    public static Stream<Class<? extends Point>> stream() {
        return POINTS.stream();
    }
}
""")
    file.close()
}

def createPropertiesFile(Set<String> properties) {
    def file = new FileWriter("../src/main/java/org/eclipse/smarthome/core/semantics/model/property/Properties.java")
    file.write(header())
    file.write("""package org.eclipse.smarthome.core.semantics.model.property;

import java.util.Set;
import java.util.HashSet;
import java.util.stream.Stream;

import org.eclipse.smarthome.core.semantics.model.Property;

/**
 * This class provides a stream of all defined properties.
 * 
 * @author Generated from generateTagClasses.groovy - Initial contribution
 *
 */
public class Properties {

    static final Set<Class<? extends Property>> PROPERTIES = new HashSet<>();

    static {
        PROPERTIES.add(Property.class);
""")    
    Iterator it = properties.iterator();
    while(it.hasNext() ) {
        String property = it.next();
        file.write("        PROPERTIES.add(" + property + ".class);\n")
    }
    file.write("""    }

    public static Stream<Class<? extends Property>> stream() {
        return PROPERTIES.stream();
    }
}
""")
    file.close()
}

def header() { """/**
 * Copyright (c) 2014,2018 Contributors to the Eclipse Foundation
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
"""
}