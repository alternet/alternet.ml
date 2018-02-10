package ml.alternet.test.security.auth.hashers;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;

import org.apache.commons.codec.digest.UnixCrypt;
import org.assertj.core.api.Assertions;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.hashers.UnixHashers;

public class UnixCryptTest {

    Hasher UNIX_CRYPT = UnixHashers.UNIX_CRYPT.get();

    @DataProvider(name="pwd")
    public String[][] getPasswords() {
        return new String[][] {
            {"secret", "/.4WmlAsvgZlI"},
            {"secret", "xxWAum7tHdIUw"},
            {"", "12UFlHxel6uMM"},
            {"secret", "12FJgqDtVOg7Q"}
        };
    }
    @Test(dataProvider="pwd")
    public void testUnixCryptStrings(String pwd, String crypt) throws InvalidAlgorithmParameterException {
        Assertions.assertThat(
                // ensure we get the same result with Apache UnixCrypt implementation
                UnixCrypt.crypt(pwd.getBytes(StandardCharsets.UTF_8), crypt.substring(0,2))
            ).isEqualTo(crypt);
        Assertions.assertThat(
                UNIX_CRYPT.check(Credentials.fromPassword(
                    pwd.toCharArray()),
                    crypt))
                .isTrue();
    }

    @DataProvider(name="encPwd")
    public Object[][] getEncPasswords() {
        return new Object[][] {
            // UTF-8 stores \u00e4 "a with diaeresis" as two bytes 0xc3 0xa4.
                                           // this is the default
            {"t\u00e4st", "./287bds2PjVw", StandardCharsets.UTF_8},
            // ISO-8859-1 stores "a with diaeresis" as single byte 0xe4.
            {"t\u00e4st", "./bLIFNqo9XKQ", StandardCharsets.ISO_8859_1}
        };
    }
    @Test(dataProvider="encPwd")
    public void testUnixCryptEncodedStrings(String pwd, String crypt, Charset encoding) throws InvalidAlgorithmParameterException {
        // what is nice in Alternet Security, is that hashers are working
        // primarily on chars, not directly on bytes, and you can configure
        // it with a charset, but you are not compelled to preprocess them
        // like with other implementations such as Apache Unix Crypt implementation

        Assertions.assertThat(
                // check with Apache Unix Crypt implementation, works with bytes
                // unlike with Alternet Security, the charset is not part
                // of the configuration of UnixCrypt
                UnixCrypt.crypt(pwd.getBytes(encoding), "./")
            ).isEqualTo(crypt);

        Assertions.assertThat(
                UNIX_CRYPT.getBuilder()
                    .setCharset(encoding)
                    .build()
                    .check(Credentials.fromPassword(
                    pwd.toCharArray()),
                    crypt))
                .isTrue();
    }

//    /**
//     * Single character salts are illegal!
//     * E.g. with glibc 2.13, crypt("secret", "x") = "xxZREZpkHZpkI" but
//     * crypt("secret", "xx") = "xxWAum7tHdIUw" which makes it unverifyable.
//     */
//    @Test(expected = IllegalArgumentException.class)
//    public void testUnixCryptWithHalfSalt() {
//        UnixCrypt.crypt("secret", "x");
//    }

//    @Test
//    public void testUnixCryptWithoutSalt() {
//        final String hash = UnixCrypt.crypt("foo");
//        assertTrue(hash.matches("^[a-zA-Z0-9./]{13}$"));
//        final String hash2 = UnixCrypt.crypt("foo");
//        assertNotSame(hash, hash2);
//    }

}