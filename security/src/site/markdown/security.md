# Alternet Security

Alternet Security aims to enhance security on passwords handled in the JVM.

## Maven import

```xml
<dependency>
    <groupId>ml.alternet</groupId>
    <artifactId>alternet-security</artifactId>
    <version>1.0</version>
</dependency>
```

## JavaDoc API documentation

* [Alternet Security](apidocs/index.html)

Other Alternet APIs :

* [Alternet Libs](../apidocs/index.html)

## Usage

The idea is to keep low the period where a password
appeared in clear in the memory, in order to make it
difficult to find when a memory dump is performed.

A password can be created thanks to a [PasswordManager](apidocs/ml/alternet/security/PasswordManager.html), that
exist in several flavors. To pick one, use the [PasswordManagerFactory](apidocs/ml/alternet/security/PasswordManagerFactory.html)
or supply your own implementation (your own implementation can
override the default one with the [discovery service](../tools/tools.html)).

### Password creation

``` java
// pick one of the available password manager
// (replace XXX with the one you prefer)
PasswordManager manager = PasswordManagerFactory.getXXXPasswordManager();
Password pwd = manager.newPassword(pwdChars);
// from this point,
// pwd is safe for staying in memory as long as necessary,
// pwdChars has been unset after the creation of the password.
```

### Typical usage

``` java
try (Password.Clear clear = pwd.getClearCopy()) {
    char[] clearPwd = clear.get();
    // use clearPwd in the block
}
// at this point clearPwd has been unset
// before being eligible by the garbage collector
```

The user has to ensure to keep the try-with-resource
block as short as possible, and to not make copies of
the char array if possible.

## About passwords

The [Crypto Specification](http://docs.oracle.com/javase/8/docs/technotes/guides/security/crypto/CryptoSpec.html#PBEEx)
states that since as String is immutable (and subject to be stored in a
pool by the JVM), char arrays are preferred for storing password.

Usually, a password may be used for accessing
a resource ; before and after such access, the intermediate
data used as the password should not remain in memory :
the idea of the [Password](apidocs/ml/alternet/security/Password.html) class is to allow a long-term
store of the password between two resource accesses.
Since the Password class obfuscate the data, if the memory is dumped
it will be difficult to guess which part of the memory was
a password. The idea is to limit usage of the clear password
outside of the Password class.

In many cases, the Password will be built from data
that are coming from a string (such as an URI), it is
recommended to not keep a strong reference to such data
