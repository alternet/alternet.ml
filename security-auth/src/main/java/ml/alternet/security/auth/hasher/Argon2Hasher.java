package ml.alternet.security.auth.hasher;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;

import ml.alternet.discover.DiscoveryService;
import ml.alternet.discover.LookupKey;
import ml.alternet.encode.BytesEncoding;
import ml.alternet.misc.Thrower;
import ml.alternet.security.Password;
import ml.alternet.security.algorithms.Argon2;
import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.CryptParts;
import ml.alternet.security.auth.crypt.SaltedParts;
import ml.alternet.security.auth.formats.CryptFormatter;
import ml.alternet.security.auth.formats.ModularCryptFormat;
import ml.alternet.security.binary.SafeBuffer;
import ml.alternet.util.StringUtil;

/**
 * The Argon2 hasher.
 *
 * @author Philippe Poulard
 */
public class Argon2Hasher extends HasherBase<Argon2Hasher.Argon2Parts> implements Hasher.Configuration.Extension {

    /**
     * Create an Argon2 hasher.
     *
     * @param config The configuration of this hasher.
     */
    public Argon2Hasher(Configuration config) {
        super(config);
    }

    @Override
    public Hasher configureWithCrypt(String crypt) {
        // the hash part is used to set the hash byte size
        Argon2Hasher hr = this;
        CryptParts parts= hr.getConfiguration().getFormatter().parse(crypt, hr);
        if (parts.hash != null) {
            hr = (Argon2Hasher) hr.getBuilder().setHashByteSize(parts.hash.length)
                .use(null) // this avoid loooooooop
                .build();
        }
        return hr;
    }

    @Override
    public byte[] encrypt(Credentials credentials, Argon2Parts parts) {
        ByteBuffer bb;
        try (Password.Clear pwd = credentials.getPassword().getClearCopy()) {
            bb = SafeBuffer.encode(CharBuffer.wrap(pwd.get()), getConfiguration().getCharset());
        }
        byte[] passwordb = SafeBuffer.getData(bb);
        Configuration conf = parts.hr.getConfiguration();
//        int outputLength = parts.hash == null ? conf.getHashByteSize() : parts.hash.length;
        int outputLength = conf.getHashByteSize();
        byte[] hash = Argon2Bridge.$.encrypt(passwordb, parts.salt, parts.memoryCost, parts.timeCost, parts.parallelism,
                Argon2Bridge.Type.valueOf(conf.getVariant()), parts.version,
                outputLength, parts.data, getSecret(parts.keyid));

        Arrays.fill(passwordb, (byte) 0);

        return hash;
    }

    @Override
    public Argon2Parts initializeParts() {
        Argon2Parts parts = new Argon2Parts(this);
        parts.generateSalt();
        return parts;
    }

    /**
     * Return the secret bound to the given key.
     *
     * You must supply an implementation of {@link SecretResolver} in order to
     * support the "keyid" parameter.
     *
     * @param keyid The key ID.
     *
     * @return The secret bound to the key ID, or <code>null</code> if the key ID was null.
     *
     * @throws IllegalArgumentException When the key is not found.
     *
     * @see SecretResolver
     */
    public byte[] getSecret(byte[] keyid) { // TODO : should try with hash conf to get an implementation ???
        if (keyid == null) {
            return null;
        } else {
            try {
                SecretResolver sr = DiscoveryService.lookupSingleton(SecretResolver.class);
                return sr.getSecret(keyid);
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                return Thrower.doThrow(e);
            }
        }
    };

    /**
     * You must supply an implementation of this interface in order to
     * support the "keyid" parameter.
     *
     * <pre>{@literal @}LookupKey(forClass=SecretResolver.class)
     *public static class MySecretResolver implements SecretResolver {
     *
     *    {@literal @}Override
     *    public byte[] getSecret(byte[] keyid) {
     *        byte[] secret = ... // your code here
     *        return secret;
     *    }
     *}</pre>
     *
     * @author Philippe Poulard
     */
    public static interface SecretResolver {

        /**
         * Return the secret bound to the given key.
         *
         * @param keyid The key ID.
         *
         * @throws IllegalArgumentException When the key is not found.
         *
         * @return The secret.
         */
        byte[] getSecret(byte[] keyid) throws IllegalArgumentException;

    }

    public static class Argon2Parts extends SaltedParts {

        public Argon2Parts(Hasher hr) {
            super(hr);
        }

        public int version = -1;
        public int memoryCost = -1;
        public int timeCost = -1;
        public int parallelism = -1;
        public byte[] keyid;
        public byte[] data;

    }

    /**
     * The Argon2 hash format is defined by the argon2 reference implementation.
     * It’s compatible with the PHC Format and Modular Crypt Format, and uses
     * $argon2i$, $argon2d$ and $argon2id$ as it’s identifying prefixes for all
     * its strings.
     *
     * An example hash (of password) is:
     *
     * <code>$argon2i$v=19$m=512,t=3,p=2$c29tZXNhbHQ$SqlVijFGiPG+935vDSGEsA</code>
     *
     * This string has the format <code>$argon2X$v=V$m=M,t=T,p=P[,data=DATA]$salt$digest</code>, where:
     *
     * <ul>
     * <li>X is either i or d, depending on the argon2 variant (i in the example).</li>
     * <li>V is an integer representing the argon2 revision. the value (when rendered into
     * hexadecimal) matches the argon2 version (in the example, v=19 corresponds to 0x13,
     * or Argon2 v1.3).</li>
     * <li>M is an integer representing the variable memory cost, in kibibytes (512kib in the example).</li>
     * <li>T is an integer representing the variable time cost, in linear iterations. (3 in the example).</li>
     * <li>P is a parallelization parameter, which controls how much of the hash calculation is
     * parallelization (2 in the example).</li>
     * <li>DATA is an optional additional amount of data (omitted in the example)</li>
     * <li>salt - this is the base64-encoded version of the raw salt bytes passed into the Argon2
     * function (c29tZXNhbHQ in the example).</li>
     * <li>digest - this is the base64-encoded version of the raw derived key bytes returned from
     * the Argon2 function. Argon2 supports a variable checksum size, though the hashes
     * will typically be 16 bytes, resulting in a 22 byte digest (SqlVijFGiPG+935vDSGEsA in the
     * example).</li>
     * </ul>
     *
     * All integer values are encoded uses ascii decimal, with no leading zeros.
     * All byte strings are encoded using the standard base64 encoding, but without any trailing padding (“=”) chars.
     *
     * Note
     *
     * The v=version$ segment was added in Argon2 v1.3; older version Argon2 v1.0 hashes may not include this portion.
     * The algorithm used by all of these schemes is deliberately identical and simple: The password is encoded
     * into UTF-8 if not already encoded, and handed off to the Argon2 function. A specified number of bytes
     * (16 byte default) returned result are encoded as the checksum.
     */
    public static final CryptFormatter<Argon2Parts> ARGON2_FORMATTER = new CryptFormatter<Argon2Hasher.Argon2Parts>() {

        @Override
        public Argon2Parts parse(String crypt, Hasher hr) {
            Argon2Parts parts = new Argon2Parts(hr);
            String[] stringParts = crypt.split("\\$");
            if (! stringParts[1].equals(parts.hr.getConfiguration().getVariant())) {
                hr = parts.hr.getBuilder().setVariant(stringParts[1]).build();
                parts.hr = hr;
            }
            int versionPresent = 0;
            if (stringParts[2].startsWith("v=")) {
                versionPresent = 1;
                parts.version = Integer.parseInt(stringParts[2].substring(2));
            }
            BytesEncoding encoding = hr.getConfiguration().getEncoding();
            if (stringParts.length > 2 + versionPresent && ! StringUtil.isVoid(stringParts[2 + versionPresent])) {
                String[] params = stringParts[2 + versionPresent].split(",");
                if (params.length > 0 && params[0].startsWith("m=")) {
                    parts.memoryCost = Integer.parseInt(params[0].substring(2));
                }
                if (params.length > 1 && params[1].startsWith("t=")) {
                    parts.timeCost = Integer.parseInt(params[1].substring(2));
                }
                if (params.length > 2 && params[2].startsWith("p=")) {
                    parts.parallelism = Integer.parseInt(params[2].substring(2));
                }
                if (params.length > 3) {
                    int keyPresent = 0;
                    if (params[3].startsWith("keyid=")) {
                        keyPresent = 1;
                        parts.keyid = encoding.decode(params[3].substring(6));
                    }
                    if (params.length > 3+keyPresent && params[3+keyPresent].startsWith("data=")) {
                        parts.data = encoding.decode(params[3+keyPresent].substring(5));
                    }
                }
            }
            if (stringParts.length > 3 + versionPresent && ! StringUtil.isVoid(stringParts[3 + versionPresent])) {
                parts.salt = encoding.decode(stringParts[3 + versionPresent]);
            }
            if (stringParts.length > 4 + versionPresent && ! StringUtil.isVoid(stringParts[4 + versionPresent])) {
                parts.hash = encoding.decode(stringParts[4 + versionPresent]);
            }
            return parts;
        }

        @Override
        public String format(Argon2Parts parts) {
            StringBuffer crypt = new StringBuffer(60);
            crypt.append("$")
                .append(parts.hr.getConfiguration().getVariant())
                .append("$v=")
                .append(Integer.toString(parts.version))
                .append("$m=")
                .append(Integer.toString(parts.memoryCost))
                .append(",t=")
                .append(Integer.toString(parts.timeCost))
                .append(",p=")
                .append(Integer.toString(parts.parallelism));
            BytesEncoding encoding = parts.hr.getConfiguration().getEncoding();
            if (parts.keyid != null) {
                crypt.append(",keyid=")
                .append(encoding.encode(parts.keyid));
            }
            if (parts.data != null) {
                crypt.append(",data=")
                    .append(encoding.encode(parts.data));
            }
            crypt.append("$");
            crypt.append(encoding.encode(parts.salt));
            if (parts.hash != null && parts.hash.length > 0) {
                crypt.append("$")
                    .append(encoding.encode(parts.hash));
            }
            return crypt.toString();
        }

        @Override
        public CryptFormat getCryptFormat() {
            return new ModularCryptFormat();
        }
    };

    /**
     * A bridge with any Argon2 implementation.
     *
     * @author Philippe Poulard
     */
    @LookupKey(byDefault=true, implClass=Bridge.class)
    public interface Argon2Bridge {

        enum Type {
            // order matters !!!
            argon2d, argon2i, argon2id
        }

        /**
         * Computes an Argon2 hash.
         *
         * @param passwordb the password to encrypt
         * @param salt the salt
         * @param memoryCost the memory cost, -1 for the default
         * @param timeCost the time cost, -1 for the default
         * @param parallelism the parallelism, -1 for the default
         * @param variant "argon2i", "argon2i", "argon2id"
         * @param version v=19 corresponds to 0x13, or Argon2 v1.3 ; can also be 0x10
         * @param outputLength -1 for the default which is 32 bytes
         * @param additional Maybe null
         * @param secret Maybe null
         *
         * @return The hash.
         */
        byte[] encrypt(byte[] passwordb, byte[] salt, int memoryCost, int timeCost, int parallelism,
                Type variant, int version, int outputLength, byte[] additional, byte[] secret);

        /**
         * The singleton implementation.
         */
        static Argon2Bridge $ = argon2();

    }

    private static Argon2Bridge argon2() {
        try {
            return DiscoveryService.newInstance(Argon2Bridge.class);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            return Thrower.doThrow(e);
        }
    }

    public static class Bridge implements Argon2Bridge {
        @Override
        public byte[] encrypt(byte[] passwordb, byte[] salt, int memoryCost, int timeCost, int parallelism,
                Type type, int version, int outputLength, byte[] additional, byte[] secret) {
            Argon2 argon2 = new Argon2();

            if (memoryCost != -1) {
                argon2.setMemoryInKiB(memoryCost);
            }
            if (timeCost != -1) {
                argon2.setIterations(timeCost);
            }
            if (parallelism != -1) {
                argon2.setParallelism(parallelism);
            }
            argon2.setType(type);
            if (version != -1) {
                argon2.setVersion(version);
            }
            if (outputLength != -1) {
                argon2.setOutputLength(outputLength);
            }
            if (additional != null) {
                argon2.setAdditional(additional);
            }
            if (secret != null) {
                argon2.setSecret(secret);
            }
            return argon2.hash(passwordb, salt);
        }
    }

}
