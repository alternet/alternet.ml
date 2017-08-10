package ml.alternet.test.security.auth.hashers;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.formats.ModularCryptFormat;
import ml.alternet.security.auth.hashers.ModularCryptFormatHashers;
import ml.alternet.security.auth.hashers.impl.MD5BasedHasher;

public class MD5CryptTest {

//    ModularCryptFormat mcf = new ModularCryptFormat();
//    Credentials pwd = Credentials.fromPassword("secret".toCharArray());
//
//    @Test
//    public void testApr1CryptStrings2() throws InvalidAlgorithmParameterException {
//        // A random example using htpasswd
//        String crypt = "$apr1$TqI9WECO$LHZB2DqRlk9nObiB6vJG9.";
//        Assertions.assertThat(
//                mcf.resolve(crypt).get().build().check(pwd, crypt))
//            .isTrue();
//    }
//
//    @Test
//    public void testApr1CryptStrings() throws InvalidAlgorithmParameterException {
//        // A random example using htpasswd
//        Assertions.assertThat(
//            MD5_CRYPT.check(Credentials.fromPassword(
//                "secret".toCharArray()),
//                "$apr1$TqI9WECO$LHZB2DqRlk9nObiB6vJG9."))
//            .isTrue();
////                Md5Crypt.apr1Crypt("secret", "$apr1$TqI9WECO"));
//
///*
//        // empty data
//        assertEquals("$apr1$foo$P27KyD1htb4EllIPEYhqi0", Md5Crypt.apr1Crypt("", "$apr1$foo"));
//        // salt gets cut at dollar sign
//        assertEquals("$apr1$1234$mAlH7FRST6FiRZ.kcYL.j1", Md5Crypt.apr1Crypt("secret", "$apr1$1234"));
//        assertEquals("$apr1$1234$mAlH7FRST6FiRZ.kcYL.j1", Md5Crypt.apr1Crypt("secret", "$apr1$1234$567"));
//        assertEquals("$apr1$1234$mAlH7FRST6FiRZ.kcYL.j1", Md5Crypt.apr1Crypt("secret", "$apr1$1234$567$890"));
//        // salt gets cut at maximum length
//        assertEquals("$apr1$12345678$0lqb/6VUFP8JY/s/jTrIk0", Md5Crypt.apr1Crypt("secret", "$apr1$1234567890123456"));
//        assertEquals("$apr1$12345678$0lqb/6VUFP8JY/s/jTrIk0", Md5Crypt.apr1Crypt("secret", "$apr1$123456789012345678"));
//*/
//    }
//
//    @Test
//    @SuppressWarnings("deprecation") // TODO remove when Java 7 is minimum and Charsets constants can be replaced
//    public void testApr1CryptBytes() {
//        // random salt
//        final byte[] keyBytes = new byte[] { '!', 'b', 'c', '.' };
//        final String hash = Md5Crypt.apr1Crypt(keyBytes);
//        assertEquals(hash, Md5Crypt.apr1Crypt("!bc.", hash));
//
//        // An empty Bytearray equals an empty String
//        assertEquals("$apr1$foo$P27KyD1htb4EllIPEYhqi0", Md5Crypt.apr1Crypt(new byte[0], "$apr1$foo"));
//        // UTF-8 stores \u00e4 "a with diaeresis" as two bytes 0xc3 0xa4.
//        assertEquals("$apr1$./$EeFrYzWWbmTyGdf4xULYc.", Md5Crypt.apr1Crypt("t\u00e4st", "$apr1$./$"));
//        // ISO-8859-1 stores "a with diaeresis" as single byte 0xe4.
//        assertEquals("$apr1$./$kCwT1pY9qXAJElYG9q1QE1", Md5Crypt.apr1Crypt("t\u00e4st".getBytes(Charsets.ISO_8859_1), "$apr1$./$"));
//    }
//
//    @Test
//    public void testApr1CryptExplicitCall() {
//        // When explicitly called the prefix is optional
//        assertEquals("$apr1$1234$mAlH7FRST6FiRZ.kcYL.j1", Md5Crypt.apr1Crypt("secret", "1234"));
//        // When explicitly called without salt, a random one will be used.
//        assertTrue(Md5Crypt.apr1Crypt("secret".getBytes()).matches("^\\$apr1\\$[a-zA-Z0-9./]{0,8}\\$.{1,}$"));
//        assertTrue(Md5Crypt.apr1Crypt("secret".getBytes(), null).matches("^\\$apr1\\$[a-zA-Z0-9./]{0,8}\\$.{1,}$"));
//    }
//
//    @Test
//    public void testApr1LongSalt() {
//        assertEquals("$apr1$12345678$0lqb/6VUFP8JY/s/jTrIk0", Md5Crypt.apr1Crypt("secret", "12345678901234567890"));
//    }
//
//    @Test(expected = NullPointerException.class)
//    public void testApr1CryptNullData() {
//        Md5Crypt.apr1Crypt((byte[]) null);
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void testApr1CryptWithEmptySalt() {
//        Md5Crypt.apr1Crypt("secret".getBytes(), "");
//    }
//
//    @Test
//    public void testApr1CryptWithoutSalt() {
//        // Without salt, a random is generated
//        final String hash = Md5Crypt.apr1Crypt("secret");
//        assertTrue(hash.matches("^\\$apr1\\$[a-zA-Z0-9\\./]{8}\\$[a-zA-Z0-9\\./]{22}$"));
//        final String hash2 = Md5Crypt.apr1Crypt("secret");
//        assertNotSame(hash, hash2);
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void testApr1CryptWithInvalidSalt() {
//        Md5Crypt.apr1Crypt(new byte[0], "!");
//    }
//
//    @Test
//    public void testCtor() {
//        assertNotNull(new Md5Crypt()); // for code-coverage
//    }
//
//    @Test
//    public void testMd5CryptStrings() {
//        // empty data
//        assertEquals("$1$foo$9mS5ExwgIECGE5YKlD5o91", Crypt.crypt("", "$1$foo"));
//        // salt gets cut at dollar sign
//        assertEquals("$1$1234$ImZYBLmYC.rbBKg9ERxX70", Crypt.crypt("secret", "$1$1234"));
//        assertEquals("$1$1234$ImZYBLmYC.rbBKg9ERxX70", Crypt.crypt("secret", "$1$1234$567"));
//        assertEquals("$1$1234$ImZYBLmYC.rbBKg9ERxX70", Crypt.crypt("secret", "$1$1234$567$890"));
//        // salt gets cut at maximum length
//        assertEquals("$1$12345678$hj0uLpdidjPhbMMZeno8X/", Crypt.crypt("secret", "$1$1234567890123456"));
//        assertEquals("$1$12345678$hj0uLpdidjPhbMMZeno8X/", Crypt.crypt("secret", "$1$123456789012345678"));
//    }
//
//    @Test
//    @SuppressWarnings("deprecation") // TODO remove when Java 7 is minimum and Charsets constants can be replaced
//    public void testMd5CryptBytes() {
//        // An empty Bytearray equals an empty String
//        assertEquals("$1$foo$9mS5ExwgIECGE5YKlD5o91", Crypt.crypt(new byte[0], "$1$foo"));
//        // UTF-8 stores \u00e4 "a with diaeresis" as two bytes 0xc3 0xa4.
//        assertEquals("$1$./$52agTEQZs877L9jyJnCNZ1", Crypt.crypt("t\u00e4st", "$1$./$"));
//        // ISO-8859-1 stores "a with diaeresis" as single byte 0xe4.
//        assertEquals("$1$./$J2UbKzGe0Cpe63WZAt6p//", Crypt.crypt("t\u00e4st".getBytes(Charsets.ISO_8859_1), "$1$./$"));
//    }
//
//    @Test
//    public void testMd5CryptExplicitCall() {
//        assertTrue(Md5Crypt.md5Crypt("secret".getBytes()).matches("^\\$1\\$[a-zA-Z0-9./]{0,8}\\$.{1,}$"));
//        assertTrue(Md5Crypt.md5Crypt("secret".getBytes(), null).matches("^\\$1\\$[a-zA-Z0-9./]{0,8}\\$.{1,}$"));
//    }
//
//    @Test
//    public void testMd5CryptLongInput() {
//        assertEquals("$1$1234$MoxekaNNUgfPRVqoeYjCD/", Crypt.crypt("12345678901234567890", "$1$1234"));
//    }
//
//    @Test(expected = NullPointerException.class)
//    public void testMd5CryptNullData() {
//        Md5Crypt.md5Crypt((byte[]) null);
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void testMd5CryptWithEmptySalt() {
//        Md5Crypt.md5Crypt("secret".getBytes(), "");
//    }

}
