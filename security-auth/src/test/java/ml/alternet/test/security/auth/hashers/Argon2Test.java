package ml.alternet.test.security.auth.hashers;

import java.util.function.Predicate;

import ml.alternet.security.auth.crypt.Argon2Parts;
import ml.alternet.security.auth.hasher.Argon2Hasher;
import ml.alternet.security.auth.formats.ModularCryptFormat;

public class Argon2Test extends CryptTestBase<Argon2Hasher, Argon2Parts> {

    String data[][] = { // in CryptTestBase it is [ "PASSWORD", "SALT", "HASH" ]
            // but for Argon2, the SALT can't be cut from the hash, because the hash length is a parameter
            { "password", "$argon2i$v=19$m=512,t=2,p=2$aI2R0hpDyLm3ltLa+1/rvQ$LqPKjd6n8yniKtAithoR7A",
            "$argon2i$v=19$m=512,t=2,p=2$aI2R0hpDyLm3ltLa+1/rvQ$LqPKjd6n8yniKtAithoR7A" },

            { "password", "$argon2i$v=19$m=512,t=4,p=2$eM+ZMyYkpDRGaI3xXmuNcQ$c5DeJg3eb5dskVt1mDdxfw",
                "$argon2i$v=19$m=512,t=4,p=2$eM+ZMyYkpDRGaI3xXmuNcQ$c5DeJg3eb5dskVt1mDdxfw"}

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
    protected Argon2Hasher newHasher() {
        return (Argon2Hasher) ModularCryptFormat.Hashers.$argon2i$.get();
    }

    @Override
    protected Argon2Hasher newHasher(String salt) {
        return (Argon2Hasher) resolve(salt);
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
