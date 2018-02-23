package ml.alternet.test.security.auth.hashers;

import java.security.InvalidAlgorithmParameterException;
import java.util.function.Predicate;

import ml.alternet.encode.BytesEncoder;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.WorkFactorSaltedParts;
import ml.alternet.security.auth.formatters.ColonCryptFormatter;
import ml.alternet.security.auth.hasher.PBKDF2Hasher;

public class PBKDF2Test extends CryptTestBase<PBKDF2Hasher, WorkFactorSaltedParts> {

    static final String UNSAFE_PASSWORD = "Da_actu@L pazzw0r|) !";

    String data[][] = {
        { UNSAFE_PASSWORD,
            "PBKDF2:99:66A402BDE5CFE340A1411C9221524C0F34A9A27E0B0805AE",
            "PBKDF2:99:66A402BDE5CFE340A1411C9221524C0F34A9A27E0B0805AE:C9BB3D469D7C7F599EA35ED956E0D9F2C282EC96A38EA91C"},
        { UNSAFE_PASSWORD,
            "PBKDF2:999:0314E17362D0D966C8F999A66045210DBE7EA897F024E07F",
            "PBKDF2:999:0314E17362D0D966C8F999A66045210DBE7EA897F024E07F:3AE344F7AB5AA17308A49FDAD997105340DD6E348FDF5623"}
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
                .setEncoding(BytesEncoder.hexa)
                .setSaltByteSize(24)
                .setHashByteSize(24)
                .setIterations(29000)
                .setFormatter(new ColonCryptFormatter())
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

    @Override
    public void crypts_should_match(String plain, String salt, String expected)
            throws InvalidAlgorithmParameterException {
        // this explain why we should NOT check crypts, but rather bytes
        super.crypts_should_match(plain, salt.toLowerCase(),
                "PBKDF2" + expected.substring("PBKDF2".length()).toLowerCase());
    }

}
