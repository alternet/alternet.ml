/**
 * Handle safe passwords inside the Tomcat Web server, meaning that in the
 * Web processing chain, a password NEVER appear as a String (unsafe)
 * inside the Tomcat server.
 *
 * <p>This package focus on handling the passwords inside the Tomcat Web
 * server by capturing the data received from the incoming HTTP requests,
 * and replacing them with '*' characters before Strings are created.</p>
 *
 * <p>Once a password is captured, it is available to the Web application
 * in its obfuscated form.</p>
 *
 * @see ml.alternet.security.web.Passwords
 * @see ml.alternet.security.web.PasswordParam
 */
@ml.alternet.misc.InfoClass
package ml.alternet.security.web.tomcat;
