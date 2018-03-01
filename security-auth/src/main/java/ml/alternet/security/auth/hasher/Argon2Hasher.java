package ml.alternet.security.auth.hasher;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;

import ml.alternet.discover.DiscoveryService;
import ml.alternet.discover.LookupKey;
import ml.alternet.misc.Thrower;
import ml.alternet.security.Password;
import ml.alternet.security.algorithms.Argon2;
import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.Argon2Parts;
import ml.alternet.security.auth.crypt.CryptParts;
import ml.alternet.security.binary.SafeBuffer;

/**
 * The Argon2 hasher.
 *
 * @author Philippe Poulard
 */
public class Argon2Hasher extends HasherBase<Argon2Parts> implements Hasher.Configuration.Extension {

    /**
     * Create an Argon2 hasher.
     *
     * @param config The configuration of this hasher.
     */
    public Argon2Hasher(Builder config) {
        super(config);
    }

    @Override
    public Hasher configureWithCrypt(String crypt) {
        // the hash part is used to set the hash byte size
        Argon2Hasher hr = this;
        CryptParts parts = hr.getConfiguration().getFormatter().parse(crypt, hr);
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
     * You must supply an implementation of {@link Argon2Hasher.SecretResolver} in order to
     * support the "keyid" parameter.
     *
     * @param keyid The key ID.
     *
     * @return The secret bound to the key ID, or <code>null</code> if the key ID was null.
     *
     * @throws IllegalArgumentException When the key is not found.
     *
     * @see Argon2Hasher.SecretResolver
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
    interface SecretResolver {

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

    /**
     * A bridge with any Argon2 implementation.
     *
     * @author Philippe Poulard
     */
    @LookupKey(byDefault = true, implClass = Bridge.class)
    public interface Argon2Bridge {

        /**
         * Argon2 variants.
         *
         * @author Philippe Poulard
         */
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
        Argon2Bridge $ = argon2();

    }

    private static Argon2Bridge argon2() {
        try {
            return DiscoveryService.newInstance(Argon2Bridge.class);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            return Thrower.doThrow(e);
        }
    }

    /**
     * A bridge with the Argon2 algorithm.
     *
     * @author Philippe Poulard
     */
    public static class Bridge implements Argon2Bridge {
        @Override
        public byte[] encrypt(byte[] passwordb, byte[] salt, int memoryCost, int timeCost, int parallelism,
                Type type, int version, int outputLength, byte[] additional, byte[] secret)
        {
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
