# Alternet Tools

<div class="nopub">
<a href="http://alternet.github.io/alternet-libs/tools/tools.html">
Published version of this page available HERE</a></div>

**Alternet Tools** include discovery service tools, properties binder and classes generator, concurrent and locking tools, and more.

## Overview

Alternet Tools contains mainly :

* [a properties binder and its classes generator](properties.html)
* [a discovery service](discovery.html) that can be optionally used with injection (JSR330)
* [a class generator](../tools-generator/generator.html)
* concurrent and locking tools
* and additional utility classes

This documentation covers the two latter topics.

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

## Some useful tools

### CharRange

Allow to define ranges of Unicode characters, merge them, exclude some, etc :

```java
    // ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/
    CharRange base64 = 
        CharRange.range('A', 'Z')
        .union(
            CharRange.range('a', 'z'),
            CharRange.range('0', '9'),
            CharRange.is('+'),
            CharRange.is('/')
        );
    CharRange others = CharRange.ANY.except(base64);
```

* [`CharRange`](apidocs/ml/alternet/misc/CharRange.html)

### Thrower

Allow to cast (**without wrapping**) any exception to a `RuntimeException`.

Helpful for lambdas. For example, a method that accept a supplier :

```java
    takeIt(() -> {
        try {
            // do some I/O
        } catch (IOException e) {
            return Thrower.doThrow(e);
        }
    });
```
* [`Thrower`](apidocs/ml/alternet/misc/Thrower.html)

### Type

Allow to work on Java types even on those that doesn't exist :

```java
    Type listOfInts = Type.of(java.util.List.class).withTypeParameters(Type.of(int.class));
    // gives java.util.List<java.lang.Integer>

    Type type = Type.parseTypeDefinition("org.acme.Foo<java.util.Map<?, ? super java.lang.Integer>,com.example.Bar[],java.lang.Appendable>");
    // gives it...
```

* [`Type`](apidocs/ml/alternet/misc/Type.html)

### Enum extension

Java `Enum`s are not designed to be extended, which prevents using an illegal value (one of the child type enum) on an enum variable that would be of the parent enum type. However, it appears that sometimes we do want to extend an enum. `EnumUtil::extend` can copy existing enum values to a new enum :

```java
    public enum A {
        a, b, c;
    }

    public enum Z {
        x, y, z;
        static {
            EnumUtil.extend(A.class);
        }
        // Z : (a, b, c, x, y, z)
    }
```

#### Exemple of 2 enums merged in a 3rd

```java

    public static enum Weekday {
        MON, TUE, WED, THU, FRI;
    }

    public static enum WeekendDay {
        SAT, SUN;
    }

    public static enum DayOfWeek {
        MON; // enum expect at least one value

        static {
            EnumUtil.extend(Weekday.class);
            EnumUtil.extend(WeekendDay.class);
        }

        // DayOfWeek contains MON, TUE, WED, THU, FRI, SAT, SUN
    }
```

It may be helpful to inherit some behaviours of the other enums :

```java
    public static enum DayOfWeek {
        MON; // enum expect at least one value

        private boolean isWeekendDay = false;

        public boolean isWeekendDay() {
            return this.isWeekendDay;
        }

        private DayOfWeek() { }

        private DayOfWeek(WeekendDay weekendDay) {
            this.isWeekendDay = true;
        }

        static {
            EnumUtil.extend(Weekday.class);
            EnumUtil.extend(WeekendDay.class);
        }
    }
```

If a value of the new enum already exist in the base enum, the new behaviour override the existing one.

* [`EnumUtil`](apidocs/ml/alternet/util/EnumUtil.html)

### JAXBStream

Allow to stream repeatable elements from an XML document to objects, with eventually a cache strategy :

```xml
<list xmlns="http://example.com/items">
    <item>...</item>
    <item>...</item>
    <item>...</item>
    ...
</list>
```

For unmarshalling `<item>`s to a stream of `example.com.Item`, use :

```java
    new JAXBStream<Item>("http://example.com/items.xml", 
            new QName("http://example.com/items", "item"), 
            Item.class, 
            JAXBStream.CacheStrategy.noCache)
        .stream().forEach(item -> {
            // TODO
        });
```

* [`JAXBStream`](apidocs/ml/alternet/misc/JAXBStream.html)

### Concurrent and locking tools

The concurrent package allows to manage mutexes and locks, based on a string ID, safe for
synchronization for a given user context.

As mentionned in section 3.10.5 of the Java Language Spec 2.0 :
> "Literal strings within different classes in different packages likewise represent references to the same String object."

The [`MutexContext`](apidocs/ml/alternet/concurrent/MutexContext.html) class
allow to create a context in order to manage safe string-based mutexes, that
is to say without the inherent conditions of the String class that can lead
to a dead-lock.

This class is not magic:

* It's up to the user to create a context per domain to avoid ID
collisions. The scope of such domain depends on the application.
* It can't avoid dead-lock due to programming (it only avoids the inherent
possibility of dead-locks due to string usage)

A mutex or a lock doesn't have to be explicitly remove from its context, it
will be automatically removed after all references to it are dropped.

```java
   MutextContext mutexContext = new MutextContext();
   // ...
   
   String id = someObject.getCanonicalID();
   Mutex mutex = mutexContext.getMutex(id);
   synchronized(mutex) {
      // ...
   }
```

Instead of synchronizing on a [`Mutex`](apidocs/ml/alternet/concurrent/Mutex.html)
it is also possible to get one of the available lock :

* [`Lock`](apidocs/ml/alternet/concurrent/MutexContext.html#getLock-java.lang.String-)
* [`ReadWriteLock`](apidocs/ml/alternet/concurrent/MutexContext.html#getReadWriteLock-java.lang.String-)
* [`StampedLock`](apidocs/ml/alternet/concurrent/MutexContext.html#getStampedLock-java.lang.String-)

## Other tools

* [a properties binder and its classes generator](properties.html)
* [a discovery service](discovery.html) that can be optionally used with injection (JSR330)
* [a class generator](../tools-generator/generator.html)
