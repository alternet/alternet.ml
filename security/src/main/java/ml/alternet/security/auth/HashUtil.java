package ml.alternet.security.auth;

import java.security.InvalidAlgorithmParameterException;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;

import ml.alternet.discover.DiscoveryService;
import ml.alternet.security.auth.formats.ColonCryptFormat;
import ml.alternet.util.Util;

/**
 * Allow to find a specific hasher, or to check credentials (a password)
 * regarding a crypt.
 *
 * @author Philippe Poulard
 */
@Util
public class HashUtil {

    /**
     * Find a hasher by its crypt format family name and its scheme.
     *
     * @param cryptFormatFamilyName The crypt format family name.
     * @param scheme The scheme of the hasher to lookup.
     *
     * @param properties The properties used for configuring the hasher ;
     *      maybe <code>null</code> if the hasher doesn't need properties,
     *      or if the given hasher is used only for checking passwords
     *      (not for computing crypts).
     *
     * @return The hasher.
     *
     * @throws InvalidAlgorithmParameterException When the hasher of the
     *      variant required can't be found, or when the properties can't
     *      be applied.
     *
     * @see CryptFormat#family()
     * @see DiscoveryService
     */
    public static Optional<Hasher> lookup(String cryptFormatFamilyName, String scheme, Properties properties)
            throws InvalidAlgorithmParameterException
    {
        try {
            Hasher hasher = DiscoveryService.newInstance(Hasher.class.getName()
                    + '/' + cryptFormatFamilyName + '/' + scheme);
            if (properties != null) {
                hasher.configure(properties);
            }
            return Optional.ofNullable(hasher);
        } catch (InvalidAlgorithmParameterException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidAlgorithmParameterException(e);
        }
    }

    /**
     * Find a hasher by its crypt format and its scheme.
     *
     * @param cryptFormat The crypt format.
     * @param scheme The scheme of the hasher to lookup.
     *
     * @param properties The properties used for configuring the hasher ;
     *      maybe <code>null</code> if the hasher doesn't need properties,
     *      or if the given hasher is used only for checking passwords
     *      (not for computing crypts).
     *
     * @return The hasher.
     *
     * @throws InvalidAlgorithmParameterException When the hasher of the
     *      variant required can't be found, or when the properties can't
     *      be applied.
     *
     * @see CryptFormat#family()
     * @see DiscoveryService
     */
    public static Optional<Hasher> lookup(CryptFormat cryptFormat, String scheme, Properties properties)
            throws InvalidAlgorithmParameterException
    {
        return lookup(cryptFormat.family(), scheme, properties);
    }

    /**
     * Find a hasher among the given crypt formats.
     *
     * @param crypt The actual crypt.
     * @param properties The properties used for configuring the hasher ;
     *      maybe <code>null</code> if the hasher doesn't need properties,
     *      or if the given hasher is used only for checking passwords
     *      (not for computing crypts).
     * @param cryptFormats The list of crypt formats.
     *
     * @return The hasher.
     *
     * @throws InvalidAlgorithmParameterException When a property is
     *      not supported by this hasher.
     */
    public static Optional<Hasher> lookup(String crypt, Properties properties, CryptFormat... cryptFormats)
            throws InvalidAlgorithmParameterException
    {
        Hasher hasher = Arrays.stream(cryptFormats)
                .map(c -> c.resolve(crypt))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .get();
        if (hasher != null) {
            hasher.configure(properties);
        }
        return Optional.ofNullable(hasher);
    }

    /**
     * Check a password with a crypt ; the hasher is looked up among a list
     * of crypt formats regarding the format of the crypt.
     *
     * @param credentials Credentials, that must contain at least the password.
     * @param crypt The crypt, that has the format of one of the crypt formats
     * @param cryptFormats The candidate crypt formats
     * @return <code>true</code> if the password matches the crypt,
     *          <code>false</code> otherwise.
     *
     * @throws InvalidAlgorithmParameterException When the hash algorithm fails or when
     *      the crypt doesn't have the expected format.
     */
    public static boolean check(Credentials credentials, String crypt, CryptFormat... cryptFormats)
            throws InvalidAlgorithmParameterException
    {
        try {
            Hasher hasher = lookup(crypt, null, cryptFormats)
                .orElseThrow(() -> new InvalidAlgorithmParameterException("Hasher not found"));
            return hasher.check(credentials, crypt);
        } catch (InvalidAlgorithmParameterException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidAlgorithmParameterException(e);
        }
    }

    /**
     * Check a password with a crypt.
     *
     * The hasher corresponding to the scheme of the crypt
     * is first looked up.
     *
     * @see {@link #lookup(String, Properties, CryptFormat...)
     *
     * @param credentials Credentials, that must contain at least the password.
     * @param crypt The crypt, that has the format "<code>[scheme]:[schemeSpecificPart]</code>"
     * @return <code>true</code> if the password matches the crypt,
     *          <code>false</code> otherwise.
     *
     * @throws InvalidAlgorithmParameterException When the hash algorithm fails or when
     *      the crypt doesn't have the expected format.
     *
     * @see #DEFAULT
     */
    public static boolean check(Credentials credentials, String crypt) throws InvalidAlgorithmParameterException {
        try {
            Hasher hasher = lookup(crypt, null, DEFAULT)
                .orElseThrow(() -> new InvalidAlgorithmParameterException("Hasher not found"));
            return hasher.check(credentials, crypt);
        } catch (InvalidAlgorithmParameterException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidAlgorithmParameterException(e);
        }
    }

    /**
     * The default crypt format.
     *
     * @see ColonCryptFormat#SINGLETON
     */
    public static CryptFormat DEFAULT = ColonCryptFormat.SINGLETON;

}
