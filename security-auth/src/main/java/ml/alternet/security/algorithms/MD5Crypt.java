/*
MD5Crypt.java

Created: 3 November 1999
Release: $Name:  $
Version: $Revision: 7678 $
Last Mod Date: $Date: 2007-12-28 11:51:49 -0600 (Fri, 28 Dec 2007) $
Java Port By: Jonathan Abbey, jonabbey@arlut.utexas.edu
Original C Version:
----------------------------------------------------------------------------
"THE BEER-WARE LICENSE" (Revision 42):
<phk@login.dknet.dk> wrote this file.  As long as you retain this notice you
can do whatever you want with this stuff. If we meet some day, and you think
this stuff is worth it, you can buy me a beer in return.   Poul-Henning Kamp
----------------------------------------------------------------------------

This Java Port is

  Copyright (c) 1999-2008 The University of Texas at Austin.

  All rights reserved.

  Redistribution and use in source and binary form are permitted
  provided that distributions retain this entire copyright notice
  and comment. Neither the name of the University nor the names of
  its contributors may be used to endorse or promote products
  derived from this software without specific prior written
  permission. THIS SOFTWARE IS PROVIDED "AS IS" AND WITHOUT ANY
  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE
  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
  PARTICULAR PURPOSE.

*/
package ml.alternet.security.algorithms;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import ml.alternet.misc.Thrower;
import ml.alternet.security.Password;
import ml.alternet.security.binary.SafeBuffer;
import ml.alternet.util.BytesUtil;

/**
 * Encrypt an OpenBSD/FreeBSD/Linux-compatible md5-salted-encoded password.
 *
 * @author Jonathan Abbey
 *
 * <br>Modified September 2015
 * @author Philippe Poulard
 */
public class MD5Crypt {

    /*
     * There are two magic strings that make sense to use here.. '$1$' is the
     * magic string used by the FreeBSD/Linux/OpenBSD MD5Crypt algorithm, and
     * '$apr1$' is the magic string used by the Apache MD5Crypt algorithm.
     */
    private static final byte[] APR1_BYTES = "$apr1$".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] _1_BYTES = "$1$".getBytes(StandardCharsets.US_ASCII);

    private static MessageDigest getMD5() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            return Thrower.doThrow(ex);
        }
    }

    public static byte[] encrypt(Password password, byte[] salt, boolean isApacheVariant) {
        byte fs[]; // finalState
        MessageDigest ctx1, ctx = getMD5();
        ByteBuffer pwdBuf = null;
        try {
            try (Password.Clear pwd = password.getClearCopy()) {

                pwdBuf = SafeBuffer.encode(CharBuffer.wrap(pwd.get()), StandardCharsets.UTF_8);
                pwdBuf.mark();
                // The password first, since that is what is most unknown
                ctx.update(pwdBuf);
                // Then our magic string
                ctx.update(isApacheVariant ? APR1_BYTES : _1_BYTES);
                // Then the raw salt
                ctx.update(salt);

                /* Then just as many characters of the MD5(pw,salt,pw) */
                ctx1 = getMD5();
                pwdBuf.reset();
                ctx1.update(pwdBuf);
                ctx1.update(salt);
                pwdBuf.reset();
                ctx1.update(pwdBuf);
                fs = ctx1.digest();

                for (int pl = pwd.get().length; pl > 0; pl -= 16) {
                    ctx.update(fs, 0, pl > 16 ? 16 : pl);
                }

                /* the original code claimed that finalState was being cleared
                to keep dangerous bits out of memory, but doing this is also
                required in order to get the right output. */
                BytesUtil.unset(fs);

                /* Then something really weird... */
                for (int i = pwd.get().length; i != 0; i >>>=1) {
                    if ((i & 1) != 0) {
                        ctx.update(fs, 0, 1);
                    } else {
                        pwdBuf.reset();
                        ctx.update(pwdBuf.get());
                        // ctx.update(password.getBytes(), 0, 1);
                    }
                }
            }

            fs = ctx.digest();

            /*
             * and now, just to make sure things don't run too fast
             * On a 60 Mhz Pentium this takes 34 msec, so you would
             * need 30 seconds to build a 1000 entry dictionary...
             *
             * (The above timings from the C version)
             */
            for (int i = 0; i < 1000; i++) {
                ctx1.reset();
                if ((i & 1) != 0) {
                    pwdBuf.reset();
                    ctx1.update(pwdBuf);
                } else {
                    ctx1.update(fs, 0, 16);
                }

                if ((i % 3) != 0) {
                    ctx1.update(salt);
                }
                if ((i % 7) != 0) {
                    pwdBuf.reset();
                    ctx1.update(pwdBuf);
                }
                if ((i & 1) != 0) {
                    ctx1.update(fs, 0, 16);
                } else {
                    pwdBuf.reset();
                    ctx1.update(pwdBuf);
                }
                fs = ctx1.digest();
            }
        } finally {
            if (pwdBuf != null) {
                BytesUtil.unset(pwdBuf.array());
            }
        }
        return fs;
    }

}
