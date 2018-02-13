package ml.alternet.test.security.auth.hashers;

import java.util.function.Predicate;

import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.formats.ColonCryptFormat;
import ml.alternet.security.auth.formats.WorkFactorSaltedParts;
import ml.alternet.security.auth.hashers.impl.PBKDF2Hasher;
import ml.alternet.security.binary.BytesEncoder;

public class PBKDF2b64Test extends CryptTestBase<PBKDF2Hasher, WorkFactorSaltedParts> {

    static final String UNSAFE_PASSWORD = "Da_actu@L pazzw0r|) !";

    String data[][] = {
        { UNSAFE_PASSWORD,
            "PBKDF2:1000:4s6LCkD3z89vs5Qfjngam2X+QpbkLzgi",
            "PBKDF2:1000:4s6LCkD3z89vs5Qfjngam2X+QpbkLzgi:zGl3/rbAv5CqxGwLHBR/K7370Sofua3Z"},
        { UNSAFE_PASSWORD,
            "PBKDF2:1000:uGWNzmy5WSU7dlwF6WQp0oFysI6bbnXD",
            "PBKDF2:1000:uGWNzmy5WSU7dlwF6WQp0oFysI6bbnXD:u+BetVYiks7q3Gu9SR6B4i+8ccTMTq2/"},
        };

    @Override
    protected Hasher resolve(String crypt) {
        return newHasher();
    }

    @Override
    protected String[][] getData() {
        return this.data;
    }

    @Override
    protected Predicate<String[]> acceptWrongCred() {
        return s -> true;
    }

    @Override
    protected PBKDF2Hasher newHasher() {
        return (PBKDF2Hasher) Hasher.Builder.builder()
                .setClass(PBKDF2Hasher.class)
                .setScheme("PBKDF2")
                .setAlgorithm("PBKDF2WithHmacSHA1")
                .setEncoding(BytesEncoder.base64)
                .setSaltByteSize(24)
                .setHashByteSize(24)
                .setIterations(29000)
                .setFormatter(ColonCryptFormat.COLON_CRYPT_FORMATTER)
                .build();
    }

    @Override
    protected PBKDF2Hasher newHasher(String salt) {
      return newHasher();
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
