package ml.alternet.test.security.auth.hashers;

import java.util.function.Predicate;

import org.testng.annotations.Test;

import ml.alternet.security.auth.crypt.CryptParts;
import ml.alternet.security.auth.hasher.MessageHasher;
import ml.alternet.security.auth.formats.ModularCryptFormat;

@Test
public class NTHashTest extends CryptTestBase<MessageHasher, CryptParts> {

    String data[][] = {
        { "password", "$3$$",
                      "$3$$8846f7eaee8fb117ad06bdd830b7586c" },
        { "d@s Pà§§\\^/0R|)", "$3$$",
                      "$3$$3e95db79fced020f301d8df799a0bf0f" }
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
    protected MessageHasher newHasher() {
        return (MessageHasher) ModularCryptFormat.Hashers.$3$.get();
    }

    @Override
    protected MessageHasher newHasher(String salt) {
      return (MessageHasher) resolve(salt);
    }

    @Override
    protected boolean altAlgo() {
        return false;
    }

    @Override
    protected String altCrypt(String plain, String salt) {
        return null;
    }

    @Override
    protected boolean altCheck(String plain, String expected) {
        return false;
    }

}
