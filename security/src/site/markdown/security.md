## Passwords in Java

The [Crypto Specification](http://docs.oracle.com/javase/8/docs/technotes/guides/security/crypto/CryptoSpec.html#PBEEx)
states that since as String is immutable (and subject to be stored in a
pool by the JVM), char arrays are preferred for storing password.
Additionally, when a String password is no longer used, it won't be necessarily
reclaimed immediately by the garbage collector ; in the meantime it is impossible
to unset its characters. Strings are unsafe for storing passwords.

Usually, a password may be used for granting an access to
a resource ; before and after granting such permit, the intermediate
data used as the password should not remain in memory :
the idea of the [Password](apidocs/ml/alternet/security/Password.html) class is to allow a long-term
storage of the password between its creation and when it is used.
The [Password](apidocs/ml/alternet/security/Password.html) class aims to
obfuscate the characters of a password, if the memory is dumped
it will be difficult to guess which part of the memory was
a password, and even to deobfuscate it when it has been encrypted.
The idea is to limit usage of the clear password
outside of the [Password](apidocs/ml/alternet/security/Password.html) class.

In some cases, passwords will be built from data that are coming from
a String. In this case it is recommended to not keep a strong reference
to such data, but be aware that such practice create a flaw in your
environment.

In Alternet Security, passwords are always built from characters,
never from String. Additionally, Web facilities are supplied to ensure
that when a Web server is receiving a password, a String is never
built from the incoming data. The obfuscation method used by default
in Web applications is the strongest obfuscation method in Alternet
Security, which relies on cryptography.

You will find in Alternet Security 3 mains features :

1. Means to [handle secure passwords](#passwords)
2. A nice [authentication framework](#auth) that relies on such passwords
3. Extensions for [Web applications](#webapps) with implementations for [Tomcat](#tomcat) and [Jetty](#jetty) web servers

<a name="passwords"></a>

## JavaDoc API documentation

* [Alternet Security](apidocs/index.html)
* [Alternet Security Authentication](../security-auth/apidocs/index.html)
* [Alternet Security for Jetty](../security-jetty-9.1/apidocs/index.html)
* [Alternet Security for Tomcat](../security-tomcat-8.0/apidocs/index.html)

All Alternet APIs :

* [Alternet Libs](../apidocs/index.html)

# Secure passwords handling

## Maven import

```xml
<dependency>
    <groupId>ml.alternet</groupId>
    <artifactId>alternet-security</artifactId>
    <version>1.0</version>
</dependency>
```

Additional Maven modules are available for using Alternet Security in Tomcat or Jetty (see below).

## JavaDoc API documentation

* [Alternet Security](apidocs/index.html)

All Alternet APIs :

* [Alternet Libs](../apidocs/index.html)

## Usage

The idea is to keep low the period where a password
appeared in clear in the memory, in order to make it
difficult to find when a memory dump is performed.

A password can be created thanks to a [PasswordManager](apidocs/ml/alternet/security/PasswordManager.html), that
exist in several flavors (weak, default, or strong with encryption).
To pick one, use the [PasswordManagerFactory](apidocs/ml/alternet/security/PasswordManagerFactory.html)
or supply your own implementation (your own implementation can
override the default one with the [discovery service](../tools/tools.html)).

<div class="alert alert-danger" role="alert">
Once again : DO NOT CREATE A STRING OBJECT WITH THE PASSWORD CHARACTERS.
</div>

### Password creation

``` java
char[] pwdChars = ... // this is the password
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

<a name="auth"></a>

# Authentication framework

Credentials are informations supplied by a user that
are used to authenticate him on an application.
Typical credentials are the user login, its password,
and optionally a realm or domain, but the more often
only the password is used for authentication, although
some tools are computing hashes with the login name.

Alternet Security Authentication supply a framework for
implementations that reasonably clean sensible data from the memory after
the hash is computed, which can't be guaranteed by Java
standard packages.

## Maven import

This module (or any other implementation) is required for
performing credentials verification, such as in Web applications
(see below).

```xml
<dependency>
    <groupId>ml.alternet</groupId>
    <artifactId>alternet-security-auth</artifactId>
    <version>1.0</version>
</dependency>
```

## JavaDoc API documentation

* [Alternet Security Authentication](../security-auth/apidocs/index.html)

All Alternet APIs :

* [Alternet Libs](../apidocs/index.html)

## Project page

* [Project page](../security-auth/index.html)

## Usage

Alternet Security Authentication comes with out-of-the-box popular hashers and
crypt formatters, including legacy algorithms such as Unix crypt.
The latter should be used only if you still have old passwords to check.
Consider using moderns hashers instead such as PBKDF2 or BCrypt.

3 main classes are supplied :

 * [Credentials](../security/apidocs/ml/alternet/security/auth/Credentials.html), which is roughly a wrapper around `Password`
 * [Hasher](../security-auth/apidocs/ml/alternet/security/auth/Hasher.html) which allow to compute a crypt, and check some credentials given a crypt
 * [CryptFormatter](../security-auth/apidocs/ml/alternet/security/auth/formats/CryptFormatter.html) which allow to turn the bytes of a hash to a string crypt, or to parse a crypt in its parts

### Generate a hash

Most common hashers are available and parameterized in :

 * [CurlyBracesCryptFormatHashers](../security-auth/apidocs/ml/alternet/security/auth/hashers/CurlyBracesCryptFormatHashers.html)
 * [ModularCryptFormatHashers](../security-auth/apidocs/ml/alternet/security/auth/hashers/ModularCryptFormatHashers.html)
 * [UnixHashers](../security-auth/apidocs/ml/alternet/security/auth/hashers/UnixHashers.html)

If the default configuration doesn't suit your needs, you can alter
any supported parameter.

Pick a hasher and set its parameters from a formatter family. For example,
let's hash "`password`" with PBKDF2,  set a custom number of iterations,
and format it in curly braces :

"`password`" -&gt; "`{PBKDF2}131000$tLbWWssZ45zzfi9FiDEmxA$dQlpmhY4dGvmx4MOK/uOj/WU7Lg`"

``` java
Password pwd = ... // see above
Credentials credentials = Credentials.fromPassword(pwd);
                       // or         .fromUserPassword(user, pwd)

// pick a hasher
Hasher hasher = CurlyBracesCryptFormatHashers.PBKDF2.get()
        .setIterations(15000) // before build, you can change the parameters
        .build();

// create a crypt
String crypt = hasher.encrypt(credentials);

// now you can store it in your database
```

### Check the credentials

When checking some credentials, you don't need to alter parameters such as the
number of iterations, since when supported they are encoded in the string crypt.

Typically you will use the same hasher for creating a crypt or checking credentials :

``` java
Password pwd = ... // see above
Credentials credentials = Credentials.fromPassword(pwd);
                       // or         .fromUserPassword(user, pwd)

String crypt = ... // some hash stored in a database

// get the hasher
Hasher hasher = CurlyBracesCryptFormatHashers.PBKDF2.get()
        .build(); // no need to change the iteration, it is encoded in the crypt

if (hasher.check(credentials, crypt)) {
    // authentication succeeds
} else {
    // authentication fails
}
```

Sometimes, you may have existing crypts available in various formats.
For that purpose, a more suitable credentials checker (see [CredentialsChecker](../security-auth/apidocs/ml/alternet/security/auth/CredentialsChecker.html))
will lookup for the right hasher.

For example, the following credentials checker will try all the formats in sequence.
Note in the example the `PlainTextCryptFormat` is used as the last attempt to check clear passwords,
and should not be used in production.

``` java
CredentialsChecker checker = new CredentialsChecker.$(
    ModularCryptFormat, 
    CurlyBracesCryptFormat, 
    PlainTextCryptFormat
);
if (checker.check(credentials, crypt)) {
    // authentication succeeds
} else {
    // authentication fails
}
```

<a name="webapps"></a>

# Alternet Security for Web applications

Alternet Security for Web applications allow handling safe passwords inside Web applications :
during the Web processing chain, a password NEVER appear as a String (unsafe) inside the server.

It consist on two parts :

* Using safe password in servlet-based applications or RESTful (JAX-RS) applications (included in the first Maven module)
* Enhancing an existing Web container ([Jetty](#jetty), [Tomcat](#tomcat)) to make that feature available in Web applications (specifics Maven modules are supplied).

The default password manager used in Web application is the strong password manager that encrypt passwords.

According to the Web container ([Jetty](#jetty), [Tomcat](#tomcat)) in use, additional configurations are expected (see below).

## Web applications

Safe passwords can be handled in the same way whatever the concrete underlying Web container.

In web applications, a password can be sent to the server :

* For authentication (with HTTP Basic or Form authentications)
* For other specific process, such as the registration of a new user account, or for changing a user password.

Note that sending a form with a password (for login, for registration, for changing a
user password or for any other purpose) have to be performed with HTTP POST ; sending
a password with HTTP GET is unsecure and therefore not available with Alternet Security.

The Web application have to be configured (`web.xml` file) with values that takes
the shape of a path, as shown follow :

```XML
<context-param>
    <param-name>ml.alternet.security.web.config.formFields</param-name>
    <param-value>
          /doRegister.html?pwd&confirmPwd,
          /doUpdatePassword.html?oldPwd&newPwd&confirmPwd
    </param-value><!-- didn't you forget to XML-escape the '&' characters above ? -->
</context-param>
<context-param>
    <param-name>ml.alternet.security.web.config.authenticationMethod</param-name>
    <param-value>Basic</param-value>
</context-param>
```

The former context parameter consist of a comma-separated sequence of
paths relative to the context path ; those paths are the target
destination to which form data are posted, and that indicates in
their query string the fields of the form parameters that contains
the passwords to capture.

The latter context parameter indicates the authentication method ;
setting "Basic" indicates to capture passwords
during HTTP Basic Authentication ; setting "Form" indicates to capture
passwords during HTTP Form Authentication (in this case no need to mention
the path and password fields if they are standard values). If this parameter
is missing, no login will be handled by Alternet Security.

Once configured properly (see below ([Jetty](#jetty) and [Tomcat](#tomcat)) configurations),
the Web container will substitute the passwords found in the form fields or HTTP authentication header
with '*' characters, in order to make such strings unusable ; the actual passwords will be wrapped in
[Password](apidocs/ml/alternet/security/Password.html) instances, and will be available like shown hereafter :

### Servlets applications

To retrieve the passwords in a Servlet, use the [Passwords](apidocs/ml/alternet/security/web/Passwords.html) class :

```java
public void doPost(HttpServletRequest req, HttpServletResponse resp) {
    // retrieve a single password of a form field
    Password pwd = Passwords.getPasswords(req, "pwdField");
    // ...
}
```

The [PasswordParam](apidocs/ml/alternet/security/web/PasswordParam.html) class represent
a sequence of passwords, since fields (in Web forms or in HTTP headers) can be multivalued :

```java
public void doPost(HttpServletRequest req, HttpServletResponse resp) {
    // retrieve all passwords of a multivalued field
    PasswordParam allPwd = Passwords.getPasswords(req, "pwdField");
    for (Password pwd: allPwd) {
        // ...
    }
}
```

### RESTfull applications (JAX-RS)

Simply use the [Password](apidocs/ml/alternet/security/Password.html) class
(or the [PasswordParam](apidocs/ml/alternet/security/web/PasswordParam.html) class
for handling multiple values) in the parameter of a REST method :

<div class="alert alert-danger" role="alert">
Don't use <code>List&lt;Password&gt;</code> for handling multiple values, use
<code>PasswordParam</code> instead.
</div>

```java
@POST
public String getSomeData(@FormParam("username") String userName,
                          @FormParam("pwdField") Password pwd) {
    // ...
}
```

Note that the [Password](apidocs/ml/alternet/security/Password.html) instance
as well as the [PasswordParam](apidocs/ml/alternet/security/web/PasswordParam.html)
instance are supplied thanks to the [PasswordConverterProvider](apidocs/ml/alternet/security/web/PasswordConverterProvider.html)
annotated as a provider. According to the JAX-RS specification, this provider has to
be registered to the concrete JAX-RS engine (this can be done either manually or discovered
automatically ; please check the documentation of your JAX-RS engine).

## Web containers

Specific submodules supply means for the Web container to capture the received passwords.

To ensure security, passwords received by the server are NEVER stored in a String.

The passwords are captured from the raw input data stream (char or bytes) and
stored in a safe password object ; the raw data are replaced by a dummy value
(typically '*' characters) that will appear in Strings that would be handled
uselessly by the application. Of course, this value can't be used in the
application, instead, the safe password object will be injected in the Web
application.

<a name="jetty"></a>

### Jetty

Note that Jetty provides a feature for obfuscating passwords, but it doesn't serve the same
purpose : only the passwords allowing to access data sources are obfuscated, and this
is performed in order to store them on the file system aside the application
configuration (typically, the password that allow to access a database).
Jetty doesn't supply any mean to handle passwords from HTTP requests the way Alternet Security does.

* Require Java 8+ and Jetty 9.1+.
* Jetty usually use buffers that are large enough to process passwords handling in a single chunk of data.

The [Jetty tests](../security-jetty-9.1/surefire-report.html) show how a password is captured
([source code](https://github.com/alternet/alternet.ml/blob/master/security-jetty-9.1/src/test/java/ml/alternet/test/security/web/jetty/RESTTest.java)).

#### Maven import

Don't forget to also import the authentication module for checking the credentials (see above).

Jetty 9.1 and 9.2 :

```xml
<dependency>
    <groupId>ml.alternet</groupId>
    <artifactId>alternet-security-jetty-9.1</artifactId>
    <version>1.0</version>
</dependency>
```

Jetty 9.3 :

```xml
<dependency>
    <groupId>ml.alternet</groupId>
    <artifactId>alternet-security-jetty-9.3</artifactId>
    <version>1.0</version>
</dependency>
```

#### JavaDoc API documentation

* [Alternet Security for Jetty](../security-jetty-9.1/apidocs/index.html)

All Alternet APIs :

* [Alternet Libs](../apidocs/index.html)

#### Project page

* [Project page](../security-jetty-9.1/index.html)

#### Jetty programmatic configuration

Configuring Jetty is very simple :

* set a parameter to the Web app context to tell
which paths and form fields to intercept in the HTTP requests
(like in the `web.xml` configuration file, see above),
* create a [AltHttpConnectionFactory](../security-jetty-9.1/apidocs/ml/alternet/security/web/jetty/AltHttpConnectionFactory.html)
* and bound it to the Jetty server connector.

```Java
    WebAppContext wac = new WebAppContext();
    // configure for handling the "pwd" field when POSTing on the "/doRegister.html" path
    wac.setInitParameter(Passwords.FORMS_INIT_PARAM, "/doRegister.html?pwd");

    Server server = new Server();
    AltHttpConnectionFactory cf = new AltHttpConnectionFactory(server);

    ServerConnector connector = new ServerConnector(server, cf);
    connector.setPort(port);
    server.setConnectors(new Connector[]{connector});

    // do things normally with Jetty
    ServletHolder sh = ... // your servlet

    ServletHandler servletHandler = new ServletHandler();
    servletHandler.addServletWithMapping(sh, "/*");

    wac.setServletHandler(servletHandler);
    wac.setResourceBase(resourceBase); // static resources
    wac.setContextPath("/app");

    server.setHandler(wac);

    server.start();
```

Various full working examples of programmatic configurations are available in the [project test pages](https://github.com/alternet/alternet.ml/blob/master/security-jetty-9.1/src/test/java/ml/alternet/test/security/web/jetty/).

#### Jetty XML configuration

Instead of configuring the Jetty server with :

```XML
    <New class="org.eclipse.jetty.server.HttpConnectionFactory"/>
```

...you have to replace it with :

```XML
    <New class="ml.alternet.security.web.jetty.AltHttpConnectionFactory">
        <Arg><Ref refid="Server"/></Arg>
    </New>
```

#### Native Jetty Authentication

So far, the configuration above will only capture passwords.
If you want let Jetty to perform authentication, you need additional configuration,
according to the underlying authentication mechanism.

##### LDAP authentication

Here is the full XML configuration file :

```XML
<Configure id="Server" class="org.eclipse.jetty.server.Server">
  <Call name="addBean">
    <Arg>
       <New class="org.eclipse.jetty.jaas.JAASLoginService">
         <Set name="name">Alternet Realm</Set>
         <Set name="loginModuleName">ldap</Set>
       </New>
    </Arg>
  </Call>
  <Call name="addConnector">
    <Arg>
      <New id="httpConnector" class="org.eclipse.jetty.server.ServerConnector">
        <Arg name="server"><Ref refid="Server" /></Arg>
        <Arg name="factories">
          <Array type="org.eclipse.jetty.server.ConnectionFactory">
            <Item>
              <New class="ml.alternet.security.web.jetty.AltHttpConnectionFactory">
                <Arg><Ref refid="Server"/></Arg>
              </New>
            </Item>
          </Array>
        </Arg>
      </New>
    </Arg>
  </Call>
</Configure>
```

The JAAS login module, (typically in `${jetty.base}/etc/ldap.conf`) will look like :

```
ldap {
    ml.alternet.security.web.jetty.auth.AltLdapLoginModule required

    useSSL="false"
    debug="true"
    contextFactory="com.sun.jndi.ldap.LdapCtxFactory"
    hostname="localhost"
    port="10389"

    bindDn="ou=people,dc=alternet,dc=ml"
    bindPassword="secret"
    authenticationMethod="simple"
    forceBindingLogin="false"
    ml.alternet.security.auth.CryptFormat="ml.alternet.security.auth.formats.ModularCryptFormat, ml.alternet.security.auth.formats.CurlyBracesCryptFormat, ml.alternet.security.auth.formats.PlainTextCryptFormat"

    userBaseDn="ou=people,dc=alternet,dc=ml"
    userRdnAttribute="uid"
    userIdAttribute="uid"
    userPasswordAttribute="userPassword"
    userObjectClass="inetOrgPerson"

    roleBaseDn="ou=groups,dc=alternet,dc=ml"
    roleNameAttribute="cn"
    roleMemberAttribute="member"
    roleObjectClass="groupOfNames";

};
```

If `forceBindingLogin="true"`, it means that the LDAP server will perform the check
of the credentials ; in that case the `ml.alternet.security.auth.CryptFormat` parameter may be omitted.

If `forceBindingLogin="false"`, it means that Jetty will retrieve the hash from the
LDAP server and perform the check of the credentials ; in that case the `ml.alternet.security.auth.CryptFormat` parameter will contain a comma-separated
list of concrete classes. Note that `ml.alternet.security.auth.formats.PlainTextCryptFormat` is used for plain text
passwords stored not hashed in the LDAP server and MUST NOT be used in production environments.

<a name="tomcat"></a>

### Jetty full running example

You can checkout this full featured example available here https://github.com/alternet/alternet.ml/tree/master/security-jetty-9.1-demo-jaas-ldap

Run it with `mvn verify` and launch a browser to http://localhost:8080

This example runs a Jetty server and an LDAP server and connect the authentication plumbing with Alternet Security.

### Tomcat

* Require Java 8+ and Tomcat 8.0.17 and above.

The [Tomcat tests](../security-tomcat-8.0/surefire-report.html) show how a password is captured
([source code](https://github.com/alternet/alternet.ml/blob/master/security-tomcat-8.0/src/test/java/ml/alternet/test/security/web/tomcat/RESTTest.java)).

#### Maven import

Don't forget to also import a module that checks the credentials (see above).

```xml
<dependency>
    <groupId>ml.alternet</groupId>
    <artifactId>alternet-security-tomcat-8.0</artifactId>
    <version>1.0</version>
</dependency>
```

#### JavaDoc API documentation

* [Alternet Security for Tomcat](../security-tomcat-8.0/apidocs/index.html)

All Alternet APIs :

* [Alternet Libs](../apidocs/index.html)

#### Project page

* [Project page](../security-tomcat-8.0/index.html)

#### Tomcat programmatic configuration

Configuring Tomcat is very simple :

* create a specific connector [AltProtocolHandler](../security-tomcat-8.0/apidocs/ml/alternet/security/web/tomcat/AltProtocolHandler.html)
* create a specific authenticator [AltBasicAuthenticator](../security-tomcat-8.0/apidocs/ml/alternet/security/web/tomcat/AltBasicAuthenticator.html)
* create a specific credential handler [AltCredentialHandler](../security-tomcat-8.0/apidocs/ml/alternet/security/web/tomcat/AltCredentialHandler.html)

```Java
Tomcat server = new Tomcat();

// use the specific connector
Connector connector = new Connector("ml.alternet.security.web.tomcat.AltProtocolHandler");
connector.setPort(port);
// set in "tomcatProtocol" what you would have set in the Connector constructor
connector.setProperty("tomcatProtocol", "HTTP/1.1");
// you can omit the following line, it is the default setting
connector.setProperty("passwordManager", "ml.alternet.security.impl.StrongPasswordManager");
server.getService().addConnector(connector);
server.setConnector(connector);

// create a webapp
Context wac = server.addContext(contextPath, resourceBase);
Wrapper servlet = Tomcat.addServlet(wac, "YOUR_SERVLET", new YourServlet());
wac.addServletMapping("/*", "YOUR_SERVLET");

// specific security config
AuthenticatorBase auth = new AltBasicAuthenticator();
auth.setSecurePagesWithPragma(false);
auth.setChangeSessionIdOnAuthentication(false);
auth.setAlwaysUseSession(true);
((StandardContext) wac).addValve(auth);

// the following setting should be set in web.xml
ApplicationParameter initParam = new ApplicationParameter();
initParam.setName(Config.AUTH_METHOD_INIT_PARAM);
initParam.setValue("Basic");
wac.addApplicationParameter(initParam);

Realm realm = new ...();
realm.setCredentialHandler(new AltCredentialHandler());
wac.setRealm(realm);

// the remaining configuration is standard tomcat configuration
wac.setPreemptiveAuthentication(true);
wac.setPrivileged(true);
wac.addSecurityRole("customer");
wac.addSecurityRole("admin");
wac.setLoginConfig(new LoginConfig("BASIC", "realm", "/login.html", "/error.html"));
SecurityConstraint security = new SecurityConstraint();
security.addAuthRole("admin");
security.setAuthConstraint(true);
SecurityCollection coll = new SecurityCollection();
coll.addMethod("GET");
coll.addPattern("/*");
security.addCollection(coll);
wac.addConstraint(security);

server.start();
```

Various full working examples of programmatic configurations are available in the [project test pages](https://github.com/alternet/alternet.ml/blob/master/security-tomcat-8.0/src/test/java/ml/alternet/test/security/web/tomcat/).

#### Tomcat XML configuration

Configuring Tomcat can be done simply in `server.xml` ; the Alternet libraries also have to be available to Catalina.
You can either drop the required library in the `lib` directory of Catalina, or preferably as mentioned in the
[Tomcat documentation (Advanced Configuration - Multiple Tomcat Instances)](http://tomcat.apache.org/tomcat-8.0-doc/RUNNING.txt),
in a separate Tomcat instance. Below, we consider that the Tomcat installation is left as-is, and that we are creating a separate instance.

The idea is to set `$CATALINA_HOME` and `$CATALINA_BASE` when launching Tomcat.

```
export CATALINA_HOME=/path/to/server/apache-tomcat-8.0.18
export CATALINA_BASE=/path/to/server/myTomcat
cp -R $CATALINA_HOME/conf $CATALINA_BASE
```

Firstly, generate the required Alternet libraries and copy it to your Tomcat custom instance :

```
cd /path/to/projects/ml.alternet/security-tomcat-8.0
mvn jar:jar shade:shade
```

This will produce the required library in the project folder : `src/server/tomcat.8080/lib/alternet-security-tomcat-bundle.jar`
Copy this file in your folder `$CATALINA_BASE/lib/` ; you can of course copy additional libraries, for example
a custom hasher (see below).

```
cp src/server/tomcat.8080/lib/alternet-security-tomcat-bundle.jar $CATALINA_BASE/lib/
```

Edit the file `$CATALINA_BASE/conf/server.xml` and change it as follow :

```XML
<Server port="8005" shutdown="SHUTDOWN">
    <GlobalNamingResources>
        <Resource auth="Container" description="User database that can be updated and saved"
                factory="org.apache.catalina.users.MemoryUserDatabaseFactory"
                name="UserDatabase" pathname="conf/tomcat-users.xml"
                type="org.apache.catalina.UserDatabase" />
    </GlobalNamingResources>
    <Service name="Catalina">
        <Connector connectionTimeout="20000" port="8080" redirectPort="8443"
            tomcatProtocol="HTTP/1.1"
            protocol="ml.alternet.security.web.tomcat.AltProtocolHandler"
            passwordManager="ml.alternet.security.impl.StrongPasswordManager"/>
            <!-- you can omit the "passwordManager" attribute if the default
                value shown above suits your needs -->
        <Engine defaultHost="localhost" name="Catalina">
            <Realm className="org.apache.catalina.realm.UserDatabaseRealm" resourceName="UserDatabase">
                <CredentialHandler className="ml.alternet.security.web.tomcat.AltCredentialHandler"
                    hasher="ml.alternet.security.auth.impl.PBKDF2Hasher"/>
                    <!-- you can omit the "hasher" attribute if the default
                         value shown above suits your needs -->
                    <!-- TODO : refactoring needed => use crypt formats instead -->

            </Realm>
            <Host appBase="webapps" autoDeploy="true" name="localhost" unpackWARs="true">
                <Context docBase="webapps/ROOT"
                    path="" reloadable="false" >
                    <Valve className="ml.alternet.security.web.tomcat.AltBasicAuthenticator"/>
                    <!-- you can omit the <Valve> element if you don't want
                         Alternet handling your BASIC HTTP Authentication, or
                         use your own -->
                </Context>
            </Host>
        </Engine>
    </Service>
</Server>
```

* configure the Alternet `<Connector>` : the "passwordManager" attributes contains the name of a class that implements [PasswordManager](apidocs/ml/alternet/security/PasswordManager.html) ; the default value is shown in the example above
* set the Alternet `<CredentialHandler>` to your `<Realm>` : the "hasher" attribute contains the name of a class that implements [Hasher](apidocs/ml/alternet/security/auth/Hasher.html) ; the default value is shown in the example above
* configure your `<Context>` with an Alternet `<Valve>` that use a specific authenticator (if you intend to deploy an application that use BASIC HTTP Authentication)

Ensure that the passwords available in your realm are hashed values.
If you use `$CATALINA_BASE/conf/tomcat-users.xml` it will look like this :

```XML
<tomcat-users ...>
    <role ...>
    <user username="tomcat" roles="tomcat"
        password="PBKDF2:1000:hOB9xOwuxpl1vE02QqVP5Cx4dWcnMghJ:ruJNwcPd7nH4ebwSG0GaIdCdUmkzrYX3"/>
    <...>
</tomcat-users>
```

If you use another kind of resource, such as a database, store the user password hash accordingly.
The Alternet credential handler will simply compare the password supply by the user with the hash stored in the resource.

<hr />
TODO : the following is deprecated

To produce such a hash, you can compute a hash with Alternet Security :

```
java -cp $CATALINA_BASE/lib/alternet-security-tomcat-bundle.jar ml.alternet.security.auth.impl.PBKDF2Hasher --encrypt
```

you will be prompted for a password.

<hr />

Ideally, a user account manager application should allow to initialize user accounts transparently.

Once all those settings are done, you can deploy your Web application in `$CATALINA_BASE/webapps/`

Don't forget the required settings in `$CATALINA_BASE/webapps/yourApp/WEB-INF/web.xml` of your Web app as mentioned before.

Launch tomcat :

```
/path/to/server/apache-tomcat-8.0.18/bin/catalina.sh start
```

#### Example

A full working example of Tomcat XML configuration + Webapp is available in the [project demo page](https://github.com/alternet/alternet.ml/blob/master/security-tomcat-8.0/src/server/tomcat.8080/).

It can be launched from the project directory :

```
export CATALINA_BASE=/path/to/projects/ml.alternet/security-tomcat/src/server/tomcat.8080/
/path/to/server/apache-tomcat-8.0.18/bin/catalina.sh start
```
<!--

### Grizzly

TODO

Maven import :

```xml
<dependency>
    <groupId>ml.alternet</groupId>
    <artifactId>alternet-security-grizzly</artifactId>
    <version>1.0</version>
</dependency>
```

-->

# Background

## Preserving the security chain

Many third-party authentication mechanisms (LDAP, relational databases, etc)
are performing authentication thanks to String.

As mentioned before, the purpose of this library is to
ensure that the Web container won't ever create a String object
from a password when the relevant Web container is properly configured.

It is strongly recommended to avoid String creation from passwords for
this stage, for example by passing instead a char array to the authenticator.
([LDAP authenticator](https://docs.oracle.com/javase/tutorial/jndi/ldap/simple.html)
support char arrays passwords)

## The Authentication Framework

It's worth to mention that hash algorithms are kept as close as possible to the original.
Adaptations are made for using them with the Password class, to clean intermediate data when possible,
and to separate the production of the formated crypt from the hashed bytes.
[They are available here](../security-auth/apidocs/ml/alternet/security/algorithms/package-summary.html).

[Here are some considerations about the Authentication Framework](../security-auth/auth-formats.html)
(description of popular crypt formats and design considerations)

<!--

## OWASP

TODO

-->
