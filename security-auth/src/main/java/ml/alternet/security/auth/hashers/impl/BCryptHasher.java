package ml.alternet.security.auth.hashers.impl;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import ml.alternet.security.Password;
import ml.alternet.security.algorithms.BCrypt;
import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.formats.CryptFormatter;
import ml.alternet.security.auth.formats.WorkFactorSaltedParts;
import ml.alternet.security.auth.hashers.ModularCryptFormatHashers;
import ml.alternet.security.binary.SafeBuffer;
import ml.alternet.security.binary.BytesEncoding;
import ml.alternet.util.BytesUtil;
import ml.alternet.util.StringUtil;

/**
 * The BCrypt hasher.
 *
 * @author Philippe Poulard
 */
public class BCryptHasher extends HasherBase<WorkFactorSaltedParts> {

    public BCryptHasher(Configuration config) {
        super(config);
    }

//    @Override
//    public Properties getConfiguration() {
//        Properties prop = super.getConfiguration();
//        prop.put(Builder.LOG_ROUNDS_PROPERTY_NAME, getLogRounds());
//        return prop;
//    }

    static char getVersion(Hasher hr) {
        String variant = hr.getConfiguration().getVariant();
        if (variant != null && variant.length() > 0) {
            return variant.charAt(1);
        } else {
            return (char) 0;
        }
    }

    @Override
    public byte[] encrypt(Credentials credentials, WorkFactorSaltedParts parts) {
        ByteBuffer bb;
        try (Password.Clear pwd = credentials.getPassword().getClearCopy()) {
            bb = SafeBuffer.encode(CharBuffer.wrap(pwd.get()), getConfiguration().getCharset());
        }
        if (getVersion(this) >= 'a') {
            // append some more bytes to the buffer
            byte[] minorBytes = "\000".getBytes(getConfiguration().getCharset());
            bb = SafeBuffer.append(bb, minorBytes);
        }
        byte[] passwordb = SafeBuffer.getData(bb);
        byte[] hash = new BCrypt().crypt_raw(passwordb, parts.salt, parts.workFactor);
        BytesUtil.unset(passwordb);

        // adjust size
        if (hash.length > BCrypt.resLen) {
            byte[] b = new byte[BCrypt.resLen];
            System.arraycopy(hash, 0, b, 0, BCrypt.resLen);
            hash = b;
        }
        return hash;
    }

    /**
     * The most popular formatter for BCrypt is the Modular Crypt Format.
     *
     * <p>"<tt>password</tt>" -&gt; "<tt>$2a$08$YkG5/ze2FPw8C6vuAs7WHuvS0IeyyQfLgE7Ti8tT5F2sMEkVJlNo.</tt>"</p>
     *
     * @see ModularCryptFormatHashers
     */
    public static final CryptFormatter<WorkFactorSaltedParts> BCRYPT_FORMATTER = new CryptFormatter<WorkFactorSaltedParts>() {
        @Override
        public WorkFactorSaltedParts parse(String crypt, Hasher hr) {
            WorkFactorSaltedParts parts = new WorkFactorSaltedParts(hr);
            String[] stringParts = crypt.split("\\$");
            if (! stringParts[1].equals(parts.hr.getConfiguration().getVariant())) {
                hr = parts.hr.getBuilder().setVariant(stringParts[1]).build();
                parts.hr = hr;
            }
            if (stringParts.length > 2 && ! StringUtil.isVoid(stringParts[2])) {
                parts.workFactor = Integer.parseInt(stringParts[2]);
            }
            if (stringParts.length > 3 && ! StringUtil.isVoid(stringParts[3])) {
                String salt = stringParts[3].substring(0, 22);
                BytesEncoding encoding = hr.getConfiguration().getEncoding();
                parts.salt = encoding.decode(salt);
                String hash = stringParts[3].substring(22);
                if (hash.length() > 0) {
                    parts.hash = encoding.decode(hash);
                }
            }
            return parts;
//            char version = (char) 0;
//
//            if (crypt.charAt(0) != '$' || crypt.charAt(1) != '2')
//                throw new IllegalArgumentException ("Invalid salt version");
//            if (crypt.charAt(2) == '$') {
//                off = 3;
//            } else {
//                version = crypt.charAt(2);
//                if ((version != 'a' && version != 'b' && version != 'y') || crypt.charAt(3) != '$')
//                    throw new IllegalArgumentException ("Invalid salt revision");
//                off = 4;
//            }
//            if (crypt.charAt(off + 2) > '$') {
//                throw new IllegalArgumentException ("Missing salt rounds");
//            }
//            parts.workFactor = Integer.parseInt(crypt.substring(off, off + 2));
//
//            String realSalt = crypt.substring(off + 3, off + 25);
//            BytesEncoding encoding = hr.getConfiguration().getEncoding();
//            parts.salt = encoding.decode(realSalt);
//            String hash = crypt.substring(off + 25);
//            if (hash.length() > 0) {
//                parts.hash = encoding.decode(hash);
//            }
//            return parts;
        }

        @Override
        public String format(WorkFactorSaltedParts parts) {
            StringBuffer crypt = new StringBuffer(60);
            crypt.append("$2");
            char version = getVersion(parts.hr);
            if (version >= 'a') {
                crypt.append(version);
            }
            crypt.append("$");
            if (parts.workFactor < 10)
                crypt.append("0");
            if (parts.workFactor > 30) {
                throw new IllegalArgumentException(
                        "log_rounds exceeds maximum (30)");
            }
            crypt.append(Integer.toString(parts.workFactor));
            crypt.append("$");
            BytesEncoding encoding = parts.hr.getConfiguration().getEncoding();
            crypt.append(encoding.encode(parts.salt));
            if (parts.hash != null && parts.hash.length > 0) {
                crypt.append(encoding.encode(parts.hash));
            }
            return crypt.toString();
        }
    };

    @Override
    public WorkFactorSaltedParts initializeParts() {
        WorkFactorSaltedParts parts = new WorkFactorSaltedParts(this);
        parts.workFactor = getConfiguration().getLogRounds();
        parts.generateSalt();
        return parts;
    }

    @Override
    public String getScheme() {
        return "Blowfish";
    }

}