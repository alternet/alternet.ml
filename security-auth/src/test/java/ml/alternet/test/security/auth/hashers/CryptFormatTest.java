package ml.alternet.test.security.auth.hashers;

import java.security.InvalidAlgorithmParameterException;

import org.assertj.core.api.Assertions;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.formats.CurlyBracesCryptFormat;
import ml.alternet.security.auth.formats.ModularCryptFormat;

public class CryptFormatTest {

    @DataProvider(name="data")
    public Object[][] getData() {
        return new Object[][] {
            {   "password",
                    "{PBKDF2}131000$tLbWWssZ45zzfi9FiDEmxA$dQlpmhY4dGvmx4MOK/uOj/WU7Lg",
                    CurlyBracesCryptFormat.class},
            {   "secret",
                    "$1$1234$ImZYBLmYC.rbBKg9ERxX70",
                    ModularCryptFormat.class},
            {   "password",
                    "$1$gwvn5BO0$3dyk8j.UTcsNUPrLMsU6/0",
                    ModularCryptFormat.class},
            {   "password",
                    "{plaintext}password",
                    CurlyBracesCryptFormat.class},
            {   "password",
                    "{MD5}X03MO1qnZdYdgyfeuILPmQ==",
                    CurlyBracesCryptFormat.class},
            {   "password",
                    "{SHA}W6ph5Mm5Pz8GgiULbPgzG37mj9g=",
                    CurlyBracesCryptFormat.class},
            {   "password",
                    "{SMD5}jNoSMNY0cybfuBWiaGlFw3Mfi/U=",
                    CurlyBracesCryptFormat.class},
            {   "password",
                    "{SSHA}pKqkNr1tq3wtQqk+UcPyA3HnA2NsU5NJ",
                    CurlyBracesCryptFormat.class},
            {   "password",
                    "{MD5.HEX}5f4dcc3b5aa765d61d8327deb882cf99",
                    CurlyBracesCryptFormat.class},
            {   "password",
                    "{SHA.HEX}5baa61e4c9b93f3f0682250b6cf8331b7ee68fd8",
                    CurlyBracesCryptFormat.class},
            {   "password",
                    "{MD5.b64}X03MO1qnZdYdgyfeuILPmQ==",
                    CurlyBracesCryptFormat.class},
            {   "password",
                    "{SHA.b64}W6ph5Mm5Pz8GgiULbPgzG37mj9g=",
                    CurlyBracesCryptFormat.class},
            {   "password",
                    "{SMD5.b64}jNoSMNY0cybfuBWiaGlFw3Mfi/U=",
                    CurlyBracesCryptFormat.class},
            {   "password",
                    "{SSHA.b64}pKqkNr1tq3wtQqk+UcPyA3HnA2NsU5NJ",
                    CurlyBracesCryptFormat.class},
            {   "password",
                    "{CRYPT}$1$gwvn5BO0$3dyk8j.UTcsNUPrLMsU6/0",
                    CurlyBracesCryptFormat.class},

        };
    }

    @Test(dataProvider="data")
    public void password_should_matchCrypt(String password, String crypt, Class<CryptFormat> cf) throws InvalidAlgorithmParameterException, InstantiationException, IllegalAccessException {
        Assertions.assertThat(
            cf.newInstance()
                .resolve(crypt)
                .get()
                .check(
                    Credentials.fromPassword(password.toCharArray()),
                    crypt)
        ).isTrue();
    }

    @Test(dataProvider="data")
    public void wrongPassword_shouldNot_matchCrypt(String password, String crypt, Class<CryptFormat> cf) throws InvalidAlgorithmParameterException, InstantiationException, IllegalAccessException {
        Assertions.assertThat(
            cf.newInstance()
                .resolve(crypt)
                .get()
                .check(
                    Credentials.fromPassword("\\^/rongP@zzw0r|)".toCharArray()),
                    crypt)
        ).isFalse();
    }

}
