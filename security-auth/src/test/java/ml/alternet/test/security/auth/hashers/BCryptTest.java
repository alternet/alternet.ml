// Copyright (c) 2006 Damien Miller <djm@mindrot.org>
//
// Permission to use, copy, modify, and distribute this software for any
// purpose with or without fee is hereby granted, provided that the above
// copyright notice and this permission notice appear in all copies.
//
// THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
// WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
// MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
// ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
// WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
// ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
// OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

package ml.alternet.test.security.auth.hashers;

import java.security.InvalidAlgorithmParameterException;
import java.util.function.Predicate;

import org.assertj.core.api.Assertions;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import jodd.util.BCrypt;
import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.formats.WorkFactorSaltedParts;
import ml.alternet.security.auth.hashers.ModularCryptFormatHashers;
import ml.alternet.security.auth.hashers.impl.BCryptHasher;

/**
 * unit tests for BCrypt routines
 * @author Damien Miller
 * @version 0.2
 *
 * Adapted to TestNG
 *
 * @author Philippe Poulard
 */
public class BCryptTest extends CryptTestBase<BCryptHasher, WorkFactorSaltedParts> {

    String data[][] = {
//            { "PASSWORD",
//                "SALT",
//                "HASH" },
            { "",
                "$2a$06$DCq7YPn5Rq63x1Lad4cll.",
                "$2a$06$DCq7YPn5Rq63x1Lad4cll.TV4S6ytwfsfvkgY8jIucDrjc8deX1s." },
            { "",
                "$2a$08$HqWuK6/Ng6sg9gQzbLrgb.",
                "$2a$08$HqWuK6/Ng6sg9gQzbLrgb.Tl.ZHfXLhvt/SgVyWhQqgqcZ7ZuUtye" },
            { "",
                "$2a$10$k1wbIrmNyFAPwPVPSVa/ze",
                "$2a$10$k1wbIrmNyFAPwPVPSVa/zecw2BCEnBwVS2GbrmgzxFUOqW9dk4TCW" },
            { "",
                "$2a$12$k42ZFHFWqBp3vWli.nIn8u",
                "$2a$12$k42ZFHFWqBp3vWli.nIn8uYyIkbvYRvodzbfbK18SSsY.CsIQPlxO" },
            { "a",
                "$2a$06$m0CrhHm10qJ3lXRY.5zDGO",
                "$2a$06$m0CrhHm10qJ3lXRY.5zDGO3rS2KdeeWLuGmsfGlMfOxih58VYVfxe" },
            { "a",
                "$2a$08$cfcvVd2aQ8CMvoMpP2EBfe",
                "$2a$08$cfcvVd2aQ8CMvoMpP2EBfeodLEkkFJ9umNEfPD18.hUF62qqlC/V." },
            { "a",
                "$2a$10$k87L/MF28Q673VKh8/cPi.",
                "$2a$10$k87L/MF28Q673VKh8/cPi.SUl7MU/rWuSiIDDFayrKk/1tBsSQu4u" },
            { "a",
                "$2a$12$8NJH3LsPrANStV6XtBakCe",
                "$2a$12$8NJH3LsPrANStV6XtBakCez0cKHXVxmvxIlcz785vxAIZrihHZpeS" },
            { "abc",
                "$2a$06$If6bvum7DFjUnE9p2uDeDu",
                "$2a$06$If6bvum7DFjUnE9p2uDeDu0YHzrHM6tf.iqN8.yx.jNN1ILEf7h0i" },
            { "abc",
                "$2a$08$Ro0CUfOqk6cXEKf3dyaM7O",
                "$2a$08$Ro0CUfOqk6cXEKf3dyaM7OhSCvnwM9s4wIX9JeLapehKK5YdLxKcm" },
            { "abc",
                "$2a$10$WvvTPHKwdBJ3uk0Z37EMR.",
                "$2a$10$WvvTPHKwdBJ3uk0Z37EMR.hLA2W6N9AEBhEgrAOljy2Ae5MtaSIUi" },
            { "abc",
                "$2a$12$EXRkfkdmXn2gzds2SSitu.",
                "$2a$12$EXRkfkdmXn2gzds2SSitu.MW9.gAVqa9eLS1//RYtYCmB1eLHg.9q" },
            { "abcdefghijklmnopqrstuvwxyz",
                "$2a$06$.rCVZVOThsIa97pEDOxvGu",
                "$2a$06$.rCVZVOThsIa97pEDOxvGuRRgzG64bvtJ0938xuqzv18d3ZpQhstC" },
            { "abcdefghijklmnopqrstuvwxyz",
                "$2a$08$aTsUwsyowQuzRrDqFflhge",
                "$2a$08$aTsUwsyowQuzRrDqFflhgekJ8d9/7Z3GV3UcgvzQW3J5zMyrTvlz." },
            { "abcdefghijklmnopqrstuvwxyz",
                "$2a$10$fVH8e28OQRj9tqiDXs1e1u",
                "$2a$10$fVH8e28OQRj9tqiDXs1e1uxpsjN0c7II7YPKXua2NAKYvM6iQk7dq" },
            { "abcdefghijklmnopqrstuvwxyz",
                "$2a$12$D4G5f18o7aMMfwasBL7Gpu",
                "$2a$12$D4G5f18o7aMMfwasBL7GpuQWuP3pkrZrOAnqP.bmezbMng.QwJ/pG" },
            { "~!@#$%^&*()      ~!@#$%^&*()PNBFRD",
                "$2a$06$fPIsBO8qRqkjj273rfaOI.",
                "$2a$06$fPIsBO8qRqkjj273rfaOI.HtSV9jLDpTbZn782DC6/t7qT67P6FfO" },
            { "~!@#$%^&*()      ~!@#$%^&*()PNBFRD",
                "$2a$08$Eq2r4G/76Wv39MzSX262hu",
                "$2a$08$Eq2r4G/76Wv39MzSX262huzPz612MZiYHVUJe/OcOql2jo4.9UxTW" },
            { "~!@#$%^&*()      ~!@#$%^&*()PNBFRD",
                "$2a$10$LgfYWkbzEvQ4JakH7rOvHe",
                "$2a$10$LgfYWkbzEvQ4JakH7rOvHe0y8pHKF9OaFgwUZ2q7W2FFZmZzJYlfS" },
            { "~!@#$%^&*()      ~!@#$%^&*()PNBFRD",
                "$2a$12$WApznUOJfkEGSmYRfnkrPO",
                "$2a$12$WApznUOJfkEGSmYRfnkrPOr466oFDCaj4b6HY3EXGvfxm43seyhgC" },
            { "password",
                "$2a$08$YkG5/ze2FPw8C6vuAs7WHu",
                "$2a$08$YkG5/ze2FPw8C6vuAs7WHuvS0IeyyQfLgE7Ti8tT5F2sMEkVJlNo."},
            { "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
                "$2a$08$YkG5/ze2FPw8C6vuAs7WHu",
                "$2a$08$YkG5/ze2FPw8C6vuAs7WHuF3ECZado/tsSXGiF08J4Wa.ZrwE.eTe"},
            { "비밀번호",
                "$2a$08$YkG5/ze2FPw8C6vuAs7WHu",
                "$2a$08$YkG5/ze2FPw8C6vuAs7WHu0Sh0l821Y32X4qFDZAkWBVqKQDeayVG"},
            { "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
                "$2b$08$YkG5/ze2FPw8C6vuAs7WHu",
                "$2b$08$YkG5/ze2FPw8C6vuAs7WHuF3ECZado/tsSXGiF08J4Wa.ZrwE.eTe"},
            { "비밀번호",
                "$2b$08$YkG5/ze2FPw8C6vuAs7WHu",
                "$2b$08$YkG5/ze2FPw8C6vuAs7WHu0Sh0l821Y32X4qFDZAkWBVqKQDeayVG"},
            { "password",
                "$bcrypt-sha256$2a,12$LrmaIX5x4TRtAwEfwJZa1.$",
                "$bcrypt-sha256$2a,12$LrmaIX5x4TRtAwEfwJZa1.$2ehnw6LvuIUTM0iz4iz9hTxv21B6KFO"},
            { "password",
                "$bcrypt-sha256$2b,13$Mant9jKTadXYyFh7xp1W5.$",
                "$bcrypt-sha256$2b,13$Mant9jKTadXYyFh7xp1W5.$J8xpPZR/HxH7f1vRCNUjBI7Ev1al0hu"}
    };

    @Override
    protected String[][] getData() {
        return this.data;
    }

    @DataProvider(name="pwdWithLogRounds")
    Object[][] getPwdWithLogRounds() {
        String[][] pwd = getPwd();
        Object[][] o = new Object[pwd.length *9][2];
        for (int i = 4; i <= 12; i++) {
            for (int j =0 ; j < pwd.length; j++) {
                int k = (i-4) * pwd.length + j;
                o[k][0] = i;
                o[k][1] = pwd[j][0];
            }
        }
        return o;
    }

    @Override
    protected Predicate<String[]> acceptWrongCred() {
        return s -> s[2].startsWith("$2a$");
    }

    @Override
    protected BCryptHasher newHasher() {
        return (BCryptHasher) ModularCryptFormatHashers.$2a$.get();
    }

    @Override
    protected BCryptHasher newHasher(String salt) {
        return (BCryptHasher) resolve(salt);
    }

    /**
     * Test method for 'BCrypt.hashpw(String, String)'
     * @throws InvalidAlgorithmParameterException
     */
    @Override
    @Test(dataProvider="creds")
    public void crypts_should_match(String plain, String salt, String expected) throws InvalidAlgorithmParameterException {
        Credentials cred = Credentials.fromPassword(plain.toCharArray());
        String hashed = encrypt(cred, salt);
        if (salt.startsWith("$2b$")) {
            // because $2b$ parse with $2b$ but format to $2a$
            hashed = hashed.substring("$2b$".length());
            expected = expected.substring("$2b$".length());
        }
        Assertions.assertThat(hashed).isEqualTo(expected);

        if (salt.startsWith("$2a$")) {
            // check that BCrypt behaviour is the same
            String hashedChecked = BCrypt.hashpw(plain, salt);
            Assertions.assertThat(hashedChecked).isEqualTo(expected);
        }
    }

    @Override
    protected String altCrypt(String plain, String salt) {
        return BCrypt.hashpw(plain, salt);
    }

    @Override
    protected boolean altCheck(String plain, String expected) {
        return BCrypt.checkpw(plain, expected);
    }

    /**
     * Test method for 'BCrypt.checkpw(String, String)'
     * expecting success
     * @throws InvalidAlgorithmParameterException
     */
    @Override
    @Test(dataProvider="goodCreds")
    public void checkPassword_should_success(String plain, String expected) throws InvalidAlgorithmParameterException {
        Credentials cred = Credentials.fromPassword(plain.toCharArray());
        Hasher hr = resolve(expected);
        Assertions.assertThat(hr.check(cred, expected)).isTrue();

        if (expected.startsWith("$2a$")) {
            // check that BCrypt behaviour is the same
            Assertions.assertThat(BCrypt.checkpw(plain, expected)).isTrue();
        }
    }

    /**
     * Test method for 'BCrypt.gensalt(int)'
     * @throws InvalidAlgorithmParameterException
     */
    @Test(dataProvider="pwdWithLogRounds")
    public void logRound_should_affectCrypt(int logRounds, String plain) throws InvalidAlgorithmParameterException {
        Hasher hr = ModularCryptFormatHashers.$2a$.get().getBuilder().setLogRounds(logRounds).build();
        Credentials cred = Credentials.fromPassword(plain.toCharArray());
        String hashed1 = hr.encrypt(cred);
        String hashed2 = encrypt(cred, hashed1);
        Assertions.assertThat(hashed1).isEqualTo(hashed2);

        // check that the log round was in the crypt
        BCryptHasher bcrypt = newHasher(hashed1);
        WorkFactorSaltedParts parts = (WorkFactorSaltedParts) bcrypt.getConfiguration().getFormatter()
            .parse(hashed1, bcrypt);
        Assertions.assertThat(parts.workFactor).isEqualTo(logRounds);
    }

    /**
     * Test for correct hashing of non-US-ASCII passwords
     * @throws InvalidAlgorithmParameterException
     */
    @Test
    public void internationalChars_should_beHashedCorrectly() throws InvalidAlgorithmParameterException {
        String pw1 = "\u2605\u2605\u2605\u2605\u2605\u2605\u2605\u2605";
        String pw2 = "????????";
        Credentials cred1 = Credentials.fromPassword(pw1.toCharArray());
        Credentials cred2 = Credentials.fromPassword(pw2.toCharArray());

        String h1 = hr.encrypt(cred1);
        Assertions.assertThat(hr.check(cred2, h1)).isFalse();

        String h2 = hr.encrypt(cred2);
        Assertions.assertThat(hr.check(cred1, h2)).isFalse();
    }

}

