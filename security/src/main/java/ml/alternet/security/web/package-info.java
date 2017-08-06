/**
 * Handle safe passwords on a Web environment, meaning that in the
 * Web processing chain, a password NEVER appear as a String (unsafe)
 * inside the Web container.
 *
 * <p>An additional requirement for the Web container is that once extracted
 * from the incoming data stream, the clear password have to be unset.</p>
 *
 * <p>This package focus on handling the passwords inside a Web application
 * (servlet-based applications as well as RESTful (JAX-RS) applications).
 * It doesn't supply means for the Web container to capture the received
 * passwords.</p>
 *
 * <p>Please refer to Alternet Security subprojects to configure your
 * servlet container to make this feature available.</p>
 *
 * @see ml.alternet.security.web.Passwords
 * @see ml.alternet.security.web.PasswordParam
 */
@ml.alternet.misc.InfoClass
package ml.alternet.security.web;
