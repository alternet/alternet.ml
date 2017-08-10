Alternet Libraries are built, assembled and installed using Maven.
They are deployed to the Maven Central repository.

Developers using maven are likely to find it easier to include and
manage dependencies of their applications. This document will explain
to both maven and non-maven developers how to use Alternet Libraries
in their applications. 

In general, if you're not using Maven, you'd need to
download dependencies (jar files) directly from the Maven repository and
include it in the classpath.

## Maven

If you want to compile an application with Alternet Libs, declare
a dependency on the relevant alternet module in maven project.

### Alternet Parsing

```xml
<dependency>
    <groupId>ml.alternet</groupId>
    <artifactId>alternet-parsing</artifactId>
    <version>1.0</version>
</dependency>
```

### Alternet Scanner

```xml
<dependency>
    <groupId>ml.alternet</groupId>
    <artifactId>alternet-scanner</artifactId>
    <version>1.0</version>
</dependency>
```

### Alternet Security

```xml
<dependency>
    <groupId>ml.alternet</groupId>
    <artifactId>alternet-security</artifactId>
    <version>1.0</version>
</dependency>
```

### Alternet Tools

```xml
<dependency>
    <groupId>ml.alternet</groupId>
    <artifactId>alternet-tools</artifactId>
    <version>1.0</version>
</dependency>
```

## Non-Maven project

For non-maven projects, the [Alternet API jar](http://search.maven.org/#search|ga|1|g%3A%22ml.alternet%22%20AND%20p%3A%22jar%22) can be downloaded and included in the classpath for compiling the applications.
