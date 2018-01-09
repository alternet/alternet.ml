# Discovery Service

<div class="nopub" style="padding: 10px; color: #586069; background-color: #f1f8ff; border: 1px solid #c8e1ff;">
<a href="http://alternet.ml/alternet-libs/tools/discovery.html">
Published version of this page available HERE</a></div>

Alternet Tools include discovery service tools.

## Maven import

```xml
<dependency>
    <groupId>ml.alternet</groupId>
    <artifactId>alternet-tools</artifactId>
    <version>1.0</version>
</dependency>
```

## JavaDoc API documentation

* [Alternet Tools](apidocs/index.html)

Other Alternet APIs :

* [Alternet Libs](../apidocs/index.html)

## Discovery Service

The [DiscoveryService](apidocs/ml/alternet/discover/DiscoveryService.html) class
is a component for retrieving classes that support the following features :

* several variant implementations can be defined aside
* automatic generation of the bindings
* support of injection (JSR330) with :
    * out-of-the-box pre-defined qualifier
    * automatic generation of producer classes

## Overview

Consider that "`Protocol`" is an interface ; a specific variant of 
this interface can be retrieved with :

```java
    Protocol protocol = DiscoveryService.newInstance("org.acme.Protocol/http");
```

The more convenient way to bind an implementation is to define the file
`META-INF/xservices/org.acme.Protocol/http` that might contain :

```
org.acme.protocols.HttpProtocol
```

which can be generated for you with a simple annotation :

```java
@LookupKey(forClass=org.acme.Protocol.class, variant="http")
public class HttpProtocol implements Protocol {
    // ...
}
```

### Lookup keys and variants

The lookup key is usually the fully qualified name of a class (usually
abstract or an interface) : `org.acme.Protocol`, and the resolved class
name should be a concrete implementation of it; the lookup key can also be a
name, even a JNDI name.

Inner class names contains a $ sign in their name ; all lookup are
performed on keys where the $ sign is replace by a dot. If you define
manually a `META-INF/xservices/` entry, don't write file names or directory
names with a dollar, but with a dot instead.

The lookup key can be supplied with a variant, in order to bind several
implementations (this has to be taken in charge by the caller); for example,
`org.acme.SerializerFactory/image/png`. Basically, it means that
we want a factory that can supply a serializer for `image/png`.

* An implementation can be render either as a raw `Class` or as a singleton 
instance (or both across multiple calls)
* The class lookup is done *once* for every class name to discover.
* Singletons are instanciated by their own method "`newInstance()`" if they have one, or by
their default zero argument constructor. If the singleton fails to
instanciate, the user would still have the possibility to get the raw class
and build itself an instance.

### Class localization

An implementation is looked up from the key supplied as follows (in order):

1. The value of the system property with the name of the key supplied if it exists and is accessible.
1. The value of the JNDI property with the name of the key supplied prepend
with `java:comp/env/` if it exists and is accessible; if the
resolved object is a string, it stands for a class name, otherwise for the
instance to return.
1. The value of the init parameter of the Web application with the name of
the key supplied if it exists and is accessible (&lt;web-app&gt; &lt;context-param&gt;)
During its initialization, the web application must have registered a
filter `ml.alternet.web.WebFilter`.
1. The contents of the file `discoveryService.properties` of the
current directory if it exists.
1. The contents of the file `$USER_HOME/discovery-service.properties` if it exists.
1. The contents of the file `$JAVA_HOME/jre/lib/discovery-service.properties` if it exists.
1. The Jar Service Provider discovery mechanism specified in the Jar File
Specification, and ammended by the special use of this class (see below).
A jar file can have a resource (i.e. an embedded file) such
as `META-INF/xservices/package.Class` (or `META-INF/xservices/package.Class/variant`
if the key contains a variant) containing the name of the concrete class to instantiate.
1. The fallback default implementation, which is given by
the `META-INF/xservices/` of the user library (services found with a line
of comment before or ending with a comment that contains `# default` will be
processed at the end)

The first value found is returned. If one of those method fails, the next is tried.

### `META-INF` services and xservices

The location used by this tool is `META-INF/xservices/`, not `META-INF/services/`.
Actually, the common `META-INF/services/`
directory is ruled by different conventions than those used here
(specifically due to the "variant" of the key). To avoid confusion, another
directory name has been retained, actually `xservices`(that stands for
"extended services").

### Self-registration

The [@LookupKey](apidocs/ml/alternet/discover/LookupKey.html) annotation can be
used in order to generate automatically an entry under `META-INF/xservices/` :

When no variant is specified, the annotation : 

```java
@LookupKey(impClass=org.acme.tools.ToolsImpl.class)
interface Tools {
    // ... 
}
```

…will generate the entry in `META-INF/xservices/org.acme.tools.Tools` that contains :

```
 # default
 org.acme.tools.ToolsImpl
```

A variant can also be specified in the lookup key annotation. The default directive
can be unset. The annotation can be placed on the target implementation.

```java
@LookupKey(impClass=org.acme.protocols.HttpProtocol.class, variant="http")
interface Protocol {
    // ...
}
```

…will generate the entry in `META-INF/xservices/org.acme.Protocol/http` that contains :

```
 # default
 org.acme.protocols.HttpProtocol
```

### Injection

Two additional annotations can also be used with a CDI container (JSR330).

The discovery service can be used without a CDI container, but
if you want to inject classes to lookup (specifically with a variant), the
annotations supplied may help.

Even if you used a CDI container, you are not compelled to inject every
classes to lookup, just the ones you want to or have to inject.

- As mentioned previously, [@LookupKey](apidocs/ml/alternet/discover/LookupKey.html)
generates an entry in `META-INF/xservices/`

Additionally :

- [@Injection.LookupKey](apidocs/ml/alternet/discover/Injection.LookupKey.html) qualifies an instance to inject
- [@Injection.Producer](apidocs/ml/alternet/discover/Injection.Producer.html) generates a class producer for injections

#### Example : start situation (without injection)

For example, consider a constructor :

```java
public A(B b) {
    // ...
}
```

where B is an interface :

```java
public interface B {
    // ...
}
```

which has several implementations :

```java
@LookupKey(forClass = B.class, variant = "greedy")
public class B1 implements B {
    // ...
}
```

and :

```java
@LookupKey(forClass = B.class, variant = "lazy")
public class B2 implements B {
   // ...
}
```

You want to inject the right class ? See below...

#### Target situation (with injection)

First, in the constructor (or directly in the field, up to you) one have to
indicate that an instance have to be injected :

```java
@javax.inject.Inject
public A(@Injection.LookupKey("standard") B b) {
    // ...
}
```

Then you have to set on the interface which variant you want to produce :

```java
@Injection.Producer(variant = "standard", lookupVariant="lazy")
public interface B {
    // ...
}
```

A producer class will be generated at compile time, that will perform the
lookup with the specified variant. At runtime, the container will inject the
instance supplied by the producer that matches both the expected target class
and the variant, if any.

#### Other usage

It is also possible to omit the variant to inject.

It is also possible to use the same variant at the injection point :

```java
@javax.inject.Inject
public A(@Injection.LookupKey("lazy") B b) {
    // ...
}
```

...and accordingly to generate a relevant producer :

```java
@Injection.Producer(variant = "lazy")
public interface B {
    // ...
}
```

Sometimes, the developer doesn't own the type to inject neither their
implementations ; it is still possible to set the annotations on some code
owned by the developer, for example one of its package and specify both the
implementation and the target class.
