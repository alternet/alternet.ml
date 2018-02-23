package ml.alternet.test.security.auth.hashers;

import java.security.InvalidAlgorithmParameterException;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import ml.alternet.misc.Thrower;
import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.CryptFormatter;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.CryptParts;
import ml.alternet.security.auth.formats.ModularCryptFormat;
import ml.alternet.security.auth.hasher.HasherBase;

/**
 * Crypt test harness
 *
 * @author Philippe Poulard
 */
public abstract class CryptTestBase<H extends HasherBase<C>,C extends CryptParts> {

    /**
     * Return an array of [ "PASSWORD", "SALT", "HASH" ]
     *
     * @return
     */
    protected abstract String[][] getData();

    /**
     * Return an array of [ "PASSWORD", "SALT", "HASH" ]
     *
     * @return
     */
    @DataProvider(name="creds")
    protected String[][] getCreds() {
        return getData();
    }

    /**
     * Return an array of [ "PASSWORD" ]
     *
     * @return
     */
    @DataProvider(name="pwd")
    protected String[][] getPwd() {
        String[][] str = Stream.of(getData())
                .map(s -> s[0]) // the pwd
                .distinct()
                .filter(s -> s.length() > 0)
                .map(s -> new String[] {s}) // restructure
                .toArray(String[][]::new);
        return str;
    }

    /**
     * A filter on [ "PASSWORD", "SALT", "HASH" ]
     * that return eligible "wrong creds".
     *
     * @return
     */
    protected abstract Predicate<String[]> acceptWrongCred();

    /**
     * Return an array of [ "PASSWORD", "HASH" ]
     *
     * @return
     */
    @DataProvider(name="wrongCreds")
    protected String[][] getWrongCreds() {
        String[][] cred = Stream.of(getPwd())
            .map(s -> {
                try {
                    String crypt = Stream.of(getData())
                        .filter(d -> ! s[0].equals(d[0]))
                        .filter(acceptWrongCred())
                        .map(d -> d[2])
                        .findFirst()
                        .orElse(
                            newHasher().encrypt("Wrong p@sssw0r|)".toCharArray())
                        );
                    return new String[] {s[0], crypt};
                } catch (Exception e) {
                    return Thrower.doThrow(e);
                }
            }).toArray(String[][]::new);
        return cred;
    }

    /**
     * Return an array of [ "PASSWORD", "HASH" ]
     *
     * @return
     */
    @DataProvider(name="goodCreds")
    protected String[][] getGoodCreds() {
        String[][] cred = Stream.of(getData())
            .map(s -> new String[] {s[0], s[2]})
            .toArray(String[][]::new);
        return cred;
    }

    protected Hasher resolve(String crypt) {
        return new ModularCryptFormat().resolve(crypt).get();
    }

    protected Hasher hr = newHasher();

    protected abstract H newHasher();

    protected abstract H newHasher(String salt);

    protected String encrypt(Credentials cred, String salt) {
        H hr = newHasher(salt);
        @SuppressWarnings("unchecked")
        C parts = (C) hr.getConfiguration().getFormatter()
            .parse(salt, hr);
        parts.hash = hr.encrypt(cred, parts);
        @SuppressWarnings("unchecked")
        CryptFormatter<C> f = (CryptFormatter<C>) hr.getConfiguration().getFormatter();
        String hashed = f.format(parts);
        return hashed;
    }

    /**
     * Indicates whether an alternate algorithm for hashing a password is available.
     *
     * @return true by default
     */
    protected boolean altAlgo() {
        return true;
    }

    /**
     * An alternate algorithm for hashing a password.
     *
     * @param plain The pwd
     * @param salt The salt
     * @return The crypt
     */
    protected abstract String altCrypt(String plain, String salt);

    /**
     * An alternate algorithm for checking a password.
     *
     * @param plain The pwd
     * @param expected The crypt
     * @return
     */
    protected abstract boolean altCheck(String plain, String expected);

    /**
     * @throws InvalidAlgorithmParameterException
     */
    @Test(dataProvider="creds")
    public void crypts_should_match(String plain, String salt, String expected) throws InvalidAlgorithmParameterException {
        Credentials cred = Credentials.fromPassword(plain.toCharArray());
        String hashed = encrypt(cred, salt);
        Assertions.assertThat(hashed).isEqualTo(expected);

        if (altAlgo()) {
            // check that crypt behaviour is the same
            String hashedChecked = altCrypt(plain, salt);
            Assertions.assertThat(hashedChecked).isEqualTo(expected);
        }
    }

    /**
     * Test method for 'BCrypt.gensalt()'
     * @throws InvalidAlgorithmParameterException
     */
    @Test(dataProvider="pwd")
    public void generatedCrypt_should_match(String plain) throws InvalidAlgorithmParameterException {
        Credentials cred = Credentials.fromPassword(plain.toCharArray());
        String hashed1 = hr.encrypt(cred);
        String hashed2 = encrypt(cred, hashed1);
        Assertions.assertThat(hashed1).isEqualTo(hashed2);
    }

    /**
     * Test method for 'BCrypt.checkpw(String, String)'
     * expecting success
     * @throws InvalidAlgorithmParameterException
     */
    @Test(dataProvider="goodCreds")
    public void checkPassword_should_success(String plain, String expected) throws InvalidAlgorithmParameterException {
        Credentials cred = Credentials.fromPassword(plain.toCharArray());
        Hasher hr = resolve(expected);
        Assertions.assertThat(hr.check(cred, expected)).isTrue();

        if (altAlgo()) {
            // check that crypt behaviour is the same
            Assertions.assertThat(altCheck(plain, expected)).isTrue();
        }
    }

    /**
     * Test method for 'BCrypt.checkpw(String, String)'
     * expecting failure
     * @throws InvalidAlgorithmParameterException
     */
    @Test(dataProvider="wrongCreds")
    public void checkPassword_should_fail(String plain, String expected) throws InvalidAlgorithmParameterException {
        Credentials cred = Credentials.fromPassword(plain.toCharArray());
        Hasher hr = resolve(expected);
        Assertions.assertThat(hr.check(cred, expected)).isFalse();

        if (altAlgo()) {
            // check that BCrypt behaviour is the same
            Assertions.assertThat(altCheck(plain, expected)).isFalse();
        }
    }

}
