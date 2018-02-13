package ml.alternet.test.security.auth.hashers;

import java.security.InvalidAlgorithmParameterException;

import org.assertj.core.api.Assertions;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.formats.CryptFormatter;
import ml.alternet.security.auth.formats.CryptParts;
import ml.alternet.security.auth.formats.CurlyBracesCryptFormat;
import ml.alternet.security.auth.formats.ModularCryptFormat;
import ml.alternet.security.auth.formats.SaltedParts;
import ml.alternet.security.auth.hashers.impl.HasherBase;

public class CryptFormatTest {

    @DataProvider(name="data")
    public Object[][] getData() {
        return new Object[][] {
            {   "password",
                    "{PBKDF2-SHA256}29000$bi3FuPcewzhnjBGi1FqLcQ$XxCvGSouMoOmuy27dcdn4XkjaF0YScsvwN8rrLnk9EM",
                    CurlyBracesCryptFormat.class},
            {   "password",
                    "$pbkdf2-sha256$29000$bi3FuPcewzhnjBGi1FqLcQ$XxCvGSouMoOmuy27dcdn4XkjaF0YScsvwN8rrLnk9EM",
                    ModularCryptFormat.class},
            {   "password",
                    "$pbkdf2-sha256$6400$0ZrzXitFSGltTQnBWOsdAw$Y11AchqV4b0sUisdZd0Xr97KWoymNE0LNNrnEgY4H9M",
                    ModularCryptFormat.class},
            {   "password",
                    "$pbkdf2-sha256$6400$.6UI/S.nXIk8jcbdHx3Fhg$98jZicV16ODfEsEZeYPGHU3kbrUrvUEXOPimVSQDD44",
                    ModularCryptFormat.class},
            {   "password",
                    "$1$5pZSV9va$azfrPr6af3Fc7dLblQXVa0",
                    ModularCryptFormat.class},
            {   "secret",
                    "$1$1234$ImZYBLmYC.rbBKg9ERxX70",
                    ModularCryptFormat.class},
            {   "password",
                    "$1$gwvn5BO0$3dyk8j.UTcsNUPrLMsU6/0",
                    ModularCryptFormat.class},
            {   "secret",
                    "$apr1$TqI9WECO$LHZB2DqRlk9nObiB6vJG9.",
                    ModularCryptFormat.class},
            {   "",
                    "$apr1$foo$P27KyD1htb4EllIPEYhqi0",
                    ModularCryptFormat.class},
            {   "secret",
                    "$apr1$1234$mAlH7FRST6FiRZ.kcYL.j1",
                    ModularCryptFormat.class},
            {   "secret",
                    "$apr1$12345678$0lqb/6VUFP8JY/s/jTrIk0",
                    ModularCryptFormat.class},
            {   "password",
                    "{CRYPT}$1$gwvn5BO0$3dyk8j.UTcsNUPrLMsU6/0",
                    CurlyBracesCryptFormat.class},
            {   "password",
                    "{plaintext}password",
                    CurlyBracesCryptFormat.class},
            {   "password",
                    "{PBKDF2}131000$tLbWWssZ45zzfi9FiDEmxA$dQlpmhY4dGvmx4MOK/uOj/WU7Lg",
                    CurlyBracesCryptFormat.class},
            {   "password",
                    "{SMD5}jNoSMNY0cybfuBWiaGlFw3Mfi/U=",
                    CurlyBracesCryptFormat.class},
            {   "password",
                    "{MD5}X03MO1qnZdYdgyfeuILPmQ==",
                    CurlyBracesCryptFormat.class},
            {   "password",
                    "{SHA}W6ph5Mm5Pz8GgiULbPgzG37mj9g=",
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
                    CurlyBracesCryptFormat.class}
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

    @SuppressWarnings("unchecked")
    @Test(dataProvider="data")
    public void encryptedPassword_should_matchCrypt(String password, String crypt, Class<CryptFormat> cf) throws InvalidAlgorithmParameterException, InstantiationException, IllegalAccessException {
        Hasher hr = cf.newInstance().resolve(crypt).get();
        Credentials cred = Credentials.fromPassword(password.toCharArray());
        CryptParts parts = hr.getConfiguration().getFormatter().parse(crypt, hr);
        String cryptPwd;
        if (parts instanceof SaltedParts) {
            // we encrypt with the salt
            SaltedParts sparts = (SaltedParts) parts;
            parts.hash = ((HasherBase<SaltedParts>) hr).encrypt(cred, sparts);
            CryptFormatter<SaltedParts> crf = (CryptFormatter<SaltedParts>) hr.getConfiguration().getFormatter();
            cryptPwd = crf.format(sparts);
        } else {
            cryptPwd = hr.encrypt(cred);
        }
        Assertions.assertThat(cryptPwd).isEqualTo(crypt);
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
