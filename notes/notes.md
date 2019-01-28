# Eclipse SmartHome

## IDE

### Setup instructions

* Start the Eclipse Installer
* Switch to advanced mode
* Use "Eclipse IDE for Java Developers"
* Product Version: 2018-12
* Java 1.8+ VM: your 1.8 Oracle JDK
* Next
* Press "+" to add a new Resource URI
* Catalog: Github Projects
* Resource URI: https://raw.githubusercontent.com/maggu2810/esh-puremaven/master/tools/oomph/EclipseSmartHome.setup
* OK
* Select the new "SmartHome" entry in the "Github Project, <User>" section
* Next
* Setup your desired settings (location, etc.)
* Next
* Finish
* After the installer has been finished, start the IDE (dependent on your installer settings this is done automatically or not)
* The initial tasks are executed
* Wait until the bottom right corner of the IDE does not show "Build" or "Refreshing workspace" or any other progress messages
* On my system there are 199 Errors left (Problem view) - the number does not matter
* Press "Help", "Perform Setup Tasks..."
* Press "Finish"
* The dialog disappears but could be opened again using the icon in the (bottom) status bar (near to the left side). You should open it and check it, because the chance if high that a restart is requested. The process (perform setup tasks) continues after the restart has been done. You can bring the dialog to the front after the restart using the same icon.
* Wait after all has been done (wait for an empty buttom right status field)
* There should be no errors remaining in the "Problem" view

### Start a demo runtime

* Open the project "org.eclispe.smarthome.demo.app"
* Open the file "app.bndrun"
* Press "resolve" to resolve the "run requirements" and ensure that all requirements are able to satisfied.
* Press "Debug OSGi"
* The Gogo shell should be working, test by executing e.g. "smarthome:items list" or "scr:list"
* The Classic UI should be accessible by entering the following URL into a browser: http://127.0.0.1:8080/classicui/app

## BOM

TODO

## others

### ...

add xsd files to bundle (e.g. core.thing.xml thing-description...xsd)

### General

https://stackoverflow.com/questions/3008065/maven-2-resources-inheritance-parent-child-project
Resource locations cannot be added in child POMs, we can only replace the defined resource locations.
As we do not want to declare the resources multiple times, we need to use the build-helper plugin.
This should be done for the moment only and we should move resources to the common location (src/main/resources)

### Resources

This continues some general point:
There are resources that does not reside in the standard "src/main/resources" place.
This resources needs to be moved. I don't do it ATM to allow an easy rebase on the latest ESH repo.
The model bundles are an exception because it uses a whole different folder structure and this one needs to be analyzed later (if it can be further improved at all).

### p2 feature, repository

https://blog.sandra-parsick.de/2017/09/22/generate-p2-repository-from-maven-artifacts-in-2017/

### SAT

reintegrate SAT

### archetype

reintegrate archetype

### Metedata

recreate "./bundles/io/org.eclipse.smarthome.io.net/OSGI-INF/metatype/org.eclipse.smarthome.io.net.http.internal.WebClientFactoryImpl.xml" using annotations

