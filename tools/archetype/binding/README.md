# OpenHAB Binding Archetype

## Overview

The `binding` directory provides a reusable **archetype template** to help developers quickly create and configure new bindings for OpenHAB. It includes the necessary files and structure to standardize binding development.

---

## Folder Structure

Here’s an overview of the main components:

- **archetype-resources/**: Contains templates for key components such as:
  - **feature.xml**: Defines the OpenHAB feature.
  - **Configuration.java**: Maps thing configuration parameters.
  - **Handler.java**: Handles commands and Thing lifecycle.
  - **HandlerFactory.java**: Creates and manages Thing handlers.
- **META-INF/**: Project metadata files.
- **README.md** (inside archetype-resources): A template for binding-specific documentation.

---

## How to Use This Archetype

To create a new OpenHAB binding using this template:

1. **Generate a New Binding Project**  
   Use Maven to generate a project based on this archetype:
   ```bash
   mvn archetype:generate \
       -DarchetypeGroupId=org.openhab.core \
       -DarchetypeArtifactId=openhab-binding-archetype \
       -DgroupId=com.example \
       -DartifactId=my-new-binding
   ```
