package ml.alternet.security.auth.formatters;

import ml.alternet.encode.BytesEncoding;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.CryptFormatter;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.Argon2Parts;
import ml.alternet.security.auth.formats.ModularCryptFormat;
import ml.alternet.util.StringUtil;

/**
 * The Argon2 hash format is defined by the argon2 reference implementation.
 * It’s compatible with the PHC Format and Modular Crypt Format, and uses
 * $argon2i$, $argon2d$ and $argon2id$ as it’s identifying prefixes for all
 * its strings.
 *
 * An example hash (of password) is:
 *
 * <code>$argon2i$v=19$m=512,t=3,p=2$c29tZXNhbHQ$SqlVijFGiPG+935vDSGEsA</code>
 *
 * This string has the format <code>$argon2X$v=V$m=M,t=T,p=P[,data=DATA]$salt$digest</code>, where:
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
 * @author Philippe Poulard
 */
public class Argon2CryptFormatter implements CryptFormatter<Argon2Parts> {

    @Override
    public Argon2Parts parse(String crypt, Hasher hr) {
        Argon2Parts parts = new Argon2Parts(hr);
        String[] stringParts = crypt.split("\\$");
        if (! stringParts[1].equals(parts.hr.getConfiguration().getVariant())) {
            hr = parts.hr.getBuilder().setVariant(stringParts[1]).build();
            parts.hr = hr;
        }
        int versionPresent = 0;
        if (stringParts[2].startsWith("v=")) {
            versionPresent = 1;
            parts.version = Integer.parseInt(stringParts[2].substring(2));
        }
        BytesEncoding encoding = hr.getConfiguration().getEncoding();
        if (stringParts.length > 2 + versionPresent && ! StringUtil.isVoid(stringParts[2 + versionPresent])) {
            String[] params = stringParts[2 + versionPresent].split(",");
            if (params.length > 0 && params[0].startsWith("m=")) {
                parts.memoryCost = Integer.parseInt(params[0].substring(2));
            }
            if (params.length > 1 && params[1].startsWith("t=")) {
                parts.timeCost = Integer.parseInt(params[1].substring(2));
            }
            if (params.length > 2 && params[2].startsWith("p=")) {
                parts.parallelism = Integer.parseInt(params[2].substring(2));
            }
            if (params.length > 3) {
                int keyPresent = 0;
                if (params[3].startsWith("keyid=")) {
                    keyPresent = 1;
                    parts.keyid = encoding.decode(params[3].substring(6));
                }
                if (params.length > 3 + keyPresent && params[3 + keyPresent].startsWith("data=")) {
                    parts.data = encoding.decode(params[3 + keyPresent].substring(5));
                }
            }
        }
        if (stringParts.length > 3 + versionPresent && ! StringUtil.isVoid(stringParts[3 + versionPresent])) {
            parts.salt = encoding.decode(stringParts[3 + versionPresent]);
        }
        if (stringParts.length > 4 + versionPresent && ! StringUtil.isVoid(stringParts[4 + versionPresent])) {
            parts.hash = encoding.decode(stringParts[4 + versionPresent]);
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