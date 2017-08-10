package ml.alternet.security.auth.hashers.impl;

import java.security.InvalidAlgorithmParameterException;

import ml.alternet.misc.Thrower;
import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.formats.CryptFormatter;
import ml.alternet.security.auth.formats.CryptParts;

/**
 * Convenient base class for hasher implementations.
 *
 * Only 2 methods have to be implemented : one for initializing
 * a new fully configured crypt parts, the other for encrypting
 * the credentials with the parts (typically, the salt if any).
 *
 * @author Philippe Poulard
 *
 * @param <T> The kind of crypt parts to support.
 */
public abstract class HasherBase<T extends CryptParts> implements Hasher {

    private Configuration conf;

    /**
     * Create a hasher.
     *
     * @param conf The configuration of this hasher.
     */
    public HasherBase(Configuration conf) {
        this.conf = conf;
    }

    @Override
    public Builder getBuilder() {
        try {
            Builder builder = getConfiguration().getBuilder().newInstance();
            builder.setConfiguration(getConfiguration());
            return builder;
        } catch (InstantiationException | IllegalAccessException e) {
            return Thrower.doThrow(e);
        }
    }

    @Override
    public Configuration getConfiguration() {
        return this.conf;
    }

    @SuppressWarnings("unchecked")
    private CryptFormatter<T> getFormatter() {
        return (CryptFormatter<T>) getConfiguration().getFormatter();
    }

    @Override
    public String encrypt(Credentials credentials) throws InvalidAlgorithmParameterException {
        T parts = initializeParts();
        parts.hash = encrypt(credentials, parts);
        return getFormatter().format(parts);
    }

    @Override
    public boolean check(Credentials credentials, String crypt) throws InvalidAlgorithmParameterException {
        T parts = getFormatter().parse(crypt, this);
        byte[] hash = encrypt(credentials, parts);
        // do not use Arrays.equals(hash, pwdHash);
        return Hasher.compare(hash, parts.hash);
    }

    /**
     * Encrypt some credentials.
     *
     * @param credentials The credentials to encrypt.
     *
     * @param parts Contains additional data (typically the salt)
     *      used for encryption ; the hash field MUST NOT be
     *      updated by this method during encryption : it's up to
     *      the caller to determine if the result has to be compared
     *      to the hash field (checking credentials) OR to set the
     *      result in the hash field (encrypt credentials).
     *
     * @return The result hash.
     */
    public abstract byte[] encrypt(Credentials credentials, T parts);

    /**
     * Initialize the crypt parts (typically generate a salt).
     *
     * @return A new crypt parts with its fields initialized except
     *      its hash field.
     */
    public abstract T initializeParts();

    @Override
    public String toString() {
        return this.conf.toString();
    }

}
