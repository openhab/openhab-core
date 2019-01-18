---
layout: documentation
---

{% include base.html %}

# Handling Code Dependencies

## Eclipse SmartHome Packages

When implementing a binding, you should make sure that you do not introduce too many dependencies on different parts of Eclipse SmartHome. The following list of Java packages should suffice (if you use [create binding scripts](https://github.com/eclipse/smarthome/tree/master/extensions/binding) to create a binding these packages will be automatically added as imports in the manifest):  

 - org.eclipse.smarthome.config.core  
 - org.eclipse.smarthome.core.library.types  
 - org.eclipse.smarthome.core.thing  
 - org.eclipse.smarthome.core.thing.binding  
 - org.eclipse.smarthome.core.thing.binding.builder  
 - org.eclipse.smarthome.core.thing.type  
 - org.eclipse.smarthome.core.types  
 - org.eclipse.smarthome.core.util  
 
Depending on the kind of communication that you need to implement, you can optionally also add any exported packages from these bundles:

 - org.eclipse.smarthome.config.discovery
 - org.eclipse.smarthome.io.transport.mdns
 - org.eclipse.smarthome.io.transport.mqtt
 - org.eclipse.smarthome.io.transport.serial
 - org.eclipse.smarthome.io.transport.upnp
 
## Optional Bundles

You might also have the need to use other libraries for specific use cases like XML processing etc. In order to not have every binding use a different library, the following packages are available by default: 

### For XML Processing  
 - com.thoughtworks.xstream  
 - com.thoughtworks.xstream.annotations  
 - com.thoughtworks.xstream.converters  
 - com.thoughtworks.xstream.io  
 - com.thoughtworks.xstream.io.xml  

### For JSON Processing  
 - com.google.gson.*  
 
### For HTTP Operations  
 - org.eclipse.jetty.client.*  
 - org.eclipse.jetty.client.api.*  
 - org.eclipse.jetty.http.*  
 - org.eclipse.jetty.util.*  
 
Note: HttpClient instances should be obtained by the handler factory through the `HttpClientFactory` service and unless there are specific configuration requirements, the shared instance should be used.
 
### For Web Socket Operations  
 - org.eclipse.jetty.websocket.client  
 - org.eclipse.jetty.websocket.api
 
Note: WebSocketClient instances should be obtained by the handler factory through the `WebSocketClientFactory` service and unless there are specific configuration requirements, the shared instance should be used.

## 3rd Party Libraries

If you want your binding to rely on a custom library that might not even be an OSGi bundle, you can embed it in your bundle as a jar file following these steps: 

 - Put your jar file in the file system of your project (e.g. ```lib/library.jar```).
 - Add the new library to the ```bin.includes``` section of your [build.properties](http://help.eclipse.org/luna/index.jsp?topic=/org.eclipse.pde.doc.user/reference/pde_feature_generating_build.htm) file 
 to make sure that the library will be included in the binary.
 - To compile the binding in Eclipse, you have to add the library to your ```.classpath``` as well. Do this by adding a new classpath entry:
 `<classpathentry kind="lib" path="lib/library.jar"/>` 
 - Add the library project to the bundle classpath in MANIFEST.MF file  
  ```Bundle-ClassPath: .,
      lib/library.jar```
	  
Keep in mind that if you want to use third party libraries they have to be compatible with the [list of licenses approved for use by third-party code redistributed by Eclipse projects](https://eclipse.org/legal/eplfaq.php#3RDPARTY).  
Every bundle must contain an [NOTICE](https://www.eclipse.org/projects/handbook/#legaldoc) file, listing the 3rd party libraries and their licenses.
