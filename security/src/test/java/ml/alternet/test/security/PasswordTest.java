package ml.alternet.test.security;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import ml.alternet.security.EmptyPassword;
import ml.alternet.security.Password;
import ml.alternet.security.PasswordManager;
import ml.alternet.security.PasswordManagerFactory;
import ml.alternet.security.PasswordState;
import ml.alternet.security.impl.AbstractPassword;
import ml.alternet.security.impl.StandardPasswordManager;
import ml.alternet.util.BytesUtil;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class PasswordTest {

    @Test
    public void defaultManager_ShouldBe_StandardPasswordManager() {
        // first access : we get a delegate
        PasswordManager defaultPM = PasswordManagerFactory.getDefaultPasswordManager();
        // force the delegate to resolve the PM
        defaultPM.newPassword("password".toCharArray());
        // second access : the delegate has been replaced by the right impl
        defaultPM = PasswordManagerFactory.getDefaultPasswordManager();

        Assertions.assertThat(defaultPM).isInstanceOf(StandardPasswordManager.class);
    }

    @Test
    public void charArrayPassword_ShouldBe_Unset() {
        PasswordManager pm = PasswordManagerFactory.getDefaultPasswordManager();
        char[] pwd = "password".toCharArray();
        pm.newPassword(pwd);

        Assertions.assertThat(pwd).containsOnly((char) 0);
    }

    @Test
    public void clearCharArrayPassword_ShouldBe_Unset() {
        PasswordManager pm = PasswordManagerFactory.getDefaultPasswordManager();
        char[] pwd = "the password".toCharArray();
        Password password = pm.newPassword(pwd);
        char[] clearPwd;
        try (Password.Clear clear = password.getClearCopy()) {
            clearPwd = clear.get();
        }
        Assertions.assertThat(clearPwd).containsOnly((char) 0);
    }

    @Test
    public void gettingSeveralClearWeakPasswords_ShouldNot_BreakThePassword() {
        PasswordManager pm = PasswordManagerFactory.getWeakPasswordManager();
        // use string for later comparison (but in a real case it is a bad
        // practice for passwords)
        String pwd = "the password";
        Password password = pm.newPassword(pwd.toCharArray());
        for (int i = 0; i < 10; i++) {
            try (Password.Clear clear = password.getClearCopy()) {
                Assertions.assertThat(clear.get()).isEqualTo(pwd.toCharArray());
            }
        }
    }

    @Test
    public void gettingSeveralClearStandardPasswords_ShouldNot_BreakThePassword() {
        PasswordManager pm = PasswordManagerFactory.getStandardPasswordManager();
        // use string for later comparison (but in a real case it is a bad
        // practice for passwords)
        String pwd = "the password";
        Password password = pm.newPassword(pwd.toCharArray());
        for (int i = 0; i < 10; i++) {
            try (Password.Clear clear = password.getClearCopy()) {
                Assertions.assertThat(clear.get()).isEqualTo(pwd.toCharArray());
            }
        }
    }

    @Test
    public void gettingSeveralClearStrongPasswords_ShouldNot_BreakThePassword() {
        PasswordManager pm = PasswordManagerFactory.getStrongPasswordManager();
        // use string for later comparison (but in a real case it is a bad
        // practice for passwords)
        String pwd = "the password";
        Password password = pm.newPassword(pwd.toCharArray());
        for (int i = 0; i < 10; i++) {
            try (Password.Clear clear = password.getClearCopy()) {
                Assertions.assertThat(clear.get()).isEqualTo(pwd.toCharArray());
            }
        }
    }

    @Test
    public void nullPassword_Should_ReturnEmptyPassword() {
        PasswordManager pm = PasswordManagerFactory.getDefaultPasswordManager();
        char[] pwd = null;
        Password password = pm.newPassword(pwd);

        Assertions.assertThat(password).isSameAs(EmptyPassword.SINGLETON);
    }

    @Test
    public void voidPassword_Should_ReturnEmptyPassword() {
        PasswordManager pm = PasswordManagerFactory.getDefaultPasswordManager();
        char[] pwd = "".toCharArray();
        Password password = pm.newPassword(pwd);

        Assertions.assertThat(password).isSameAs(EmptyPassword.SINGLETON);
    }

    @Test
    public void emptyPassword_ShouldHave_TheEmptyState() {
        PasswordManager pm = PasswordManagerFactory.getDefaultPasswordManager();
        Password password = pm.newPassword(null);
        Assertions.assertThat(password.state()).isSameAs(PasswordState.Empty);
    }

    @Test
    public void newPassword_ShouldBe_Valid() {
        PasswordManager pm = PasswordManagerFactory.getDefaultPasswordManager();
        char[] pwd = "the password".toCharArray();
        Password password = pm.newPassword(pwd);
        Assertions.assertThat(password.state()).isSameAs(PasswordState.Valid);
    }

    @Test
    public void invalidatedPassword_ShouldHave_TheInvalidState() {
        PasswordManager pm = PasswordManagerFactory.getDefaultPasswordManager();
        char[] pwd = "the password".toCharArray();
        Password password = pm.newPassword(pwd);
        password.invalidate();
        Assertions.assertThat(password.state()).isSameAs(PasswordState.Invalid);
    }

    @Test
    public void clearStandardPassword_ShouldBe_TheSame() {
        PasswordManager pm = PasswordManagerFactory.getStandardPasswordManager();
        // use string for later comparison (but in a real case it is a bad
        // practice for passwords)
        String pwd = "the password";
        Password password = pm.newPassword(pwd.toCharArray());
        try (Password.Clear clear = password.getClearCopy()) {
            Assertions.assertThat(clear.get()).isEqualTo(pwd.toCharArray());
        }
    }

    @Test
    public void clearWeakPassword_ShouldBe_TheSame() {
        PasswordManager pm = PasswordManagerFactory.getWeakPasswordManager();
        // use string for later comparison (but in a real case it is a bad
        // practice for passwords)
        String pwd = "the password";
        Password password = pm.newPassword(pwd.toCharArray());
        try (Password.Clear clear = password.getClearCopy()) {
            Assertions.assertThat(clear.get()).isEqualTo(pwd.toCharArray());
        }
    }

    @Test
    public void clearStrongPassword_ShouldBe_TheSame() {
        PasswordManager pm = PasswordManagerFactory.getStrongPasswordManager();
        // use string for later comparison (but in a real case it is a bad
        // practice for passwords)
        String pwd = "the password";
        Password password = pm.newPassword(pwd.toCharArray());
        try (Password.Clear clear = password.getClearCopy()) {
            Assertions.assertThat(clear.get()).isEqualTo(pwd.toCharArray());
        }
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void invalidatedClearPassword_Should_ThrowException() {
        PasswordManager pm = PasswordManagerFactory.getStandardPasswordManager();
        char[] pwd = "the password".toCharArray();
        Password password = pm.newPassword(pwd);
        try (Password.Clear clear = password.getClearCopy()) {
            password.invalidate();
            clear.get();
        }
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void invalidatedPassword_Should_ThrowException() {
        PasswordManager pm = PasswordManagerFactory.getStandardPasswordManager();
        char[] pwd = "the password".toCharArray();
        Password password = pm.newPassword(pwd);
        password.invalidate();
        try (Password.Clear clear = password.getClearCopy()) {
        }
    }

    @Test
    public void privateStandardPassword_ShouldNotBe_TheSame() throws NoSuchMethodException, SecurityException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        PasswordManager pm = PasswordManagerFactory.getStandardPasswordManager();
        // use string for later comparison (but in a real case it is a bad
        // practice for passwords)
        String pwd = "the password";
        Password password = pm.newPassword(pwd.toCharArray());
        Method method = AbstractPassword.class.getDeclaredMethod("getPrivatePassword");
        method.setAccessible(true);
        byte[] priv = (byte[]) method.invoke(password);
        char[] cpriv = new char[priv.length];
        for (int i = 0; i < priv.length; i++) {
            cpriv[i] = (char) priv[i];
        }
        Assertions.assertThat(pwd.toCharArray()).isNotEqualTo(cpriv);
    }

    @Test
    public void privateStrongPassword_ShouldNotBe_TheSame() throws NoSuchMethodException, SecurityException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        PasswordManager pm = PasswordManagerFactory.getStrongPasswordManager();
        // use string for later comparison (but in a real case it is a bad
        // practice for passwords)
        String pwd = "the password";
        Password password = pm.newPassword(pwd.toCharArray());
        Method method = AbstractPassword.class.getDeclaredMethod("getPrivatePassword");
        method.setAccessible(true);
        byte[] priv = (byte[]) method.invoke(password);
        char[] cpriv = new char[priv.length];
        for (int i = 0; i < priv.length; i++) {
            cpriv[i] = (char) priv[i];
        }
        Assertions.assertThat(pwd.toCharArray()).isNotEqualTo(cpriv);
    }

    @Test
    public void privateWeakPassword_ShouldBe_TheSame_BecauseItsManagerIsWeak() throws NoSuchMethodException,
            SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        PasswordManager pm = PasswordManagerFactory.getWeakPasswordManager();
        // use string for later comparison (but in a real case it is a bad
        // practice for passwords)
        String pwd = "the password";
        Password password = pm.newPassword(pwd.toCharArray());
        Method method = AbstractPassword.class.getDeclaredMethod("getPrivatePassword");
        method.setAccessible(true);
        byte[] priv = (byte[]) method.invoke(password);
        char[] cpriv = BytesUtil.cast(priv);
        Assertions.assertThat(pwd.toCharArray()).isEqualTo(cpriv);
    }

}
