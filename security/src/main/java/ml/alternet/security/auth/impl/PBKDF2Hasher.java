package ml.alternet.security.auth.impl;

import java.io.PrintStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.SecureRandom;

import javax.crypto.spec.PBEKeySpec;
import javax.crypto.SecretKeyFactory;
import javax.xml.bind.DatatypeConverter;

import ml.alternet.discover.LookupKey;
import ml.alternet.security.Password;
import ml.alternet.security.auth.Hasher;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * The strong password hasher PBKDF2 (with salt).
 *
 * <p>The format of a crypt is "<code>PBKDF2:[iterations]:[salt]:[hash]</code>"</p>
 *
 * <p>Typically, a crypt computed by this class can be stored
 * safely inside a database, and retrieved for checking whether
 * a password matches it.</p>
 *
 * <p>The CLI allows to compute (and check) a crypt.</p>
 */
@LookupKey(forClass = Hasher.class, variant = "ColonCryptFormat/PBKDF2")
@LookupKey(forClass = Hasher.class, variant = "PBKDF2")
public class PBKDF2Hasher implements Hasher {

    /**
     * The parameter name "saltByteSize" ; the value must be an Integer
     */
    public static final String SALT_BYTE_SIZE_PROPERTY_NAME = "saltByteSize";

    /**
     * The parameter name "hashByteSize" ; the value must be an Integer
     */
    public static final String HASH_BYTE_SIZE_PROPERTY_NAME = "hashByteSize";

    /**
     * The parameter name "iterations" ; the value must be an Integer
     */
    public static final String ITERATIONS_PROPERTY_NAME = "iterations";

    /**
     * The parameter name "encoding" ; the value must be "base64" or "hexa",
     * or "" to unset ; default is base64 for encoding, and automatic
     * detection for decoding.
     */
    public static final String ENCODING_PROPERTY_NAME = "encoding";

    private int saltByteSize = 24;
    private int hashByteSize = 24;
    private int iterations = 1000;
    private Boolean encodeInHexa;

    /**
     * Default hasher with 24 bytes for the salt and the hash,
     * and 1000 iterations of the "PBKDF2WithHmacSHA1" algorithm.
     */
    public PBKDF2Hasher() { }

    /**
     * Create a PBKDF2 hasher.
     *
     * @param saltByteSize The number of bytes for the salt.
     * @param hashByteSize The number of bytes for the hash.
     * @param iterations The number of iterations of the PBKDF2 algorithm.
     * @param encodeInHexa For encoding bytes in hexadecimal rather that in Base64.
     */
    public PBKDF2Hasher(int saltByteSize, int hashByteSize, int iterations, boolean encodeInHexa) {
        this.saltByteSize = saltByteSize;
        this.hashByteSize = hashByteSize;
        this.iterations = iterations;
        this.encodeInHexa = encodeInHexa;
    }

    /**
     * By default, return "PBKDF2".
     *
     * @return This hasher's scheme.
     */
    @Override
    public String getScheme() {
        return "PBKDF2";
    }

    /**
     * By default, return "PBKDF2WithHmacSHA1".
     *
     * @return The algorithm used by this implementation.
     */
    protected String getAlgorithm() {
        return "PBKDF2WithHmacSHA1";
    }

    protected int getSaltByteSize() {
        return saltByteSize;
    }

    protected void setSaltByteSize(int saltByteSize) {
        this.saltByteSize = saltByteSize;
    }

    protected int getHashByteSize() {
        return hashByteSize;
    }

    protected void setHashByteSize(int hashByteSize) {
        this.hashByteSize = hashByteSize;
    }

    protected int getIterations() {
        return iterations;
    }

    protected void setIterations(int iterations) {
        this.iterations = iterations;
    }

    protected Boolean getEncodeInHexa() {
        return encodeInHexa;
    }

    protected void setEncodeInHexa(Boolean encodeInHexa) {
        this.encodeInHexa = encodeInHexa;
    }

    @Override
    public void configure(Properties properties) throws InvalidAlgorithmParameterException {
        int p = 0;
        try {
            Integer sbs = (Integer) properties.get(SALT_BYTE_SIZE_PROPERTY_NAME);
            if (sbs != null) {
                p++;
                setSaltByteSize(sbs.intValue());
            }
        } catch (ClassCastException e) {
            throw new InvalidAlgorithmParameterException(SALT_BYTE_SIZE_PROPERTY_NAME
                    + " must be an integer", e);
        }
        try {
            Integer hbs = (Integer) properties.get(HASH_BYTE_SIZE_PROPERTY_NAME);
            if (hbs != null) {
                p++;
                setHashByteSize(hbs.intValue());
            }
        } catch (ClassCastException e) {
            throw new InvalidAlgorithmParameterException(HASH_BYTE_SIZE_PROPERTY_NAME
                    + " must be an integer", e);
        }
        try {
            Integer i = (Integer) properties.get(ITERATIONS_PROPERTY_NAME);
            if (i != null) {
                p++;
                if (i <=0 ) {
                    throw new InvalidAlgorithmParameterException(ITERATIONS_PROPERTY_NAME
                            + " must be greater than 0.");
                }
                setIterations(i.intValue());
            }
        } catch (ClassCastException e) {
            throw new InvalidAlgorithmParameterException(ITERATIONS_PROPERTY_NAME
                    + " must be an integer", e);
        }
        String encoding = (String) properties.get(ENCODING_PROPERTY_NAME);
        if (encoding != null) {
            p++;
            if ("hexa".equals(encoding)) {
                setEncodeInHexa(true);
            } else if ("base64".equals(encoding)) {
                setEncodeInHexa(false);
            } else if ("".equals(encoding)) {
                setEncodeInHexa(null);
            } else {
                throw new InvalidAlgorithmParameterException("Unsupported encoding " + encoding);
            }
        }
        if (properties.size() > p) {
            String propName = properties.stringPropertyNames().stream()
                .filter(s -> ! Arrays.asList(SALT_BYTE_SIZE_PROPERTY_NAME,
                                             HASH_BYTE_SIZE_PROPERTY_NAME,
                                             ITERATIONS_PROPERTY_NAME,
                                             ENCODING_PROPERTY_NAME).contains(s))
                .collect(Collectors.joining(", "));
            throw new InvalidAlgorithmParameterException("Some properties are not supported by this hasher : "
                    + propName);
        }
    }

    @Override
    public Properties getConfiguration() {
        Properties prop = new Properties();
        prop.put(SALT_BYTE_SIZE_PROPERTY_NAME, getSaltByteSize());
        prop.put(HASH_BYTE_SIZE_PROPERTY_NAME, getHashByteSize());
        prop.put(ITERATIONS_PROPERTY_NAME, getIterations());
        prop.put(ENCODING_PROPERTY_NAME, getEncodeInHexa()==null || ! getEncodeInHexa().booleanValue() ? "base64" : "hexa");
        return prop;
    }

    @Override
    public String encrypt(Password password) throws InvalidAlgorithmParameterException {
        try (Password.Clear pwd = password.getClearCopy()) {
            return encrypt(pwd.get());
        }
    }

    @Override
    public String encrypt(char[] password) throws InvalidAlgorithmParameterException {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[getSaltByteSize()];
        byte[] hash = null;
        String encoded = null;
        while (hash == null) {
            random.nextBytes(salt);
            hash = hash(password, salt, getIterations(), getHashByteSize());
            encoded = encode(hash);
            hash = decode(encoded, false);
        } // loop if decode() is ambiguous
        return getScheme() + ':' + getIterations() + ':' + encode(salt) + ':'
                + encoded;
    }

    @Override
    public boolean check(String user, Password password, String crypt) throws InvalidAlgorithmParameterException {
        try (Password.Clear pwd = password.getClearCopy()) {
            return check(user, pwd.get(), crypt);
        }
    }

    @Override
    public boolean check(String user, char[] password, String crypt) throws InvalidAlgorithmParameterException {
        String[] fields = crypt.split(":");
        // crypt = PBKDF2:[iterations]:[salt]:[hash]
        if (! getScheme().equals(fields[0])) {
            throw new InvalidAlgorithmParameterException(getClass().getName()
                    + " can't check the hash scheme " + fields[0]);
        }
        int i = Integer.parseInt(fields[1]);
        byte[] salt = decode(fields[2], true);
        byte[] hash = decode(fields[3], true);
        byte[] pwdHash = hash(password, salt, i, hash.length);
        return Arrays.equals(hash, pwdHash);
    }

    /**
     * Encode bytes to Base64 or Hexa,
     * according to the configuration.
     *
     * @param data The data to encode
     * @return The data encoded
     */
    protected String encode(byte[] data) {
        if (getEncodeInHexa() != null && getEncodeInHexa().booleanValue()) {
            return DatatypeConverter.printHexBinary(data);
        } else {
            return DatatypeConverter.printBase64Binary(data);
        }
    }

    /**
     * Decode a Base64 or Hexa string to bytes
     * according to the configuration.
     *
     * @param data The data to decode
     * @param allowAmbiguous If the "encoding" property
     * is not specified in the configuration, the suitable
     * decoder will be applied ; if both the Hexa and Base64
     * decoder are eligible, the data will be decoded to
     * Hexa if allowAmbiguous is <tt>true</tt>, and null
     * will be returned if it is <tt>false</tt> (this case
     * is used internally when forging a hash to ensure that
     * it will never by ambiguous).
     * @return The data decoded
     */
    protected byte[] decode(String data, boolean allowAmbiguous) {
        if (getEncodeInHexa() == null) {
            byte[] fromHexa = null;
            try {
                fromHexa = DatatypeConverter.parseHexBinary(data);
            } catch (IllegalArgumentException e) { }
            if (fromHexa != null && allowAmbiguous) {
                return fromHexa;
            }
            byte[] fromB64 = null;
            try {
                fromB64 = DatatypeConverter.parseBase64Binary(data);
            } catch (IllegalArgumentException e) { }
            if (fromHexa == null) {
                return fromB64;
            }
            if (fromB64 == null) {
                return fromHexa;
            }
            // ambiguous ?
            return null;
        } else if (getEncodeInHexa().booleanValue()) {
            return DatatypeConverter.parseHexBinary(data);
        } else {
            return DatatypeConverter.parseBase64Binary(data);
        }
    }

    /**
     * Hash a password.
     *
     * @param password The password.
     * @param salt The salt.
     * @param i The number of iterations of the PBKDF2 algorithm.
     * @param bytes The number of bytes of the hash to produce.
     *
     * @return The hash.
     *
     * @throws InvalidAlgorithmParameterException When the hash algorithm fails.
     */
    protected byte[] hash(char[] password, byte[] salt, int i, int bytes) throws InvalidAlgorithmParameterException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, i, bytes * 8);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance(getAlgorithm());
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new InvalidAlgorithmParameterException(e);
        }
    }

    /**
     * A command line tool to generate or check a password crypt.
     *
     * By default, the password is read from the standard input
     * in order to avoid having it stored in the shell history.
     *
     * Usage :
     * <pre>$ java ml.alternet.security.auth.impl.PBKDF2Hasher [args]</pre>
     *
     * @param args
     *      "<code>--help" : display help.<br/>
     *      "<code>--check [crypt]</code>" : check the password
     *          read from the standard input with the given crypt ;
     *          the method exit with the return code 0 if the password
     *          matches, 1 otherwise.<br/>
     *      "<code>--encrypt</code>" : encrypt the password read
     *          from the standard input ; the canonical hash is printed
     *          to the standard output.<br/>
     *      "<code>--saltByteSize [sbs]</code>" (with the "<code>--encrypt</code>" option) :
     *          an int value to indicates the salt byte size<br/>
     *      "<code>--hashByteSize [hbs]</code>" (with the "<code>--encrypt</code>" option) :
     *           an int value to indicates the hash byte size<br/>
     *      "<code>--iterations [it]</code>" (with the "<code>--encrypt</code>" option) :
     *          an int value to indicates the number of iterations<br/>
     *      "<code>--encoding [encode]</code>" (with the "<code>--encrypt</code>" option) :
     *          \"base64\" or \"hexa\" to indicate how to encode fields ;
     *          \"\" to unset (default is base64 for encoding, and automatic
     *          detection for decoding.)<br/>
     *      "<code>--unsafe [pwd]</code>" : the password, instead of reading
     *          it from the standard input (note that using this option
     *          may lead to security issues ; this option exist conveniently
     *          for testing purpose.
     */
    public static void main(String[] args) {
        int status = 0;
        try {
            Boolean check = null;
            String crypt = null;
            char[] pwd = null;
            Properties prop = new Properties();
            for (int i = 0 ; i < args.length ; i++) {
                String arg = args[i];
                if (arg.startsWith("--")) {
                    String param = arg.substring(2);
                    if ("help".equals(param)) {
                        displayHelp();
                        return;
                    } else if ("check".equals(param)) {
                        check = true;
                        crypt = args[++i];
                    } else if ("encrypt".equals(param)) {
                        check = false;
                    } else if (Arrays.asList(SALT_BYTE_SIZE_PROPERTY_NAME,
                                             HASH_BYTE_SIZE_PROPERTY_NAME,
                                             ITERATIONS_PROPERTY_NAME).contains(param)) {
                        prop.put(param, Integer.parseInt(args[++i]));
                    } else if (ENCODING_PROPERTY_NAME.equals(param)) {
                        prop.put(param, args[++i]);
                    } else if ("unsafe".equals(param)) {
                        pwd = args[++i].toCharArray();
                    } else {
                        displayHelp();
                        return;
                    }
                }
            }
            if (check == null || (check && crypt == null)) {
                displayHelp();
                return;
            }
            if (pwd == null) {
                pwd = System.console().readPassword("[%s]", "Please enter a password : ");
                System.console().writer().println();
            }
            Hasher h = new PBKDF2Hasher();
            if (check) {
                if (h.check(null, pwd, crypt)) {
                    System.out.println("Valid password");
                } else {
                    System.out.println("Invalid password");
                    status = 1;
                }
            } else {
                h.configure(prop);
                System.out.println("Password crypt computed :");
                System.out.println(h.encrypt(pwd));
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            status = 1;
            displayHelp();
        } finally {
            System.exit(status);
        }
    }

    private static void displayHelp() {
        PrintStream w = System.out;
        w.println("Alternet PBKDF2 Hasher");
        w.println();
        w.println("Compute or verify a password crypt with the PBKDF2 algorithm");
        w.println("(with salt).");
        w.println("The format of a crypt is \"PBKDF2:[iterations]:[salt]:[hash]\"");
        w.println();
        w.println("Usage :");
        w.println();
        w.println("$ java ml.alternet.security.auth.impl.PBKDF2Hasher [options]");
        w.println();
        w.println("--help : display help");
        w.println();
        w.println("--check [crypt] : check the password read from the standard");
        w.println("                  input with the given crypt.");
        w.println("                  The method exit with the return code 0 if");
        w.println("                  the password matches, 1 otherwise.");
        w.println();
        w.println("--encrypt : encrypt the password read from the standard input ;");
        w.println("                  The crypt is printed to the standard output.");
        w.println();
        w.println("--saltByteSize [sbs] (with the --encrypt option) :");
        w.println("                  an int value to indicates the salt byte size");
        w.println();
        w.println("--hashByteSize [hbs] (with the --encrypt option) :");
        w.println("                  an int value to indicates the hash byte size");
        w.println();
        w.println("--iterations [it] (with the --encrypt option) :");
        w.println("                  an int value to indicates the number of");
        w.println("                  iterations");
        w.println();
        w.println("--encoding [encode] (with the --encrypt option) :");
        w.println("                  \"base64\" or \"hexa\" to indicate how to encode");
        w.println("                  fields ; \"\" to unset (default is base64 for");
        w.println("                  encoding, and automatic detection for decoding.)");
        w.println();
        w.println("--unsafe [pwd] : the password, instead of reading it from the");
        w.println("                  standard input (note that using this option");
        w.println("                  may lead to security issues ; this option exist");
        w.println("                  conveniently for testing purpose.");
    }

}
