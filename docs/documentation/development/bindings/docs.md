---
layout: documentation
---

{% include base.html %}

# Documenting a Binding

A binding should come with some documentation in form of a ```README.md``` file (in markdown syntax) within its project folder.
If a single file is not enough, additional resources (e.g. further md-files, images, example files) can be put in a ```doc``` folder.
To facilitate reviews and diffs, there should be a new line for every sentence within a paragraph.
Such line breaks won't have any impact on the rendering, but they immensely help for the maintenance.

__Note__: As the ```README.md``` pages might be used on Jekyll-based websites, it is important to also respect the following formatting rules:

- There must be an empty line after every header (`#...`).
- There must be an empty line before and after every list.
- There must be an empty line before and after every code section.

Neither the ```README.md``` file nor the ```doc``` folder must be added to ```build.properties```, i.e. they only exist in the source repo, but should not be packaged within the binary bundles.

It is planned to generate parts of the documentation based on the files that are available with the ```ESH-INF``` folder of the binding.
As this documentation generation is not (yet) in place, the documentation currently needs to be maintained fully manually.
The Maven archetype creates a [template for the binding documentation](https://github.com/eclipse/smarthome/blob/master/tools/archetype/binding/src/main/resources/archetype-resources/README.md).
