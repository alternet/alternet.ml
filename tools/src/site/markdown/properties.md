# Properties binder and class generator

<div class="nopub">
<a href="http://alternet.github.io/alternet-libs/tools/properties.html">
Published version of this page available HERE</a></div>

Given a Java property file, allow to populate a counterpart class with typed values.

Also allow to generate the target class(es), and run that generator as a Maven plugin.

1. [Overview](#overview)
2. [Tutorial](#tutorial)
    1. [Populating fields with property values](#binder)
    2. [Creating the template property file](#template)
        1. [Directives](#directives)
        1. [Primitives](#primitives)
        1. [URIs](#uris)
        1. [Strings](#strings)
        1. [Classes](#classes)
        1. [Passwords](#passwords)
        1. [Other types, adapters, and other directives](#adapters)
        1. [Unmarshaller](#unmarshaller)
        1. [Intermediate types](#intermediatesTypes)
        1. [Files](#files)
        1. [Dates and times](#datesTimes)
        1. [Lists](#lists)
        1. [Enums](#enums)
        1. [Unknown keys](#unknownKeys)
        1. [Types with value](#typesWithValue)
        1. [Separate types](#separateTypes)
        1. [Rules for types](#rules)
    3. [Generating the target class](#generator)
        1. [Generating the target class with Maven](#maven)
3. [Other features](#otherFeatures)
    1. [Default values](#default)
    1. [Merging separate property files](#merging)
    1. [Variable interpolation](#interpolation)
    1. [Automatic renaming](#renaming)

<a name="overview"></a>

## Overview

<div style="columns: 2">
<div>
<h3 style="margin: 0">Maven import</h3>
<pre class="prettyprint">
&lt;dependency&gt;
    &lt;groupId&gt;ml.alternet&lt;/groupId&gt;
    &lt;artifactId&gt;alternet-tools&lt;/artifactId&gt;
    &lt;version&gt;1.0&lt;/version&gt;
&lt;/dependency>
</pre>
</div>

<div style="break-before: column">
<h3>JavaDoc API documentation</h3>
<ul><li><a href="apidocs/index.html">Alternet Tools</a></li></ul>
<p>Other Alternet APIs :</p>
<ul><li><a href="../apidocs/index.html">Alternet Libs</a></li></ul>
</div>
</div>

<a name="tutorial"></a>

## Tutorial

Many Java applications are configured with external parameters that have to be loaded by the application from "`.properties`" files. Once loaded, those parameters are exposed as `String`s, and the keys to access them are also `String`s.

* The **properties binder** allow to populate a configuration class within which fields are the counterpart keys, and values are typed values.

* The **properties class generator** allow to generate such configuration class from a template property file. A Maven plugin allow to do it automatically.

<a name="binder"></a>

### Populating fields with property values

Say we have such a configuration file "<code class="files">conf.properties</code>" :

<div class="source"><pre class="prettyprint properties">
# Properties definining the GUI
gui.window.width = 500
gui.window.height = 300
gui.colors.background = #FFFFFF
gui.colors.foreground = #000080

# Properties definining datasources
service.url = https://some-services.example.org/someService
service.uuid = 5da66c77-7062-4b30-97fc-e747eb64570a

db.driver = com.fakesql.jdbc.Driver
db.url = jdbc:fakesql://localhost:3306/localdb?autoReconnect=true
db.account.login = theLogin
db.account.password = thePassword
</pre></div>

Using the property binder allow to load that file to the target class `org.example.Conf`, and then use the properties as typed data :

```java
    // unmarshall the property file to an org.example.Conf object
    Conf conf = Conf.unmarshall(new FileInputStream("conf.properties"));

    // look, we have an int :
    int width = Math.min(conf.gui.window.width, 400);
    // we can also use typed data in place :
    Dimension size = new Dimension(width, conf.gui.window.height);

   // look, we have an URI :
    URLConnection extService = conf.service.url.toURL().openConnection();

    // we can also supply an adapter for other types
    // such as java.awt.Color
    Color darkBg = conf.gui.colors.background.darker(); // darker() is a method of Color

    // you can handle groups of fields at once
    Connection dbConnection = getConnection(conf.db);
    // in fact, the class org.example.Conf wraps the class org.example.Conf.Db and others
```

To achieve this, you can create the target class by hand, but it is much more easy to generate it automatically from the property file, that serves as a template.

<a name="template"></a>

### Creating the template property file

The **template property file** aims to describe the shape of the target structure thanks to example values and additional indications for the [generator](#generator) that will create the Java source code of that class(es).

Create a copy of "`conf.properties`" to "`conf.template.properties`" and apply the following modifications to that template.

<a name="directives"></a>

#### Directives

First, create a new entry at the top, in order to indicate the fully qualified name of the target class to generate :

<div class="source"><pre class="prettyprint properties">
. = #org.example.Conf
</pre></div>

`.` is used as a special key for few directives that drives the code generation. `#org.example.Conf` stands for the class name to create.

Let's examine the other values.

<a name="primitives"></a>

#### Primitives

The right type will be used according to the value supplied in the template property file. For example, if we find `true` or `false`, the field will be a `boolean` (with that default value), if we find a list of booleans, e.g. `true, false, false` the field will be a `java.util.List<java.lang.Boolean>`.

In our template we have :

<div class="source"><pre class="prettyprint properties">
gui.window.width = 500
</pre></div>

You don't need to change anything, `500` is a number, and the field `width` will be a `short`. The more suitable number type will be used, therefore if you want a `byte`, use a value in that range (lower than 128), if you want a `float`, use a decimal in the value, etc.

If you want to force the type regardless the value, you can also write its type before the value :

<div class="source"><pre class="prettyprint properties">
gui.window.width = $int 500
</pre></div>

<div class="alert alert-info" role="alert">
Note that we use <code>$</code> for an existing type rather than a <code>#</code> for a type to create like the main class type.
</div>

<a name="uris"></a>

#### URIs

<div class="source"><pre class="prettyprint properties">
service.url = https://some-services.example.org/someService
</pre></div>

You don't need to change anything, "`https://some-services.example.org/someService`" is an URI,
and the field `url` will be a [`java.net.URI`](https://docs.oracle.com/javase/8/docs/api/java/net/URI.html).

<a name="strings"></a>

#### Strings

Similarly, the `db.url` is also an URI :

<div class="source"><pre class="prettyprint properties">
db.url = jdbc:fakesql://localhost:3306/localdb?autoReconnect=true
</pre></div>

But imagine we want it as a `java.lang.String`, we can surround it between double quotes to force it to be a `java.lang.String` instead of a `java.net.URI`:

<div class="source"><pre class="prettyprint properties">
db.url = "jdbc:fakesql://localhost:3306/localdb?autoReconnect=true"
</pre></div>

Similarly, every time we found a character used to introduce a directive, we can ignore everything by using double quotes around that value.

Finally, if no recipe is applicable, a `java.lang.String` will be assumed :

<div class="source"><pre class="prettyprint properties">
db.account.login = theLogin
</pre></div>

<a name="classes"></a>

#### Classes

For the database driver, we expect having an existing `java.lang.Class`. We can enforce that by prepending the value with `java:` in the template :

<div class="source"><pre class="prettyprint properties">
db.driver = java:com.fakesql.jdbc.Driver
</pre></div>

In your program, no more need of an additional `Class.forName("com.fakesql.jdbc.Driver");`, it is performed by the loader. Be aware that the class referred must be of the type given, therefore, a more general type should be specified in the template :

<div class="source"><pre class="prettyprint properties">
db.driver = java:java.sql.Driver
</pre></div>

or if you don't really know what will be the concrete type in the property, just use a wildcard in the template :
<div class="source"><pre class="prettyprint properties">
db.driver = java:?
</pre></div>

Note that an entry such as :

<div class="source"><pre class="prettyprint properties">
jndi.ds = java:/comp/env/jdbc/someDB
</pre></div>

wouldn't be considered as a Java class, but as a [`java.net.URI`](https://docs.oracle.com/javase/8/docs/api/java/net/URI.html).

<a name="passwords"></a>

#### Passwords

The password should not be left as-is in that template, replace the value by `*****` to get a `char[]` field (it is a bad practice to store passwords in Strings)

<div class="source"><pre class="prettyprint properties">
db.account.password = *****
</pre></div>

<div class="alert alert-info" role="alert">
Instead of getting a <code>char[]</code> field, it is possible to get a secure <code><a href="../apidocs/ml/alternet/security/Password.html">ml.alternet.security.Password</a></code>.
To achieve this, simply add the Maven declaration in your build :

<pre>
&lt;dependency&gt;
    &lt;groupId&gt;ml.alternet&lt;/groupId&gt;
    &lt;artifactId&gt;alternet-security&lt;/artifactId&gt;
    &lt;version>1.0&lt;/version&gt;
&lt;/dependency&gt;
</pre>

You also have to write your own adapter (see below). Note that since such password live aside the application in a configuration file, using that secure password has not so much advantages. Note also that they will be built from a <code>java.lang.String</code>, not from a <code>char[]</code>.

</div>

<a name="adapters"></a>

#### Other types, adapters, and other directives

Finally, we need to supply an adapter for the colors. First, indicates what is the type of the field by prepending the special notation `$java.awt.Color` before the actual value :

<div class="source"><pre class="prettyprint properties">
gui.colors.background = $java.awt.Color #FFFFFF
</pre></div>

Then, we need to supply an adapter, and tell the generator to use that adapter. An adapter is just a function that takes a `java.lang.String` and parse it to get an instance of the expected type ; our adapter is just a `java.util.function.Function<java.lang.String, java.awt.Color>`, and such function already exist, it is [`java.awt.Color::decode`](https://docs.oracle.com/javase/8/docs/api/java/awt/Color.html#decode-java.lang.String-).

To bind that adapter to the target class, we would write this Java code :

```java
    Adapter.map(Color.class, Color::decode) // when a Color type is expected, parse the text with Color.decode()
```

The [`ml.alternet.properties.Binder.Adapter`](apidocs/ml/alternet/properties/Binder.Adapter.html) class comes with convenient static methods to bind a class or a property name to an adapter function, or even a regular expression of a property name. It is also possible to map items from a list.

We will insert the above Java code inside our template, by using 2 others directives, the former to import the `Color` class, the latter to generate an unmarshaller with that adapter ; similarly, we also need an adapter for UUID :

<div class="source"><pre class="prettyprint properties">
# directives for importing the required types
. = !java.awt.Color
. = !java.util.UUID
# directives for getting adapters
. = @Adapter.map(Color.class, Color::decode)
. = @Adapter.map(UUID.class, UUID::fromString)
</pre></div>

and accordingly, setting the type on the property definition :

<div class="source"><pre class="prettyprint properties">
service.uuid = $java.util.UUID 5da66c77-7062-4b30-97fc-e747eb64570a
</pre></div>

<a name="unmarsaller"></a>

#### Unmarshaller

When a directive for getting an adapter is present, some convenient methods are added to the target class for unmarshalling a property file to an instance of that class :

```java
    Conf conf = Conf.unmarshall(new FileInputStream("conf.properties"));
```

If we don't have any adapter, it is recommended to add the `. = @` directive in order to generate the methods :

```java
    // if you use the ".=@" directive, this method will be generated for you
    // otherwise, this is what you have to write to get an instance of your
    // class from a property file
    public static Conf unmarshall(InputStream properties) throws IOException {
        return Binder.unmarshall(
            properties,
            Conf.class
        );
    }
```

<a name="intermediateTypes"></a>

#### Intermediate types

In fact, each time a property contains a dot in its name, it is breakdown in classes. That way the property name `gui.colors.background` creates the `org.example.Conf.Gui` class and the `org.example.Conf.Gui.Colors` class in the same Java source file.

Since no intermediate Java names called `UUID` or `Color` are in the scope of our property definitions, the type specified in the values will be automatically imported ; consequently, the import directives `. = !java.awt.Color` and `. = !java.util.UUID` may be discarded in our case.

Conversely, if we had a property like this (note that `gui.colors` have been changed to `gui.color`) :

<div class="source"><pre class="prettyprint properties">
gui.color.background = $java.awt.Color #FFFFFF
</pre></div>

we would have an intermediate type `org.example.Conf.Gui.Color` that would mask the `Color` type name for the `background` field ; therefore the field type in the Java code generated will be written with its fully qualified name `java.awt.Color`, and that name won't be automatically imported, whereas it is also used in the adapter. If a type is used in the adapter and not automatically imported, you have to write the import directive in the template.

Well, if you don't know whether to import a type or not, don't worry, the compiler will let you know after trying to [generate](#generator) your class : first, omit the imports, then [generate](#generator) the class and have a look at the compiler for the missing imports, and then, add them to the template, and try again !

#### The final property template file

The final property template "`conf.template.properties`" looks like this :

<div class="source"><pre class="prettyprint properties">
# Target class name
. = #org.example.Conf
# Required adapters
. = @Adapter.map(Color.class, Color::decode)
. = @Adapter.map(UUID.class, UUID::fromString)

# Properties definining the GUI
gui.window.width = $int 500
gui.window.height = 300
gui.colors.background = $java.awt.Color #FFFFFF
gui.colors.foreground = $java.awt.Color #000080

# Properties definining datasources
service.url = https://some-services.example.org/someService
service.uuid = $java.util.UUID 5da66c77-7062-4b30-97fc-e747eb64570a

db.driver = java:java.sql.Driver
db.url = "jdbc:fakesql://localhost:3306/localdb?autoReconnect=true"
db.account.login = theLogin
db.account.password = *****
</pre></div>

The next step is to [generate](#generator) the target class from that template. But let's examine before other kind of types that you can use in your templates.

<a name="files"></a>

#### Files

<div class="source"><pre class="prettyprint properties">
files.help = file:///path/to/help.txt
</pre></div>

You don't need to change anything, `file:///path/to/help.txt` is a file path,
and the field `help` will be a [`java.io.File`](https://docs.oracle.com/javase/8/docs/api/java/io/File.html).

<a name="datesTimes"></a>

#### Dates and times

Dates, times, and date-times without time-zone in the ISO-8601 calendar system such as (respectively) `2007-12-03`, `10:15:30`, and `2007-12-03T10:15:30` are giving (respectively) [`java.time.LocalDate`](https://docs.oracle.com/javase/8/docs/api/java/time/LocalDate.html), [`java.time.LocalTime`](https://docs.oracle.com/javase/8/docs/api/java/time/LocalTime.html), and [`java.time.LocalDateTime`](https://docs.oracle.com/javase/8/docs/api/java/time/LocalDateTime.html) :

<div class="source"><pre class="prettyprint properties">
schedule.launch : 2007-12-03
schedule.reload : 10:15:30
</pre></div>

<a name="lists"></a>

#### Lists

If several values are separated by a comma, the field will be a list :

<div class="source"><pre class="prettyprint properties">
gui.colors.pie = $java.awt.Color #FF0000, #00FF00, #0000FF
</pre></div>

gives a `java.util.List<java.awt.Color>`.

<div class="source"><pre class="prettyprint properties">
gui.fonts.size = $int 12, 16, 24
</pre></div>

gives a `java.util.List<java.lang.Integer>`.

<a name="enums"></a>

#### Enums

If several values are separated by a pipe, the field will be an enum :

<div class="source"><pre class="prettyprint properties">
service.status = PENDING | ACTIVE | INACTIVE | DELETED
</pre></div>

This will generate the enum `org.example.Conf.Service.Status`.

When the same enum is used several time, we can refer to it with its name, just like other properties that refer to an existing type :

<div class="source"><pre class="prettyprint properties">
plugin.status = $org.example.Conf.Service.Status PENDING | ACTIVE | INACTIVE | DELETED
</pre></div>

(in the last example, the enum has been generated once, next we are referring that existing class with `$`; of course you are not compelled to repeat the values since the type is already define elsewhere, they are here just for documenting the template)

A comma after the enumeration gives a list, e.g :

<div class="source"><pre class="prettyprint properties">
plugin.multipleStatus = $org.example.Conf.Service.Status PENDING | ACTIVE | INACTIVE | DELETED, ACTIVE
</pre></div>

gives a `java.util.List<org.example.Conf.Service.Status>`. That comma can also be set on the enum definition like this :

<div class="source"><pre class="prettyprint properties">
service.multipleStatus = PENDING | ACTIVE | INACTIVE | DELETED, ACTIVE
</pre></div>

Sometimes we expect the enum class to be generated with a different name than the name of the field; below, we don't want an enum named `org.example.Conf.Service.StartState`, we prefer having `org.example.Conf.Service.Status` :

<div class="source"><pre class="prettyprint properties">
service.startState = #Status PENDING | ACTIVE | INACTIVE | DELETED
</pre></div>

Sometimes we expect the enum class to be generated in its own file. To achieve this, write the fully qualified type name followed by the values :

<div class="source"><pre class="prettyprint properties">
service.status = #org.example.Status PENDING | ACTIVE | INACTIVE | DELETED
</pre></div>

<a name="unknownKeys"></a>

#### Unknown keys

Sometimes, a property file contains names that are not known in advance :

<div class="source"><pre class="prettyprint properties">
map.paris.geo = 48.864716, 2.349014
map.london.geo = 51.509865, -0.118092
map.istanbul.geo = 41.015137, 28.979530
</pre></div>

The template properties file may set a star character at the place of this field :

<div class="source"><pre class="prettyprint properties">
map.*.geo = $org.example.Geo 48.864716, 2.349014
</pre></div>

This will generate a special method that allow to retrieve a property by name. You also have to map that key to its adapter like this :

<div class="source"><pre class="prettyprint properties">
. = @Adapter.map("map.*.geo", Geo::parse)
</pre></div>

In your program, the special [`$()` method](apidocs/ml/alternet/properties/$.html#Z:Z:D-java.lang.String-) generated will allow to retreive the keys like this :

```java
    long dist = conf.map.<Geo> $("paris.geo")
                    .computeDistance(conf.map.<Geo> $("istanbul.geo"));
```

That `map` property may also contains other predefined fields if needed. Those fields are not reachable with the `$()` method ; that method allow to retrieve only fields that are not predefined.

<a name="typesWithValue"></a>

#### Types with value

Sometimes, there is a value as well as subproperties ; they have to be specified in the template property file as well :

<div class="source"><pre class="prettyprint properties">
gui.window = Sample application
gui.window.width = $int 500
gui.window.height = 300
</pre></div>

In your program, the field `conf.gui.window` gives an instance of `org.example.Conf.Gui.Window` with the fields `width` and `height`, and an additional field `$` that contains the value `Sample application` (a `java.lang.String` in that case, but any other type is allowed) :

```java
    System.out.println(conf.gui.window.$); // print "Sample application"
```

<a name="separateTypes"></a>

#### Separate types

In order to create a separate type (like for enums), write its type at the group level. The group level represents the class to create, and **ends with a dot**, like `gui.window.` :

<div class="source"><pre class="prettyprint properties">
gui.window. = #org.example.Window
gui.window.width = $int 500
gui.window.height = 300
</pre></div>

And without a fully qualified name, it's just a renaming from `org.example.Conf.Gui.Window` to `org.example.Conf.Gui.Application` :

<div class="source"><pre class="prettyprint properties">
gui.window. = #Application
gui.window.width = $int 500
gui.window.height = 300
</pre></div>

Do not confuse with the self value, that may also have its own type :

<div class="source"><pre class="prettyprint properties">
gui.window = $java.lang.String Sample application
gui.window. = #org.example.Gui
gui.window.width = $int 500
gui.window.height = 300
</pre></div>

<a name="rules"></a>

#### Rules for types

* `#org.example.Gui` means that we want to create a type that will hold all child properties.
* `$java.awt.Color` means that want a field that refers to an **existing type**.
* `#Application` means that we want to create a type with that name instead of the name of the property.

Except for renaming, always write classes with their fully qualified name. The Java source generated will take care of classes to import or not.

The directives `. = @Adapter.map()` are strings that may refer to other types ; those other types have to be explicitly imported with the `. = !org.acme.Foo` directive. Even when a type is explicitly imported, its name must be fully qualified in `$` references.

<a name="generator"></a>

### Generating the target class

The [`ml.alternet.properties.Generator`](apidocs/ml/alternet/properties/Generator.html) class can be used to generate the target classes from the template properties files :

```java
    File propertiesTemplatesDirectory = new File("file:///path/to/templates-dir/");
    File outputDirectory = new File("file:///path/to/target-dir/");
    new Generator()
        .setPropertiesTemplatesDirectory(propertiesTemplatesDirectory)
        .setOutputDirectory(outputDirectory)
        .generate();
```

But it is more easy to use the Maven plugin in `pom.xml`.

<a name="maven"></a>

#### Generating the target class with Maven

This [Maven Plugin](../prop-bind-maven-plugin/index.html) aims to create a bunch of classes that can be populated by `.properties` files :

```xml
  <build>
    <dependencies>
      <dependency>
        <groupId>ml.alternet</groupId>
        <artifactId>alternet-tools</artifactId>
        <version>1.0</version>
      </dependency>
    </dependencies>
    <plugins>
      <plugin>
        <groupId>ml.alternet</groupId>
        <artifactId>prop-bind-maven-plugin</artifactId>
        <version>1.0</version>
        <configuration>
        </configuration>
        <executions>
          <execution>
            <?m2e execute?>
            <id>generate</id>
            <goals>
              <goal>generate</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
```

The plugin accepts the parameters `propertiesDirectory` and `outputDirectory` that are set by default to, respectively, "`${basedir}/src/main/properties`" and "`${project.build.directory}/generated-sources/prop-bind`".

It will scan recursively the input directory and generate the Java source code in the output directory for each `.properties` file.

* Don't omit to add that directory to the Java source directories. In your IDE, such as Eclipse, you should edit the properties of your project and in "Java Build Path" add that directory to the sources.
* With Maven, the output directory will be added automatically to the sources to compile.

<a name="otherFeatures"></a>

### Other features

<a name="default"></a>

#### Default values

Before loading the property file, all the fields are empty (default value for primitives and null for objects), except for booleans that takes as a default value the one present in the template.

If you don't like default values for primitives, use the counterpart boxed type instead :

<div class="source"><pre class="prettyprint properties">
# the property will be null if the value is missing in the properties file
gui.window.width = $java.lang.Integer 500
</pre></div>

It is possible to use an existing object with default values, and load on it the property file. A typical usage would to use system properties as default values, which allow to pass default values when launching Java : `-Dgui.window.height = 500` :

```
    # start with the default System conf
    Conf conf = Conf.unmarshall(System.getProperties());
    # update with other values
    conf.update(new FileInputStream("conf.properties"));
```

or if you prefer to read first `conf.default.properties` and last to override the values with the System properties :

```
    # start with the default conf
    Conf conf = Conf.unmarshall(new FileInputStream("conf.default.properties"));
    # go on with other values
    conf.update(new FileInputStream("conf.properties"));
    # finish by overriding with System properties
    conf.update(System.getProperties());
```

<a name="merging"></a>

#### Merging separate property files

When loading properties, instead of having a single property file, it is possible to have multiple source files :

* "`conf.properties`" :

<div class="source"><pre class="prettyprint properties">
# Properties definining the GUI
gui.window.width = 500
gui.window.height = 300
gui.colors.background = #FFFFFF
gui.colors.foreground = #000080

# Properties definining datasources
service.url = https://some-services.example.org/someService
service.uuid = 5da66c77-7062-4b30-97fc-e747eb64570a

db = file:datasource/db.properties
</pre></div>

* "`datasource/db.properties`" :

<div class="source"><pre class="prettyprint properties">
# Properties definining DB datasource
driver = com.fakesql.jdbc.Driver
url = jdbc:fakesql://localhost:3306/localdb?autoReconnect=true
account.login = theLogin
account.password = thePassword
</pre></div>

Accessing a property in your Java program remains unchanged :

```java
    if ("admin".equals(conf.db.account.login)) {
        // TODO
    }
```

The file may be either a relative path **relative to the current directory** or an absolute path :

<div class="source"><pre class="prettyprint properties">
db = file:relative/path/to/db.properties
</pre></div>

<div class="source"><pre class="prettyprint properties">
db = file:///absolute/path/to/db.properties
</pre></div>

<a name="interpolation"></a>

#### Variable interpolation

Variable interpolation is available with Java EE Expression Language. In order to use this feature, the following dependency have to be added :

```xml
    <dependency>
        <groupId>org.glassfish</groupId>
        <artifactId>javax.el</artifactId>
        <version>3.0.1-b09</version>
    </dependency>
```

Only the variables already set can be referred :

<div class="source"><pre class="prettyprint properties">
# Properties definining the GUI
gui.window.height = 300
gui.window.width = ${gui.window.height + 200}
gui.colors.background = #F0F1F2
gui.colors.foreground = ${gui.colors.background.brighter()}
</pre></div>

<a name="renaming"></a>

#### Automatic renaming

If a property name is in conflict with Java naming rules (use of illegal characters, use of a keyword, name already existing in the same scope), the generator will create a valid name derived from the original name for you.

The generator will fail if the name is made of only illegal characters : in that case, just use a nice name for your properties.

