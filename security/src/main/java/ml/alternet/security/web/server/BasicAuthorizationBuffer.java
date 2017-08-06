package ml.alternet.security.web.server;

import java.util.Base64;

import ml.alternet.security.Password;
import ml.alternet.security.PasswordManager;
import ml.alternet.security.auth.Credentials;

/**
 * Extract a password of an HTTP Basic Authorization header,
 * and replace it with '*'.
 *
 * <p>When creating, one must specify the boundaries of the
 * portion of the buffer to analyze, and what this portion
 * contains (all the HTTP headers, only the Authorization header,
 * or only its value).</p>
 *
 * @author Philippe Poulard
 */
public abstract class BasicAuthorizationBuffer {

    /**
     * The scope of the lookup.
     *
     * @author Philippe Poulard
     */
    public enum Scope {
        /** The buffer contains all the HTTP headers. */
        Headers,
        /** The buffer contains the line "Authorization: Basic [base64Credential]" */
        AuthorizationHeader,
        /** The buffer contains the value of the authorization header "Basic [base64Credential]" */
        AuthorizationHeaderValue
    }

    int position;
    int limit;
    Scope scope;

    /**
     * Create a buffer.
     *
     * @param scope Indicates what is delimited in the buffer :
     *      all the headers, just the Authorization header,
     *      or simply its value.
     * @param position The start offset of the bytes.
     * @param limit The end offset of the bytes.
     */
    public BasicAuthorizationBuffer(Scope scope, int position, int limit) {
        this.position = position;
        this.limit = limit;
        this.scope = scope;
    }

    /**
     * Get the byte at the index specified.
     *
     * @param i The actual index.
     * @return The byte.
     */
    public abstract byte get(int i);

    /**
     * Set a byte at the index specified.
     *
     * @param i The actual index.
     * @param b The byte to set.
     */
    public abstract void set(int i, byte b);

    /**
     * Log informations for debugging.
     *
     * @param msg The debug message.
     */
    public abstract void debug(String msg);

    // states of the parser
    public static final int READ_AUTH = 0;
    public static final int READ_UNTIL_CRLF = 1;
    public static final int READ_OWS_BASIC = 2;
    public static final int READ_BASIC = 3;
    public static final int READ_CRED = 4;
    public static final int READ_LF = 5;
    public static final int READ_LAST_LF = 6;

    private static final String AUTHORIZATION = "authorization:";
    private static final String BASIC = "basic ";

    int auth = 0;
    int basicPos = 0;
    int state = READ_AUTH;
    int credStart = -1;
    int credEnd = -1;

    /**
     * Find the boundaries of the base64 credentials in the buffer.
     *
     * <p>String to find in the buffer : "Authorization: Basic [base64Credential]"
     * (or just "Basic [base64Credential]" according to the scope)</p>
     * <pre>    header-field = field-name ":" OWS field-value OWS</pre>
     * <p>OWS means "optional whitespace" and header fields are delimited using CRLF.</p>
     *
     * @return <code>true</code> if the credentials have been found,
     *         <code>false</code> otherwise.
     */
    public boolean findCredentialsBoundaries() {
        if (scope == Scope.AuthorizationHeaderValue) {
            state = READ_BASIC; // start here
        }
        int pos;
        loop: for (pos = position ; pos < limit ; pos++) {
            char c = (char) ( get(pos) & 0x7f );
            c = Character.toLowerCase(c);
            switch (state) {
            case READ_AUTH:
                if (c == AUTHORIZATION.charAt(auth)) {
                    auth++;
                    if (auth == AUTHORIZATION.length()) {
                        state = READ_OWS_BASIC;
                    }
                    break;
                } else if (scope == Scope.AuthorizationHeaderValue || scope == Scope.AuthorizationHeader) {
                    return false;
                } else if (c == '\r') {
                    auth = 0;
                    state = READ_LAST_LF;
                    break;
                } else {
                    auth = 0;
                    state = READ_UNTIL_CRLF;
                    break;
                }
            case READ_UNTIL_CRLF:
                if (c == '\r') {
                    state = READ_LF;
                }
                break;
            case READ_OWS_BASIC:
                if (c == ' ') {
                    break;
                } else {
                    state = READ_BASIC;
                    // do NOT break
                }
            case READ_BASIC:
                if (c == BASIC.charAt(basicPos)) {
                    basicPos++;
                    if (basicPos == BASIC.length()) {
                        state = READ_CRED;
                        credStart = pos + 1;
                    }
                } else if (scope == Scope.AuthorizationHeaderValue || scope == Scope.AuthorizationHeader) {
                    return false;
                } else {
                    basicPos = 0;
                    state = READ_UNTIL_CRLF;
                }
                break;
            case READ_CRED:
                // TODO : handle EOF ?
                if (c == ' ') {
                    credEnd = pos;
                    break loop;
                } else if (c == '\r') {
                    credEnd = pos;
                    state = READ_LF;
                }
                break;
            case READ_LF:
                if (c != '\n') {
                    // error will be raised normally
                    credEnd = -1;
                    break loop;
                }
                if (credEnd > 0) {
                    break loop;
                }
                state = READ_AUTH;
                break;
            case READ_LAST_LF:
                if (c != '\n') {
                    // error will be raised normally
                    credEnd = -1;
                }
                // end of headers
                break loop;
            default:
            }
        }
        if (scope == Scope.AuthorizationHeaderValue || scope == Scope.AuthorizationHeader) {
            // we weren't able to handle end of line
            credEnd = pos; // the last pos known is the end
            return true;
        }
        return credEnd != -1;
    }

    /**
     * Replace the raw password in the buffer with '*'
     * and reencode the credentials in Base64.
     *
     * <p>This method have to be called if {@link #findCredentialsBoundaries()}
     * has returned <code>true</code>.</p>
     *
     * @param passwordManager Allow to create a secure password.
     *
     * @return The credentials with the captured password and the user name.
     */
    public Credentials replace(PasswordManager passwordManager) {
        Credentials credentials = new Credentials();
        byte[] encodedCred = new byte[credEnd - credStart];
        for (int i = 0 ; i < encodedCred.length ; i++) {
            encodedCred[i] = get(credStart + i);
        }
        byte[] decodedCred = Base64.getDecoder().decode(encodedCred);
        // "encodedCred" will be cleaned later

        char[] password = null;
        int pwdStart = -1;
        for (int i = 0 ; i < decodedCred.length ; i++) {
            if (pwdStart == -1) {
                if (decodedCred[i] == ':') {
                    credentials.withUser(new String(decodedCred, 0, i));
                    pwdStart = i + 1;
                    password = new char[decodedCred.length - pwdStart];
                }
            } else {
                password[i - pwdStart] = (char) decodedCred[i];
                decodedCred[i] = '*';
                // this is the pwd value that will be available later in HttpField
            }
        }
        byte[] reEncodedCred = Base64.getEncoder().encode(decodedCred);
        // not sure whether "reEncodedCred" has the same length of "encodedCred"
        for (int i = 0 ; i < encodedCred.length ; i++) {
            encodedCred[i] = ' '; // clean intermediate data
            byte b = ' '; // fill with blanks if "reEncodedCred" was too short
            if (i < reEncodedCred.length) {
                b = reEncodedCred[i];
            }
            set(credStart + i, b);
        }
        debug("HTTP Headers successfully parsed");
        // now, we have it, and the buffer is filled with "*****" instead
        Password pwd = passwordManager.newPassword(password);
        credentials.withPassword(pwd);
        return credentials;
    }

}
