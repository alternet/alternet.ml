# Alternet Tools - Generator

<div class="nopub">
<a href="http://alternet.ml/alternet-libs/tools-generator/generator.html">
Published version of this page available HERE</a></div>

This generator serves to create an instance of an interface. Its methods won't be implemented.

<div style="columns: 2">
<div>
<h3 style="margin: 0">Maven import</h3>
<pre class="prettyprint"><![CDATA[
<dependency>
    <groupId>ml.alternet</groupId>
    <artifactId>alternet-tools</artifactId>
    <version>1.0</version>
</dependency>]]>
</pre>
</div>
<div style="break-before: column">
<h3>JavaDoc API documentation</h3>
<ul><li><a href="apidocs/index.html">Alternet Tools</a></li></ul>
<p>Other Alternet APIs :</p>
<ul><li><a href="../apidocs/index.html">Alternet Libs</a></li></ul>
</div>
</div>

## Usage

In fact, this generator tool is rather a generator (at compile time) of a generator (at runtime)
of byte code of classes that implement a given interface.

Ensure to add the following build plugin to your POM :

```xml
<plugin>
    <artifactId>maven-compiler-plugin</artifactId>
    <executions>
        <execution>
          <id>compile-generated</id>
          <phase>compile</phase>
          <goals>
            <goal>compile</goal>
          </goals>
          <configuration>
            <compilerArgument>-proc:none</compilerArgument>
          </configuration>
        </execution>
    </executions>
</plugin>
```

Then get an instance of `ByteCodeFactory` :

```java
    @ByteCodeSpec
    private static ByteCodeFactory BYTECODE_FACTORY = ByteCodeFactory
        .getInstance("ml.alternet.util.ByteCodeFactory$"); // exist after code generation
```

or :

```java
    @ByteCodeSpec(factoryClassName="FooFactory$", factoryPkg="org.acme.bytecode",
        parentClass=SomeClass.class, singletonName="FOO_INSTANCE",
        template="/org/acme/bytecode/FooFactory$.java.template")
    private static ByteCodeFactory BYTECODE_FACTORY = ByteCodeFactory
        .getInstance("org.acme.bytecode.FooFactory$"); // exist after code generation
```

Then, given an interface `SomeInterface`, we can get an instance of that interface, despite no known 
implementation is available :

```java
SomeInterface instance = BYTECODE_FACTORY.newInstance(SomeInterface.class);
```

The underlying class of this instance doesn't implement the methods, if any, of the interface.

We can also get the singleton :

```java
SomeInterface instance = BYTECODE_FACTORY.getInstance(SomeInterface.class);
```

The singleton has the name given by the value `singletonName` of the annotation `@ByteCodeSpec`.
That annotation can also specify a `parentClass` (must have a zero argument constructor), which can be abstract.
A `template` can be used to generate the Java code.

More or less, the result is the same as what you would get by writing :

```java
SomeInterface instance = new SomeInterface() {
    // but you must have all the methods implemented here
};
```

Therefore, this generator is just a convenient shortcut for getting such instances.
