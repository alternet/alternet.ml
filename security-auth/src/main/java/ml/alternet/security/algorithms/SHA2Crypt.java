/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ml.alternet.security.algorithms;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import ml.alternet.misc.WtfException;
import ml.alternet.security.Password;
import ml.alternet.security.binary.SafeBuffer;
import ml.alternet.util.BytesUtil;

/**
 * SHA2-based Unix crypt implementation.
 * <p>
 * Based on the C implementation released into the Public Domain by Ulrich Drepper &lt;drepper@redhat.com&gt;
 * http://www.akkadia.org/drepper/SHA-crypt.txt
 * <p>
 * Conversion to Kotlin and from there to Java in 2012 by Christian Hammers &lt;ch@lathspell.de&gt; and likewise put
 * into the Public Domain.
 * <p>
 * This class is immutable and thread-safe.
 *
 * @since 1.7
 */
public class SHA2Crypt {

    /** Default number of rounds if not explicitly specified. */
    public static final int ROUNDS_DEFAULT = 5000;

    /** Maximum number of rounds. */
    public static final int ROUNDS_MAX = 999999999;

    /** Minimum number of rounds. */
    public static final int ROUNDS_MIN = 1000;

    /** Prefix for optional rounds specification. */
    public static final String ROUNDS_PREFIX = "rounds=";

    /** The number of bytes the final hash value will have (SHA-256 variant). */
    public static final int SHA256_BLOCKSIZE = 32;

    /** The number of bytes the final hash value will have (SHA-512 variant). */
    public static final int SHA512_BLOCKSIZE = 64;

    public static byte[] encrypt(Password password, int rounds, byte[] salt, String algorithm, int blocksize) {
        ByteBuffer pwdBuf = null;
        try {
            try (Password.Clear pwd = password.getClearCopy()) {
                // 1. start digest A
                // Prepare for the real work.
                MessageDigest ctx = MessageDigest.getInstance(algorithm);

                pwdBuf = SafeBuffer.encode(CharBuffer.wrap(pwd.get()), StandardCharsets.UTF_8);
                pwdBuf.mark();
                final int keyLen = pwdBuf.limit();

                // 2. the password string is added to digest A
                /*
                 * Add the key string.
                 */
                ctx.update(pwdBuf);

                // 3. the salt string is added to digest A. This is just the salt string
                // itself without the enclosing '$', without the magic salt_prefix $5$ and
                // $6$ respectively and without the rounds=<N> specification.
                //
                // NB: the MD5 algorithm did add the $1$ salt_prefix. This is not deemed
                // necessary since it is a constant string and does not add security
                // and /possibly/ allows a plain text attack. Since the rounds=<N>
                // specification should never be added this would also create an
                // inconsistency.
                /*
                 * The last part is the salt string. This must be at most 16 characters and it ends at the first `$' character
                 * (for compatibility with existing implementations).
                 */
                ctx.update(salt);

                // 4. start digest B
                /*
                 * Compute alternate sha512 sum with input KEY, SALT, and KEY. The final result will be added to the first
                 * context.
                 */
                MessageDigest altCtx = MessageDigest.getInstance(algorithm);

                // 5. add the password to digest B
                /*
                 * Add key.
                 */
                pwdBuf.reset();
                altCtx.update(pwdBuf);

                // 6. add the salt string to digest B
                /*
                 * Add salt.
                 */
                altCtx.update(salt);

                // 7. add the password again to digest B
                /*
                 * Add key again.
                 */
                pwdBuf.reset();
                altCtx.update(pwdBuf);

                // 8. finish digest B
                /*
                 * Now get result of this (32 bytes) and add it to the other context.
                 */
                byte[] altResult = altCtx.digest();

                // 9. For each block of 32 or 64 bytes in the password string (excluding
                // the terminating NUL in the C representation), add digest B to digest A
                /*
                 * Add for any character in the key one byte of the alternate sum.
                 */
                /*
                 * (Remark: the C code comment seems wrong for key length > 32!)
                 */
                int cnt = pwdBuf.limit();
                while (cnt > blocksize) {
                    ctx.update(altResult, 0, blocksize);
                    cnt -= blocksize;
                }

                // 10. For the remaining N bytes of the password string add the first
                // N bytes of digest B to digest A
                ctx.update(altResult, 0, cnt);

                // 11. For each bit of the binary representation of the length of the
                // password string up to and including the highest 1-digit, starting
                // from to lowest bit position (numeric value 1):
                //
                // a) for a 1-digit add digest B to digest A
                //
                // b) for a 0-digit add the password string
                //
                // NB: this step differs significantly from the MD5 algorithm. It
                // adds more randomness.
                /*
                 * Take the binary representation of the length of the key and for every 1 add the alternate sum, for every 0
                 * the key.
                 */
                cnt = pwdBuf.limit();
                while (cnt > 0) {
                    if ((cnt & 1) != 0) {
                        ctx.update(altResult, 0, blocksize);
                    } else {
                        pwdBuf.reset();
                        ctx.update(pwdBuf);
                    }
                    cnt >>= 1;
                }

                // 12. finish digest A
                /*
                 * Create intermediate result.
                 */
                altResult = ctx.digest();

                // 13. start digest DP
                /*
                 * Start computation of P byte sequence.
                 */
                altCtx = MessageDigest.getInstance(algorithm);

                // 14. for every byte in the password (excluding the terminating NUL byte
                // in the C representation of the string)
                //
                // add the password to digest DP
                /*
                 * For every character in the password add the entire password.
                 */
                for (int i = 1; i <= keyLen; i++) {
                    pwdBuf.reset();
                    altCtx.update(pwdBuf);
                }

                // 15. finish digest DP
                /*
                 * Finish the digest.
                 */
                byte[] tempResult = altCtx.digest();

                // 16. produce byte sequence P of the same length as the password where
                //
                // a) for each block of 32 or 64 bytes of length of the password string
                // the entire digest DP is used
                //
                // b) for the remaining N (up to 31 or 63) bytes use the first N
                // bytes of digest DP
                /*
                 * Create byte sequence P.
                 */
                final byte[] pBytes = new byte[keyLen];
                int cp = 0;
                while (cp < keyLen - blocksize) {
                    System.arraycopy(tempResult, 0, pBytes, cp, blocksize);
                    cp += blocksize;
                }
                System.arraycopy(tempResult, 0, pBytes, cp, keyLen - cp);

                // 17. start digest DS
                /*
                 * Start computation of S byte sequence.
                 */
                altCtx = MessageDigest.getInstance(algorithm);

                // 18. repeast the following 16+A[0] times, where A[0] represents the first
                // byte in digest A interpreted as an 8-bit unsigned value
                //
                // add the salt to digest DS
                /*
                 * For every character in the password add the entire password.
                 */
                for (int i = 1; i <= 16 + (altResult[0] & 0xff); i++) {
                    altCtx.update(salt);
                }

                // 19. finish digest DS
                /*
                 * Finish the digest.
                 */
                tempResult = altCtx.digest();

                // 20. produce byte sequence S of the same length as the salt string where
                //
                // a) for each block of 32 or 64 bytes of length of the salt string
                // the entire digest DS is used
                //
                // b) for the remaining N (up to 31 or 63) bytes use the first N
                // bytes of digest DS
                /*
                 * Create byte sequence S.
                 */
                // Remark: The salt is limited to 16 chars, how does this make sense?
                final int saltLen = salt.length;
                final byte[] sBytes = new byte[saltLen];
                cp = 0;
                while (cp < saltLen - blocksize) {
                    System.arraycopy(tempResult, 0, sBytes, cp, blocksize);
                    cp += blocksize;
                }
                System.arraycopy(tempResult, 0, sBytes, cp, saltLen - cp);

                // 21. repeat a loop according to the number specified in the rounds=<N>
                // specification in the salt (or the default value if none is
                // present). Each round is numbered, starting with 0 and up to N-1.
                //
                // The loop uses a digest as input. In the first round it is the
                // digest produced in step 12. In the latter steps it is the digest
                // produced in step 21.h. The following text uses the notation
                // "digest A/C" to describe this behavior.
                /*
                 * Repeatedly run the collected hash value through sha512 to burn CPU cycles.
                 */
                for (int i = 0; i <= rounds - 1; i++) {
                    // a) start digest C
                    /*
                     * New context.
                     */
                    ctx = MessageDigest.getInstance(algorithm);

                    // b) for odd round numbers add the byte sequense P to digest C
                    // c) for even round numbers add digest A/C
                    /*
                     * Add key or last result.
                     */
                    if ((i & 1) != 0) {
                        ctx.update(pBytes, 0, keyLen);
                    } else {
                        ctx.update(altResult, 0, blocksize);
                    }

                    // d) for all round numbers not divisible by 3 add the byte sequence S
                    /*
                     * Add salt for numbers not divisible by 3.
                     */
                    if (i % 3 != 0) {
                        ctx.update(sBytes, 0, saltLen);
                    }

                    // e) for all round numbers not divisible by 7 add the byte sequence P
                    /*
                     * Add key for numbers not divisible by 7.
                     */
                    if (i % 7 != 0) {
                        ctx.update(pBytes, 0, keyLen);
                    }

                    // f) for odd round numbers add digest A/C
                    // g) for even round numbers add the byte sequence P
                    /*
                     * Add key or last result.
                     */
                    if ((i & 1) != 0) {
                        ctx.update(altResult, 0, blocksize);
                    } else {
                        ctx.update(pBytes, 0, keyLen);
                    }

                    // h) finish digest C.
                    /*
                     * Create intermediate result.
                     */
                    altResult = ctx.digest();
                }
                return altResult;
            } catch (NoSuchAlgorithmException e) {
                throw WtfException.throwException(e);
            }
        } finally {
            if (pwdBuf != null) {
                BytesUtil.unset(pwdBuf.array());
            }
        }
    }

}
