/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
tagsByType = [:]

def tagSets = new TreeMap<String, String>()

def labelsFile = new FileWriter("${baseDir}/src/main/resources/tags.properties")
labelsFile.write("# Generated content - do not edit!\n")

for (line in tagsCsv()) {
    println "Processing Tag $line.Tag"

    def tagSet = (line.Parent ? tagSets.get(line.Parent) : line.Type) + "_" + line.Tag
    tagSets.put(line.Tag,tagSet)

    appendLabelsFile(labelsFile, line, tagSet)

    if (!tagsByType.containsKey(line.Type)) {
        tagsByType[line.Type] = []
    }
    tagsByType[line.Type].add(line)

    switch(line.Type) {
        case "Location"            : break;
        case "Equipment"           : break;
        case "Point"               : break;
        case "Property"            : break;
        default : println "Unrecognized type " + line.Type
    }
}

labelsFile.close()

createDefaultSemanticTags(tagSets)
createDefaultProviderFile(tagSets)

println "\n\nTagSets:"
for (String tagSet : tagSets) {
    println tagSet
}

def tagsCsv() {
    return parseCsv(new FileReader("${baseDir}/model/SemanticTags.csv"), separator: ',')
}

def appendLabelsFile(FileWriter file, def line, String tagSet) {
    file.write(tagSet + "=" + line.Label)
    if (line.Synonyms) {
        file.write("," + line.Synonyms.replaceAll(", ", ","))
    }
    file.write("\n")
}

def camelToUpperCasedSnake(def tag) {
    return tag.split(/(?=[A-Z][a-z])/).join("_").toUpperCase()
}

def createDefaultSemanticTags(def tagSets) {
    def file = new FileWriter("${baseDir}/src/main/java/org/openhab/core/semantics/model/DefaultSemanticTags.java")
    file.write(header)
    file.write("""package org.openhab.core.semantics.model;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.semantics.SemanticTag;
import org.openhab.core.semantics.SemanticTagImpl;

/**
 * This class defines all the default semantic tags.
 *
 * @author Generated from generateTagClasses.groovy - Initial contribution
 */
@NonNullByDefault
public class DefaultSemanticTags {

    public static final SemanticTag EQUIPMENT = new SemanticTagImpl("Equipment", "", "", "");
    public static final SemanticTag LOCATION = new SemanticTagImpl("Location", "", "", "");
    public static final SemanticTag POINT = new SemanticTagImpl("Point", "", "", "");
    public static final SemanticTag PROPERTY = new SemanticTagImpl("Property", "", "", "");
""")

    for (type in tagsByType.keySet()) {
        file.write("""
    public static class ${type} {""")
        for (line in tagsByType[type]) {
            def tagId = (line.Parent ? tagSets.get(line.Parent) : line.Type) + "_" + line.Tag
            def constantName = camelToUpperCasedSnake(line.Tag)
            file.write("""
        public static final SemanticTag ${constantName} = new SemanticTagImpl( //
                "${tagId}", //
                "${line.Label}", //
                "${line.Description}", //
                "${line.Synonyms}");""")
        }
        file.write("""
    }
""")
    }
        file.write("""}
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
        defaultTags.add(DefaultSemanticTags.EQUIPMENT);
        defaultTags.add(DefaultSemanticTags.LOCATION);
        defaultTags.add(DefaultSemanticTags.POINT);
        defaultTags.add(DefaultSemanticTags.PROPERTY);
""")    
    for (line in tagsCsv()) {
        def constantName = line.Type + "." + camelToUpperCasedSnake(line.Tag)
        file.write("""        defaultTags.add(DefaultSemanticTags.${constantName});
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

    headerLines.add(0, "/*")
    headerLines.add(" */")
    headerLines.add("")

    headerLines.stream().collect(Collectors.joining(System.lineSeparator()))
}
