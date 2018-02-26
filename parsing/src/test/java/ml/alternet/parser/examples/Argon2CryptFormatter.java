package ml.alternet.parser.examples;

import static ml.alternet.parser.Grammar.$;
import static ml.alternet.parser.Grammar.range;
import static ml.alternet.parser.Grammar.is;

import ml.alternet.encode.BytesEncoding;
import ml.alternet.misc.Thrower;
import ml.alternet.parser.Grammar;
import ml.alternet.parser.ast.NodeBuilder;
import ml.alternet.parser.ast.RuleMapper;
import ml.alternet.parser.ast.TokenMapper;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.CryptFormatter;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.Argon2Parts;
import ml.alternet.security.auth.formats.ModularCryptFormat;
import ml.alternet.security.auth.hasher.Argon2Hasher;
import ml.alternet.security.auth.hasher.Argon2Hasher.Argon2Bridge.Type;
import ml.alternet.util.StringUtil;

/**
 * ALTERNATIVE CRYPT FORMATTER BASED ON A GRAMMAR FOR ARGON2
 *
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

    /**
     * {@code $argon2X$v=V$m=M,t=T,p=P[,keyid=KEYID][,data=DATA]$salt$digest}
     *
     * @author Philippe Poulard
     */
    public interface Argon2Format extends Grammar {

        Rule Dollar = is('$').drop();
        Rule Comma = is(',').drop();
        @Fragment Token DIGIT = range('0', '9');
        @Fragment Token MINUS = is('-');
        Token NUMBER = MINUS.optional().seq(DIGIT.oneOrMore()).asNumber();

        Token BYTES = $any.except('$').except(',').oneOrMore().asToken(); // string to decode

        Token VARIANT = is(Argon2Hasher.Argon2Bridge.Type.class); // enum argon2d, argon2i, argon2id
        Token VERSION = is("v=").drop().seq(NUMBER).seq(Dollar).asNumber(int.class);
        Token MEMORY = is("m=").drop().seq(NUMBER).asNumber(int.class);
        Token TIME = is("t=").drop().seq(NUMBER).asNumber(int.class);
        Token PARALLELIZATION = is("p=").drop().seq(NUMBER).asNumber(int.class);
        Rule keyid = is("keyid=").drop().seq(BYTES);
        Rule data = is("data=").drop().seq(BYTES);
        Rule salt = is(BYTES);
        Rule digest = is(BYTES);

        @MainRule
        Rule crypt = Dollar.seq(VARIANT, Dollar, VERSION.optional(),
                    MEMORY, Comma, TIME, Comma, PARALLELIZATION,
                    Comma.seq(keyid).optional(), Comma.seq(data).optional(),
                    Dollar.seq(salt, Dollar, digest).optional());

        Argon2Format $ = $();

    }

    public static class Argon2PartsBuilder extends NodeBuilder<Object> {

        Hasher hr;

        public Argon2PartsBuilder(Hasher hr) {
            super(Argon2Format.$);

            this.hr = hr;
            Argon2Parts parts = new Argon2Parts(hr); // result of the parsing

            // tokens are holding values
            setTokenMapper(TokenMapper.$()
                .add(Argon2Format.BYTES, (stack, token, next) ->
                    // decode the bytes
                    Argon2PartsBuilder.this.hr.getConfiguration()
                        .getEncoding()
                        .decode(token.<String> getValue())
                ).add(Argon2Format.VARIANT, (stack, token, next) -> {
                    Type version = token.<Type> getValue();
                    if (! version.name().equals(parts.hr.getConfiguration().getVariant())) {
                        Hasher hasher = parts.hr.getBuilder().setVariant(version.name()).build();
                        parts.hr = hasher;
                    }
                    return null; // every other value is set to the crypt parts
                }).add(Argon2Format.VERSION, (stack, token, next) -> {
                    parts.version = (int) token.getValue();
                    return null;
                }).add(Argon2Format.MEMORY, (stack, token, next) -> {
                    parts.memoryCost = (int) token.getValue();
                    return null;
                }).add(Argon2Format.TIME, (stack, token, next) -> {
                    parts.timeCost = (int) token.getValue();
                    return null;
                }).add(Argon2Format.PARALLELIZATION, (stack, token, next) -> {
                    parts.parallelism = (int) token.getValue();
                    return null;
                }).get()
            );

            // rules have arguments
            setRuleMapper(RuleMapper.$()
                .add(Argon2Format.crypt, (stack, rule, args) -> {
                    return parts; // the main rule produce the result
                }).add(Argon2Format.keyid, (stack, rule, args) -> {
                    parts.keyid = (byte[]) args.pop().getTarget();
                    return null;
                }).add(Argon2Format.data, (stack, rule, args) -> {
                    parts.data = (byte[]) args.pop().getTarget();
                    return null;
                }).add(Argon2Format.salt, (stack, rule, args) -> {
                    parts.salt = (byte[]) args.pop().getTarget();
                    return null;
                }).add(Argon2Format.digest, (stack, rule, args) -> {
                    parts.hash = (byte[]) args.pop().getTarget();
                    return null;
                }).get()
            );
        }

    }

    @Override
    public Argon2Parts parse(String crypt, Hasher hr) {
        return (Argon2Parts) Thrower.safeCall(() ->
            new Argon2PartsBuilder(hr).build(crypt, true).get()
        );
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