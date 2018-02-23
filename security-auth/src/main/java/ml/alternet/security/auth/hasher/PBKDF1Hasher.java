package ml.alternet.security.auth.hasher;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.DigestException;
import java.security.MessageDigest;
import java.util.Arrays;

import ml.alternet.encode.BytesEncoder;
import ml.alternet.misc.Thrower;
import ml.alternet.security.Password;
import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.WorkFactorSaltedParts;
import ml.alternet.security.binary.SafeBuffer;

public class PBKDF1Hasher extends HasherBase<WorkFactorSaltedParts> {

/*// MCF :
    $sha1$ {
        @Override
        public Hasher get() {
        return Hasher.Builder.builder()
                .setClass(PBKDF1Hasher.class)
                .setScheme("SHA1")
                .setAlgorithm("SHA-1")
                .setEncoding(BytesEncoder.h64be) ????
                .setFormatter(SHA2Hasher.SHA2CRYPT_FORMATTER) TODO
                .build();
        }
    },
*/


    public PBKDF1Hasher(Builder conf) {
        super(conf);
    }

    @Override
    public byte[] encrypt(Credentials credentials, WorkFactorSaltedParts parts) {
        MessageDigest digest = Thrower.safeCall(() ->
            MessageDigest.getInstance(parts.hr.getConfiguration().getAlgorithm())
        ); // SHA1
        try (Password.Clear pwd = credentials.getPassword().getClearCopy()) {
            ByteBuffer pwdBuf = SafeBuffer.encode(CharBuffer.wrap(pwd.get()), StandardCharsets.UTF_8);
            byte[] pBytes = SafeBuffer.getData(pwdBuf);
            digest.update(pBytes);
        }
        byte[] hash = digest.digest(parts.salt);
        for (int i = 1; i < parts.workFactor; i++) {
            hash = digest.digest(hash);
        }
        return Arrays.copyOf(hash, parts.hr.getConfiguration().getHashByteSize());
    }

    @Override
    public WorkFactorSaltedParts initializeParts() {
        // TODO Auto-generated method stub
        return null;
    }

    static class PBKDF1 {

        private MessageDigest digest;
        private byte[] output;
        private byte[] firstBaseOutput;
        private int position = 0;
        private int hashnumber = 0;
        private int skip = 0;
        int state = 1;

        public byte[] getBytes(Credentials credentials, WorkFactorSaltedParts parts) throws DigestException {
            int cb = parts.hr.getConfiguration().getHashByteSize();
            digest = Thrower.safeCall(() ->
                MessageDigest.getInstance(parts.hr.getConfiguration().getAlgorithm())
            ); // SHA1
            byte[]  initial = new byte[digest.getDigestLength()];

            try (Password.Clear pwd = credentials.getPassword().getClearCopy()) {
                ByteBuffer pwdBuf = SafeBuffer.encode(CharBuffer.wrap(pwd.get()), StandardCharsets.UTF_8);
                byte[] pBytes = SafeBuffer.getData(pwdBuf);
                if (parts.salt == null) {
                    initial = digest.digest(pBytes);
                } else {
                    digest.update(pBytes);
                }
            }
            if (parts.salt != null) {
                digest.update(parts.salt);
                digest.digest(initial, 0, initial.length);
            }

            byte[] result = new byte[cb];
            int cpos = 0;
            // the initial hash (in reset) + at least one iteration
            int iter = Math.max(1, parts.workFactor - 1);

            // start with the PKCS5 key
            if (output == null) {
                // calculate the PKCS5 key
                output = initial;

                // generate new key material
                for (int i = 0; i < iter - 1; i++) {
                    output = digest.digest(output);
                }
            }

            while (cpos < cb) {
                byte[] output2 = null;
                if (hashnumber == 0) {
                    // last iteration on output
                    output2 = digest.digest(output);
                } else if (hashnumber < 1000) {
                    byte[] n = Integer.toString(hashnumber).getBytes();
                    output2 = new byte[output.length + n.length];
                    for (int j = 0; j < n.length; j++) {
                        output2[j] = n[j];
                    }
                    System.arraycopy(output, 0, output2, n.length, output.length);
                    // don't update output
                    output2 = digest.digest(output2);
                } else {
                    throw new SecurityException("too long");
                }

                int rem = output2.length - position;
                int l = Math.min(cb - cpos, rem);
                System.arraycopy(output2, position, result, cpos, l);

                cpos += l;
                position += l;
                while (position >= output2.length) {
                    position -= output2.length;
                    hashnumber++;
                }
            }

            // saving first output length
            if (state == 1) {
                if (cb > 20) {
                    skip = 40 - result.length;
                } else {
                    skip = 20 - result.length;
                }
                firstBaseOutput = new byte[result.length];
                System.arraycopy(result, 0, firstBaseOutput, 0, result.length);
                state = 2;
            }
            // processing second output
            else if (skip > 0) {
                byte[] secondBaseOutput = new byte[(firstBaseOutput.length + result.length)];
                System.arraycopy(firstBaseOutput, 0, secondBaseOutput, 0, firstBaseOutput.length);
                System.arraycopy(result, 0, secondBaseOutput, firstBaseOutput.length, result.length);
                System.arraycopy(secondBaseOutput, skip, result, 0, skip);

                skip = 0;
            }

            return result;
        }
    }

}
