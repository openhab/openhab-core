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

nl = System.getProperty("line.separator")
bundleName = request.getProperties().get('bindingIdCamelCase')
githubUser = request.getProperties().get('githubUser')
outputDirectory = new File(request.getOutputDirectory())

/**
 * Find which bundle the new binding should be added
 */
def String findAfterBundle(newBundle, codeownersFile) {
    def codeowners = codeownersFile.getText('UTF-8')
    def lastIndex
    def lines = codeowners.split('\n')
                          .findAll{ it =~ /^.bundles/}
                          .collect { it.replaceAll(/\/bundles\/([^\/]+)\/.+/, '$1')}
    lines.add(newBundle)
    lines = lines.sort()
    lines.eachWithIndex { line, index ->
        if (line.contains(newBundle)) {
            lastIndex = index-1
        }
    }
    // if index == -1 it means its before all other bindings.
    // To handle this case an empty string is returned.
    // The other code handles the empty string as a special case
    return lastIndex < 0 ? "" : lines[lastIndex]
}

/**
 * Add the new bundle to the CODEWONERS file
 */
def addUserToCodewoners(githubUser, codeownersFile, bundleAfter, newBundle) {
    def newContent = ''
    // if new binding is a new first entry it's inserted directly after depencies tag
    def matchBundleAfter = bundleAfter.isEmpty() ? '# Add-on maintainers:' : (bundleAfter + '/')
    def lines = codeownersFile.eachLine { line, index ->
        newContent += line + nl
        if (line.contains(matchBundleAfter)) {
            newContent += '/bundles/' + newBundle + '/ @' + githubUser + nl
        }
    }
    codeownersFile.write(newContent)
    println 'Added github user to CODEOWNERS file'
}

/**
 * Add the new bundle to the addons bom pom.xml
 */
def addBundleToBom(bundleAfter, newBundle) {
    def bomDependency =
'''    <dependency>
      <groupId>org.openhab.addons.bundles</groupId>
      <artifactId>###</artifactId>
      <version>${project.version}</version>
    </dependency>
'''.replace('###', newBundle)
    def bomFile = new File(outputDirectory, '../bom/openhab-addons/pom.xml')
    def newContent = ''
    // if new binding is a new first entry it's inserted directly after dependencies tag
    def matchBundleAfter = bundleAfter.isEmpty() ? '<dependencies>' : (bundleAfter + '<')
    def offset = bundleAfter.isEmpty() ? 0 : 2
    def insertIndex = 0;
    def lines = bomFile.eachLine { line, index ->
        newContent += line + nl
        if (line.contains(matchBundleAfter)) {
            insertIndex = index + offset
        }
        if (insertIndex > 0 && index == insertIndex) {
            newContent += bomDependency
            insertIndex = 0
        }
    }
    bomFile.write(newContent)
    println 'Added bundle to bom pom'
}

//
/**
 * Fix the bundle parent pom. The maven archytype adds the bundle, but add the end of the module list.
 * It also modifies the first and last line 
 */
def fixBundlePom(bundleAfter, newBundle) {
    def bomFile = new File(outputDirectory, 'pom.xml')
    def module = '<module>' + newBundle + '</module>'
    def newContent = ''
    // if new binding is a new first entry it's inserted directly after modules tag
    def matchBundleAfter = bundleAfter.isEmpty() ? '<modules>' : (bundleAfter + '<')
    def insertIndex = 0;
    def lines = bomFile.eachLine { line, index ->
        if (!line.contains(module)) {
            // filter out the already added module by the achetype
            newContent += line + nl
        }
        if (line.contains(matchBundleAfter)) {
            insertIndex = index
        }
        if (insertIndex > 0 && index == insertIndex) {
            newContent += '    ' + module + nl
            insertIndex = 0
        }
    }
    bomFile.write(newContent)
    println 'Fix bundle parent pom.xml'
}

//--------------------------------------------------------------------------
// Main
def newBundle = request.getPackage()
def codeownersFile = new File(outputDirectory, '../CODEOWNERS')

if (codeownersFile.exists()) {
    def bundleAfter = findAfterBundle(newBundle, codeownersFile)
    println 'Add new bundle after: ' + bundleAfter
    addUserToCodewoners(githubUser, codeownersFile, bundleAfter, newBundle)
    addBundleToBom(bundleAfter, newBundle)
    fixBundlePom(bundleAfter, newBundle)

    println ''
    println '*********************************************'
    println ''
    println ' Start your new binding on a new git branch:'
    println ''
    println ' git checkout -b ' + bundleName.toLowerCase()
    println ''
    println '*********************************************'
    println ''
} else {
    println 'Could not find the CODEOWNERS files to perform additional annotations: ' + codeownersFile
}
