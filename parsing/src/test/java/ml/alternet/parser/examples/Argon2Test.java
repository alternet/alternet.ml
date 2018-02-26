package ml.alternet.parser.examples;

import java.util.function.Predicate;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import ml.alternet.encode.BytesEncoder;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.Argon2Parts;
import ml.alternet.security.auth.hasher.Argon2Hasher;

public class Argon2Test extends CryptTestBase<Argon2Hasher, Argon2Parts> {

    @Test
    public void argon2HasherParser_Should_useAGrammar() {
        Assertions.assertThat(newHasher()
            .getConfiguration().getFormatter()).isInstanceOf(Argon2CryptFormatter.class);

        Assertions.assertThat(newHasher("$argon2i$v=19$m=512,t=2,p=2$aI2R0hpDyLm3ltLa+1/rvQ$LqPKjd6n8yniKtAithoR7A")
            .getConfiguration().getFormatter()).isInstanceOf(Argon2CryptFormatter.class);
    }

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
        return (Argon2Hasher) Hasher.Builder.builder()
            .setClass(Argon2Hasher.class)
            .setScheme("Argon2")
            .setVariant("argon2i")
            .setAlgorithm("Blake2b") // because it is its name
            .setHashByteSize(32)
            .setSaltByteSize(16)
            .setEncoding(BytesEncoder.base64_no_padding)
            .setFormatter(new Argon2CryptFormatter())
            .build();
    }

    @Override
    protected Argon2Hasher newHasher(String salt) {
        return (Argon2Hasher) newHasher().getBuilder().use(salt).build();
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
