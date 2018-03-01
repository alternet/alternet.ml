package ml.alternet.security.auth.formatters;

import java.io.IOException;
import java.util.NoSuchElementException;

import javax.inject.Singleton;

import ml.alternet.encode.BytesEncoding;
import ml.alternet.scan.NumberConstraint;
import ml.alternet.scan.Scanner;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.CryptFormatter;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.Argon2Parts;
import ml.alternet.security.auth.formats.ModularCryptFormat;
import ml.alternet.security.auth.formats.ModularCryptFormat.Hashers;
import ml.alternet.security.auth.hasher.Argon2Hasher.Argon2Bridge.Type;

import static ml.alternet.security.auth.formatters.Util.*;

/**
 * The Argon2 hash format is defined by the argon2 reference implementation.
 * It’s compatible with the PHC Format and Modular Crypt Format, and uses
 * $argon2i$, $argon2d$ and $argon2id$ as it’s identifying prefixes for all
 * its strings.
 *
 * An example hash (of password) is:
 *
 * <code>$argon2i$[v=19$]m=512,t=3,p=2$c29tZXNhbHQ$SqlVijFGiPG+935vDSGEsA</code>
 *
 * This string has the format <code>$argon2X$[v=V$]m=M,t=T,p=P[,keyid=KEYID][,data=DATA]$salt$digest</code>, where:
 *
 * <ul>
 * <li>X is either i or d, depending on the argon2 variant (i in the example).</li>
 * <li>V is an integer representing the argon2 revision. the value (when rendered into
 * hexadecimal) matches the argon2 version (in the example, v=19 corresponds to 0x13,
 * or Argon2 v1.3).</li>
 * <li>M is an integer representing the variable memory cost, in kibibytes (512kib in the example).</li>
 * <li>T is an integer representing the variable time cost, in linear iterations. (3 in the example).</li>
 * <li>P is a parallelization parameter, which controls how much of the hash calculation is
 * parallelization (2 in the example).</li>
 * <li>DATA is an optional additional amount of data (omitted in the example)</li>
 * <li>salt - this is the base64-encoded version of the raw salt bytes passed into the Argon2
 * function (c29tZXNhbHQ in the example).</li>
 * <li>digest - this is the base64-encoded version of the raw derived key bytes returned from
 * the Argon2 function. Argon2 supports a variable checksum size, though the hashes
 * will typically be 16 bytes, resulting in a 22 byte digest (SqlVijFGiPG+935vDSGEsA in the
 * example).</li>
 * </ul>
 *
 * All integer values are encoded uses ascii decimal, with no leading zeros.
 * All byte strings are encoded using the standard base64 encoding, but without any trailing padding (“=”) chars.
 *
 * Note
 *
 * The v=version$ segment was added in Argon2 v1.3; older version Argon2 v1.0 hashes may not include this portion.
 * The algorithm used by all of these schemes is deliberately identical and simple: The password is encoded
 * into UTF-8 if not already encoded, and handed off to the Argon2 function. A specified number of bytes
 * (16 byte default) returned result are encoded as the checksum.
 *
 * @see ModularCryptFormat
 *
 * @author Philippe Poulard
 *
 * @see Hashers#$argon2i$
 */
@Singleton
public class Argon2CryptFormatter implements CryptFormatter<Argon2Parts> {

    @Override
    public Argon2Parts parse(String crypt, Hasher hr) {
        Argon2Parts parts = new Argon2Parts(hr);
        Scanner scanner = Scanner.of(crypt);
        try {
            ensure(scanner.hasNextChar('$', true));
            Type type = (Type) scanner.nextEnumValue(Type.class).get();
            if (! type.name().equals(parts.hr.getConfiguration().getVariant())) {
                hr = parts.hr.getBuilder().setVariant(type.name()).build();
                parts.hr = hr;
            }
            ensure(scanner.hasNextChar('$', true));
            // be smart, make the parts optionals
            if (scanner.hasNextString("v=", true)) {
                parts.version = scanner.nextNumber(NumberConstraint.INT_CONSTRAINT).intValue();
                ensure(scanner.hasNextChar('$', true));
            }
            if (scanner.hasNextString("m=", true)) {
                parts.memoryCost = scanner.nextNumber(NumberConstraint.INT_CONSTRAINT).intValue();
                ensure(scanner.hasNextChar(',', true));
            }
            if (scanner.hasNextString("t=", true)) {
                parts.timeCost = scanner.nextNumber(NumberConstraint.INT_CONSTRAINT).intValue();
                ensure(scanner.hasNextChar(',', true));
            }
            if (scanner.hasNextString("p=", true)) {
                parts.parallelism = scanner.nextNumber(NumberConstraint.INT_CONSTRAINT).intValue();
            }
            if (scanner.hasNextChar(',', true)) {
                boolean keyid = false;
                if (scanner.hasNextString("keyid=", true)) {
                    keyid = true;
                    parts.keyid = decode(hr, scanner, c -> c != ',' && c != '$');
                }
                if (! keyid || (keyid && scanner.hasNextChar(',', true))) {
                    if (scanner.hasNextString("data=", true)) {
                        parts.data = decode(hr, scanner, c -> c != ',' && c != '$');
                    }
                }
            }
            // below, if salt or hash are missing, the crypt is just use for configuring a hasher
            if (scanner.hasNext()) {
                ensure(scanner.hasNextChar('$', true));
                if (scanner.hasNext()) {
                    parts.salt = decode(hr, scanner, c -> c != '$');
                }
            }
            if (scanner.hasNext()) {
                ensure(scanner.hasNextChar('$', true));
                parts.hash = decode(hr, scanner, c -> true); // decode till the end
            }
        } catch (NullPointerException | NoSuchElementException | IOException e) {
            throw new IllegalArgumentException("Unable to parse " + crypt);
        }
        return parts;
    }

    @Override
    public String format(Argon2Parts parts) {
        StringBuffer crypt = new StringBuffer(60);
        crypt.append("$")
            .append(parts.hr.getConfiguration().getVariant())
            .append("$v=")
            .append(Integer.toString(parts.version))
            .append("$m=")
            .append(Integer.toString(parts.memoryCost))
            .append(",t=")
            .append(Integer.toString(parts.timeCost))
            .append(",p=")
            .append(Integer.toString(parts.parallelism));
        BytesEncoding encoding = parts.hr.getConfiguration().getEncoding();
        if (parts.keyid != null) {
            crypt.append(",keyid=")
            .append(encoding.encode(parts.keyid));
        }
        if (parts.data != null) {
            crypt.append(",data=")
                .append(encoding.encode(parts.data));
        }
        crypt.append("$");
        crypt.append(encoding.encode(parts.salt));
        if (parts.hash != null && parts.hash.length > 0) {
            crypt.append("$")
                .append(encoding.encode(parts.hash));
        }
        return crypt.toString();
    }

    @Override
    public CryptFormat getCryptFormat() {
        return new ModularCryptFormat();
    }

}
