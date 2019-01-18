---
layout: documentation
---

# Internationalization

In this chapter the Eclipse SmartHome support for internationalization is described. 
Translations

All texts can be internationalized by using Java I18N properties files. For each language a specific file with a postfix notation is used. The postfix consists of a language code and an optional region code (country code).

```
Format:     any_<language-code>_<country-code>.properties
Example:    any_de_DE.properties
```

The language code is either two or three lowercase letters that are conform to the ISO 639 standard. The region code (country code) consists of two uppercase letters that are conform to the ISO 3166 standard or to the UN M.49 standard.

The search order for those files is the following:

```
any_<language-code>_<country-code>.properties
any_<language-code>.properties
any.properties
```

You can find detailed information about Java internationalization support and a list of the ISO 639 and ISO 3166 codes at [The Java Tutorials](http://docs.oracle.com/javase/tutorial/i18n/locale/create.html) page. 

The properties files have to be placed in the following directory of the bundle:

```
ESH-INF/i18n
```

Example files:

```
|- ESH-INF
|---- i18n
|------- yahooweather_de.properties
|------- yahooweather_de_DE.properties
|------- yahooweather_fr.properties
```

## Internationalize Binding XML files

In Eclipse SmartHome a binding developer has to provide different XML files. In these XML files label and description texts must be specified. To Internationalize these texts Java I18N properties files must be defined.

For the binding definition and the thing types XML files Eclipse SmartHome defines a standard key scheme, that allows to easily reference the XML nodes. Inside the XML nodes the text must be specified in the default language English. Typically all texts for a binding are put into one file with name of the binding, but they can also be split into multiple files.

### Binding Definition

The following snippet shows the binding XML file of the Yahoo Weather Binding and its language file that localizes the binding name and description for the German language.

XML file (binding.xml):

```xml
<binding:binding id="yahooweather">
    <name>YahooWeather Binding</name>
    <description>The Yahoo Weather Binding requests the Yahoo Weather Service
		to show the current temperature, humidity and pressure.</description>
    <author>Kai Kreuzer</author>
</binding:binding>
```

Language file (yahooweather_de.properties):

```ini
binding.yahooweather.name = Yahoo Wetter Binding
binding.yahooweather.description = Das Yahoo Wetter Binding stellt verschiedene Wetterdaten wie die Temperatur, die Luftfeuchtigkeit und den Luftdruck für konfigurierbare Orte vom yahoo Wetterdienst bereit.
```

So the key for referencing the name of a binding is `binding.<binding-id>.name` and `binding.<binding-id>.description` for the description text. 

### Thing Types

The following snippet shows an excerpt of the thing type definition XML file of the Yahoo Weather Binding and its language file that localizes labels and descriptions for the German language.

XML file (thing-types.xml):

```xml
<thing:thing-descriptions bindingId="yahooweather">
    <thing-type id="weather">
        <label>Weather Information</label>
        <description>Provides various weather data from the Yahoo service</description>
        <channels>
            <channel id="precipitation" typeId="precipitation" />
            <channel id="temperature" typeId="temperature" />
            <channel id="minTemperature" typeId="temperature">
                <label>Min. Temperature</label>
                <description>Minimum temperature in degrees celsius (metric) or fahrenheit (imperial).</description>
            </channel>
        </channels>
        <config-description>
            <parameter name="location" type="text">
                <label>Location</label>
                <description>Location for the weather information. Syntax is WOEID, see https://en.wikipedia.org/wiki/WOEID.
                </description>
                <required>true</required>
            </parameter>
        </config-description>
    </thing-type>
    <channel-type id="precipitation">
        <item-type>String</item-type>
        <label>Precipitation</label>
        <description>Current precipitation (dry, rain, snow).</description>
        <state readOnly="true" pattern="%s">
            <options>
                <option value="dry">dry</option>
                <option value="rain">rain</option>
                <option value="snow">snow</option>
            </options>
        </state>
    </channel-type>
    <channel-type id="temperature">
        <item-type>Number</item-type>
        <label>Temperature</label>
        <description>Current temperature in degrees celsius (metric) or fahrenheit (imperial).</description>
        <state readOnly="true" pattern="%d Value" />
        <config-description>
            <parameter name="unit" type="text" required="true">
                <label>Temperature unit</label>
                <description>Select the temperature unit.</description>
                <options>
                    <option value="C">Degree Celsius</option>
                    <option value="F">Degree Fahrenheit</option>
                </options>
                <default>C</default>
            </parameter>
        </config-description>
    </channel-type>
</thing:thing-descriptions>

```

Language file (yahooweather_de.properties):

```ini
thing-type.yahooweather.weather.label = Wetterinformation
thing-type.yahooweather.weather.description = Stellt verschiedene Wetterdaten vom Yahoo Wetterdienst bereit.

thing-type.config.yahooweather.weather.location.label = Ort
thing-type.config.yahooweather.weather.location.description = Ort der Wetterinformation. Syntax ist WOEID, siehe https://en.wikipedia.org/wiki/WOEID.

channel-type.yahooweather.precipitation.label = Niederschlag
channel-type.yahooweather.precipitation.description = Aktueller Niederschlag (Trocken, Regen, Schnee).
channel-type.yahooweather.precipitation.state.option.dry = Trocken
channel-type.yahooweather.precipitation.state.option.rain = Regen
channel-type.yahooweather.precipitation.state.option.snow = Schnee

channel-type.yahooweather.temperature.label = Temperatur
channel-type.yahooweather.temperature.description = Aktuelle Temperatur in Grad Celsius (Metrisch) oder Grad Fahrenheit (US).
channel-type.yahooweather.temperature.state.pattern = %d Wert

thing-type.yahooweather.weather.channel.minTemperature.label = Min. Temperatur
thing-type.yahooweather.weather.channel.minTemperature.description = Minimale Temperatur in Grad Celsius (Metrisch) oder Grad Fahrenheit (US).

channel-type.config.yahooweather.temperature.unit.label = Temperatur Einheit
channel-type.config.yahooweather.temperature.unit.description = Auswahl der gewünschten Temperatur Einheit.
channel-type.config.yahooweather.temperature.unit.option.C = Grad Celsius
channel-type.config.yahooweather.temperature.unit.option.F = Grad Fahrenheit
```

So the key for referencing a label of a defined thing type is `thing-type.<binding-id>.<thing-type-id>.label`.
A label of a channel type can be referenced with `channel-type.<binding-id>.<channel-type-id>.label` and a label of a channel definition with `thing-type.<binding-id>.<thing-type-id>.channel.<channel-id>.label`.
And finally the config description parameter key is `thing-type.config.<binding-id>.<thing-type-id>.<parameter-name>.label` or `channel-type.config.<binding-id>.<channel-type-id>.<parameter-name>.label`.

The following snippet shows an excerpt of the thing type definition XML file of the Weather Underground Binding and its language file that localizes labels and descriptions for the French language.

XML file (thing-types.xml):

```xml
<thing:thing-descriptions bindingId="weatherunderground">

    <thing-type id="weather">
        <label>Weather Information</label>
        <description>Provides various weather data from the Weather Underground service</description>

        <channel-groups>
            <channel-group id="current" typeId="current" />
            <channel-group id="forecastTomorrow" typeId="forecast">
                <label>Weather Forecast Tomorrow</label>
                <description>This is the weather forecast for tomorrow</description>
            </channel-group>
            <channel-group id="forecastDay2" typeId="forecast">
                <label>Weather Forecast Day 2</label>
                <description>This is the weather forecast in two days</description>
            </channel-group>
        </channel-groups>
        
        <config-description>
            <parameter name="apikey" type="text" required="true">
                <context>password</context>
                <label>API Key</label>
                <description>API key to access the Weather Underground service</description>
            </parameter>
            <parameter name="location" type="text" required="true">
                <label>Location of Weather Information</label>
                <description>Multiple syntaxes are supported. Please read the binding documentation for more information</description>
            </parameter>
            <parameter name="language" type="text" required="false">
                <label>Language</label>
                <description>Language to be used by the Weather Underground service</description>
                <options>
                    <option value="EN">English</option>
                    <option value="FR">French</option>
                    <option value="DL">German</option>
                </options>
            </parameter>
            <parameter name="refresh" type="integer" min="5" required="false" unit="min">
                <label>Refresh interval</label>
                <description>Specifies the refresh interval in minutes.</description>
                <default>30</default>
            </parameter>
        </config-description>
    </thing-type>

    <channel-group-type id="current">
        <label>Current Weather</label>
        <description>This is the current weather</description>
        <channels>
            <channel id="conditions" typeId="currentConditions" />
            <channel id="temperature" typeId="temperature" />
        </channels>
    </channel-group-type>

    <channel-group-type id="forecast">
        <label>Weather Forecast</label>
        <description>This is the weather forecast</description>
        <channels>
            <channel id="temperature" typeId="temperature">
                <label>Temperature</label>
                <description>Forecasted temperature</description>
            </channel>
            <channel id="maxTemperature" typeId="maxTemperature" />
        </channels>
    </channel-group-type>

    <channel-type id="currentConditions">
        <item-type>String</item-type>
        <label>Current Conditions</label>
        <description>Weather current conditions</description>
        <state readOnly="true" pattern="%s"></state>
    </channel-type>

    <channel-type id="temperature">
        <item-type>Number</item-type>
        <label>Temperature</label>
        <description>Current temperature</description>
        <category>Temperature</category>
        <state readOnly="true" pattern="%.1f" />
        <config-description>
            <parameter name="SourceUnit" type="text" required="true">
                <label>Temperature Source Unit</label>
                <description>Select the temperature unit provided by the Weather Underground service</description>
                <options>
                    <option value="C">Degree Celsius</option>
                    <option value="F">Degree Fahrenheit</option>
                </options>
                <default>C</default>
            </parameter>
        </config-description>
    </channel-type>

    <channel-type id="maxTemperature">
        <item-type>Number</item-type>
        <label>Maximum Temperature</label>
        <description>Maximum temperature</description>
        <category>Temperature</category>
        <state readOnly="true" pattern="%.1f" />
        <config-description>
            <parameter name="SourceUnit" type="text" required="true">
                <label>Maximum Temperature Source Unit</label>
                <description>Select the maximum temperature unit provided by the Weather Underground service</description>
                <options>
                    <option value="C">Degree Celsius</option>
                    <option value="F">Degree Fahrenheit</option>
                </options>
                <default>C</default>
            </parameter>
        </config-description>
    </channel-type>

</thing:thing-descriptions>
```

Language file (weatherunderground_fr.properties):

```ini
# binding
binding.weatherunderground.name = Extension WeatherUnderground
binding.weatherunderground.description = L'extension Weather Underground interroge le service Weather Underground pour récupérer des données météo.

# thing types
thing-type.weatherunderground.weather.label = Informations météo
thing-type.weatherunderground.weather.description = Présente diverses données météo fournies par le service Weather Underground.

# thing type configuration
thing-type.config.weatherunderground.weather.apikey.label = Clé d'accès
thing-type.config.weatherunderground.weather.apikey.description = La clé d'accès au service Weather Underground.
thing-type.config.weatherunderground.weather.location.label = Emplacement des données météo
thing-type.config.weatherunderground.weather.location.description = Plusieurs syntaxes sont possibles. Merci de consulter la documentation de l'extension pour plus d'information.
thing-type.config.weatherunderground.weather.language.label = Langue
thing-type.config.weatherunderground.weather.language.description = La langue à utiliser par le service Weather Underground.
thing-type.config.weatherunderground.weather.language.option.EN = Anglais
thing-type.config.weatherunderground.weather.language.option.FR = Français
thing-type.config.weatherunderground.weather.language.option.DL = Allemand
thing-type.config.weatherunderground.weather.refresh.label = Fréquence de rafraîchissement
thing-type.config.weatherunderground.weather.refresh.description = La fréquence de rafraîchissement des données en minutes.

# channel group types
channel-group-type.weatherunderground.current.label = Météo actuelle
channel-group-type.weatherunderground.current.description = La météo actuelle.
channel-group-type.weatherunderground.forecast.label = Météo prévue
channel-group-type.weatherunderground.forecast.description = La météo prévue.

# channel groups
thing-type.weatherunderground.weather.group.forecastTomorrow.label = Météo de demain
thing-type.weatherunderground.weather.group.forecastTomorrow.description = La météo prévue demain.
thing-type.weatherunderground.weather.group.forecastDay2.label = Météo dans 2 jours
thing-type.weatherunderground.weather.group.forecastDay2.description = La météo prévue dans 2 jours.

# channel types
channel-type.weatherunderground.currentConditions.label = Conditions actuelles
channel-type.weatherunderground.currentConditions.description = Les conditions météo actuelles.
channel-type.weatherunderground.temperature.label = Température
channel-type.weatherunderground.temperature.description = La température actuelle.
channel-type.weatherunderground.maxTemperature.label = Température maximale
channel-type.weatherunderground.maxTemperature.description = La température maximale.

# channels inside a channel group type
channel-group-type.weatherunderground.current.channel.temperature.label = Température
channel-group-type.weatherunderground.current.channel.temperature.description = La température prévue.

# channel type configuration
channel-type.config.weatherunderground.temperature.SourceUnit.label = Unité de température
channel-type.config.weatherunderground.temperature.SourceUnit.description = Choix de l'unité de température fournie par le service Weather Underground pour la température actuelle.
channel-type.config.weatherunderground.temperature.SourceUnit.option.C = Degrés Celsius
channel-type.config.weatherunderground.temperature.SourceUnit.option.F = Degrés Fahrenheit
channel-type.config.weatherunderground.maxTemperature.SourceUnit.label = Unité de température maximale
channel-type.config.weatherunderground.maxTemperature.SourceUnit.description = Choix de l'unité de température fournie par le service Weather Undergroundde pour la température maximale.
channel-type.config.weatherunderground.maxTemperature.SourceUnit.option.C = Degrés Celsius
channel-type.config.weatherunderground.maxTemperature.SourceUnit.option.F = Degrés Fahrenheit

```

So the label of a channel group type can be referenced with `channel-group-type.<binding-id>.<channel-group-type-id>.label` and the label of a channel group definition with `thing-type.<binding-id>.<thing-type-id>.group.<channel-group-id>.label`.
A label of a channel definition inside a channel group type can be translated with `channel-group-type.<binding-id>.<channel-group-type-id>.channel.<channel-id>.label`.

### Using custom Keys

In addition to the default keys the developer can also specify custom keys inside the XML file. But with this approach the XML file cannot longer contain the English texts. So it is mandatory to define a language file for the English language. The syntax for custom keys is `@text/<key>`. The keys are unique across the whole bundle, so a constant can reference any key in all files inside the `ESH-INF/i18n` folder.

The following snippet shows a binding XML that uses custom keys:

XML file (binding.xml):

```xml
<binding:binding id="yahooweather">
    <name>@text/bindingName</name>
    <description>@text/bindingName</description>
    <author>Kai Kreuzer</author>
</binding:binding>
```

Language file (yahooweather_en.properties):

```ini
bindingName = Yahoo Weather Binding

offline.communication-error=The Yahoo Weather API is currently not available.
```

Language file (yahooweather_de.properties):

```ini
bindingName = Yahoo Wetter Binding

offline.communication-error=Die Yahoo Wetter API ist zur Zeit nicht verfügbar.
```

The custom keys are a very good practice to translate bundle dependent error messages.

```java
updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, "@text/offline.communication-error");
```

## I18n Text Provider API

To programmatically resolve texts for certain languages Eclipse SmartHome provides the OSGi service `TranslationProvider`. The service parses every file inside the `ESH-INF/i18n` folder and caches all texts. A localized text can be retrieved via the method `getText(Bundle bundle, String key, String default, Locale locale)` (or via the method `getText(Bundle bundle, String key, String default, Locale locale, Object... arguments)` if additional arguments are to be injected into the localized text), where bundle must be the reference to the bundle, in which the file is stored. The BundleContext from the Activator provides a method to get the bundle.

```java
String text = i18nProvider.getText(bundleContext.getBundle(), "my.key", "DefaultValue", Locale.GERMAN);
```

## Locale Provider

To programmatically fetch the locale used by the Eclipse SmartHome system an OSGi service `LocaleProvider` is offered. The service contains a `getLocale()` method that can be used to choose a configurable locale.

## Getting Thing Types and Binding Definitions in different languages

Thing types can be retrieved through the `ThingTypeRegistry` OSGi service. Every method takes a `Locale` as last argument. If no locale is specified the thing types are returned for the default locale which is determined by using the `LocaleProvider`, or the default text, which is specified in the XML file, if no language file for the default locale exists.

The following snippet shows how to retrieve the list of Thing Types for the German locale:

```java
List<ThingType> thingTypes = thingTypeRegistry.getThingTypes(Locale.GERMAN);
```

If one binding supports the German language and another does not, it might occur that the languages of the returned thing types are mixed.

For Binding Info and ConfigDescription, the localized objects can be retrieved via the `BindingInfoRegistry` and the `ConfigDescriptionRegistry` in the same manner as described for Thing Types.

