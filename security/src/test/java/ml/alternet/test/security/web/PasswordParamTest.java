package ml.alternet.test.security.web;

import java.util.Arrays;

import ml.alternet.security.Password;
import ml.alternet.security.PasswordManagerFactory;
import ml.alternet.security.PasswordState;
import ml.alternet.security.web.PasswordParam;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class PasswordParamTest {

    @Test
    public void defaultSequence_Should_containEmptyPassword() {
        PasswordParam pp = new PasswordParam();
        Assertions.assertThat(pp.state()).isEqualTo(PasswordState.Empty);
        Assertions.assertThat(pp.hasNext()).isFalse();
    }

    @Test
    public void singleSequence_Should_containWrapPassword() {
        String unsafePwd = "da P@zzW0r|)";
        Password pwd = PasswordManagerFactory.getWeakPasswordManager().newPassword(unsafePwd.toCharArray());
        PasswordParam pp = new PasswordParam(pwd);
        Assertions.assertThat(pp.state()).isEqualTo(PasswordState.Valid);
        Assertions.assertThat(pp.getClearCopy().get()).isEqualTo(unsafePwd.toCharArray());
        pp.destroy();
        Assertions.assertThat(pp.state()).isEqualTo(PasswordState.Invalid);
        Assertions.assertThat(pp.hasNext()).isFalse();
    }

    @Test
    public void fullSequence_Should_containWrapPasswords() {
        String unsafePwd1 = "da P@zzW0r|)";
        Password pwd1 = PasswordManagerFactory.getWeakPasswordManager().newPassword(unsafePwd1.toCharArray());
        String unsafePwd2 = "sesame";
        Password pwd2 = PasswordManagerFactory.getWeakPasswordManager().newPassword(unsafePwd2.toCharArray());
        String unsafePwd3 = "azERTY";
        Password pwd3 = PasswordManagerFactory.getWeakPasswordManager().newPassword(unsafePwd3.toCharArray());
        PasswordParam pp = new PasswordParam(Arrays.asList(pwd1, pwd2, pwd3).iterator());

        Assertions.assertThat(pp.state()).isEqualTo(PasswordState.Valid);
        Assertions.assertThat(pp.getClearCopy().get()).isEqualTo(unsafePwd1.toCharArray());
        Assertions.assertThat(pp.hasNext()).isTrue();

        pp.next();
        Assertions.assertThat(pp.state()).isEqualTo(PasswordState.Valid);
        Assertions.assertThat(pp.getClearCopy().get()).isEqualTo(unsafePwd2.toCharArray());
        Assertions.assertThat(pp.hasNext()).isTrue();
        pp.destroy();
        Assertions.assertThat(pp.state()).isEqualTo(PasswordState.Invalid);

        pp.next();
        Assertions.assertThat(pp.state()).isEqualTo(PasswordState.Valid);
        Assertions.assertThat(pp.getClearCopy().get()).isEqualTo(unsafePwd3.toCharArray());
        Assertions.assertThat(pp.hasNext()).isFalse();
    }

}
