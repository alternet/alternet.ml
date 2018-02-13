package ml.alternet.test.security.auth.hashers;

import java.util.function.Predicate;

import org.apache.commons.codec.digest.Crypt;
import org.testng.annotations.Test;

import ml.alternet.security.auth.crypt.WorkFactorSaltedParts;
import ml.alternet.security.auth.hasher.SHA2Hasher;
import ml.alternet.security.auth.hashers.ModularCryptFormatHashers;

@Test
public class SHA2CryptTest extends CryptTestBase<SHA2Hasher, WorkFactorSaltedParts> {

    String data[][] = {
        { "password", "$6$rounds=656000$dWon8CGHFnBG3arr",
                      "$6$rounds=656000$dWon8CGHFnBG3arr$HRumKDkl.jZRScX0ToRnA6i2tA448SRvtqHkpamcN/3ioy/g5tLG83.Is/ZpKjN8c2MOwRgi9jYBcpxaQ7wfh." },
        { "d@s Pà§§\\^/0R|)", "$6$rounds=656000$XUKXYod7BzClCArs",
                      "$6$rounds=656000$XUKXYod7BzClCArs$SVIFIpM5a23l2up0qG94okZv7FhiQH.9ovlONr3ODG5fJjLKd0js/9THfGzzHKVDXNlWpKC0jGeNnA7Ot0Y1h1"
        },
        { "password", "$6$49gH89TK",
                      "$6$49gH89TK$kt//rwoKf1ad/.hnthg363594OMwnM8Z4XScLZug4HdA36pw62AST6/kbirnypS5uzha83Ew2AmITy2HrCW3O0" },
        { "password", "$5$Gbk2Pwra",
                      "$5$Gbk2Pwra$NpET.3X2eOP/fE7wUQIKbghUdN73SfiJIWMJ2fq1lX1" },
        { "password", "$5$rounds=80000$wnsT7Yr92oJoP28r",
                      "$5$rounds=80000$wnsT7Yr92oJoP28r$cKhJImk5mfuSKV9b3mumNzlbstFUplKtQXXMo4G6Ep5" },
        { "password", "$5$rounds=12345$q3hvJE5mn5jKRsW.",
                      "$5$rounds=12345$q3hvJE5mn5jKRsW.$BbbYTFiaImz9rTy03GGi.Jf9YY5bmxN0LU3p3uI1iUB" }
    };

    @Override
    protected String[][] getData() {
        return this.data;
    }

    @Override
    protected Predicate<String[]> acceptWrongCred() {
        return s -> true;
    }

    @Override
    protected SHA2Hasher newHasher() {
        return (SHA2Hasher) ModularCryptFormatHashers.$5$.get();
    }

    @Override
    protected SHA2Hasher newHasher(String salt) {
      return (SHA2Hasher) resolve(salt);
    }

    @Override
    protected String altCrypt(String plain, String salt) {
        return Crypt.crypt(plain, salt);
    }

    @Override
    protected boolean altCheck(String plain, String expected) {
        return Crypt.crypt(plain, expected).equals(expected);
    }

}
