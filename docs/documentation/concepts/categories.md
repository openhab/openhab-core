---
layout: documentation
---

{% include base.html %}

# Categories

Categories in Eclipse SmartHome are used to provide meta information about Things, Channels, etc. UIs can use this information to render specific icons or provide a search functionality to for example filter all Things for a certain category.

## Differences between categories

We separate the categories into `functional` and `visual`. 
Therefore we treat `Thing categories` as how the physical device **looks like** and `Channel categories` as something that describes the **functional purpose** of the Channel.

## Thing Categories

The Thing type definition allows to specify a category. 
User interfaces can parse this category to get an idea how to render this Thing. 
A Binding can classify each Thing into one of the existing categories. 
The list of all predefined categories can be found in our categories overview:

| Category        | Description | Icon Example |
|-----------------|-------------|{% for category in site.data.categories_thing %}
|{{category.name}}|{{category.description}}|![{{category.icon}}](../features/ui/iconset/classic/icons/{{category.icon}}){:height="36px" width="36px"}|{% endfor %}

### Channel Group Categories

Channel Groups can be seen as a kind of `sub-device` as they combine certain (physical) abilities of a `Thing` into one. For such `Group Channels` one can set a category from the `Thing` category list.

## Channel Categories

The Channel type definition allows to specify a category. 
A Binding should classify each Channel into one of the existing categories or leave the category blank, if there is no good match. 
There are different types of categories for Channels, which are listed below.

{% for category in site.data.categories %}
    {% assign typesStr = typesStr | append: category.type | append: ',' %}
{% endfor %}
{% assign types = typesStr | split: ',' | uniq %}

{% for type in types %}
#### {{ type }}

| Category        | Icon Example |
|-----------------|--------------|{% for category in site.data.categories %}{% if category.type == type %}
|{{category.name}}|![{{category.name | downcase}}](../features/ui/iconset/classic/icons/{{category.name | downcase }}.png){:height="36px" width="36px"}|{% endif %}{% endfor %}
{% endfor %}
