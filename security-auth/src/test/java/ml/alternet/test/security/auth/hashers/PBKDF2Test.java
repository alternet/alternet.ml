package ml.alternet.test.security.auth.hashers;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.Permission;
import java.util.List;
import java.util.Properties;

import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.formats.ColonCryptFormat;
import ml.alternet.security.auth.hashers.impl.PBKDF2Hasher;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.assertj.core.util.Arrays;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class PBKDF2Test {

    static final String UNSAFE_PASSWORD = "Da_actu@L pazzw0r|) !";

    @DataProvider(name="crypts")
    public Object [][] crypts() {
        return new Object[][] {
                {"PBKDF2:1000:4s6LCkD3z89vs5Qfjngam2X+QpbkLzgi:zGl3/rbAv5CqxGwLHBR/K7370Sofua3Z"},
                {"PBKDF2:1000:uGWNzmy5WSU7dlwF6WQp0oFysI6bbnXD:u+BetVYiks7q3Gu9SR6B4i+8ccTMTq2/"},
                {"PBKDF2:99:66A402BDE5CFE340A1411C9221524C0F34A9A27E0B0805AE:C9BB3D469D7C7F599EA35ED956E0D9F2C282EC96A38EA91C"},
                {"PBKDF2:999:0314E17362D0D966C8F999A66045210DBE7EA897F024E07F:3AE344F7AB5AA17308A49FDAD997105340DD6E348FDF5623"}
        };
    }

//    @Test(expectedExceptions = InvalidAlgorithmParameterException.class, expectedExceptionsMessageRegExp = "Some properties are not supported by this hasher.*")
//    public void PBKDF2Hasher_Should_RejectUnknownProperty() throws InvalidAlgorithmParameterException {
//        java.util.Arrays.fill(a, val);;
//        Properties prop = new Properties();
//        prop.put(PBKDF2Hasher.SALT_BYTE_SIZE_PROPERTY_NAME, 16);
//        prop.put(PBKDF2Hasher.HASH_BYTE_SIZE_PROPERTY_NAME, 32);
//        prop.put(PBKDF2Hasher.ITERATIONS_PROPERTY_NAME, 99);
//        prop.put(PBKDF2Hasher.ENCODING_PROPERTY_NAME, "hexa");
//        prop.put("doItLikeThis", "likeThat");
//        Hasher h = HashUtil.lookup(ColonCryptFormat.SINGLETON.family(), "PBKDF2", prop).get();
//        System.out.println(h);
//    }
//
//    @Test(expectedExceptions = InvalidAlgorithmParameterException.class, expectedExceptionsMessageRegExp = ".* must be an integer")
//    public void PBKDF2Hasher_Should_RejectPropertyWithBadType() throws InvalidAlgorithmParameterException {
//        Properties prop = new Properties();
//        prop.put(PBKDF2Hasher.SALT_BYTE_SIZE_PROPERTY_NAME, "16");
//        prop.put(PBKDF2Hasher.HASH_BYTE_SIZE_PROPERTY_NAME, "32");
//        prop.put(PBKDF2Hasher.ITERATIONS_PROPERTY_NAME, "99");
//        Hasher h = HashUtil.lookup(ColonCryptFormat.SINGLETON.family(), "PBKDF2", prop).get();
//        System.out.println(h);
//    }
//
//    @Test
//    public void PBKDF2Hasher_Should_AcceptKnownProperties() throws InvalidAlgorithmParameterException {
//        Hasher h = configureHasher(16, 32, 99, "hexa");
//        Properties hp = h.getConfiguration().get();
//        Assertions.assertThat(hp.get(PBKDF2Hasher.SALT_BYTE_SIZE_PROPERTY_NAME)).isEqualTo(16);
//        Assertions.assertThat(hp.get(PBKDF2Hasher.HASH_BYTE_SIZE_PROPERTY_NAME)).isEqualTo(32);
//        Assertions.assertThat(hp.get(PBKDF2Hasher.ITERATIONS_PROPERTY_NAME)).isEqualTo(99);
//        Assertions.assertThat(hp.get(PBKDF2Hasher.ENCODING_PROPERTY_NAME)).isEqualTo("hexa");
//    }
//
//    private Hasher configureHasher(int sbs, int hbs, int iter, String encoding) throws InvalidAlgorithmParameterException {
//        Properties prop = new Properties();
//        prop.put(PBKDF2Hasher.SALT_BYTE_SIZE_PROPERTY_NAME, sbs);
//        prop.put(PBKDF2Hasher.HASH_BYTE_SIZE_PROPERTY_NAME, hbs);
//        prop.put(PBKDF2Hasher.ITERATIONS_PROPERTY_NAME, iter);
//        prop.put(PBKDF2Hasher.ENCODING_PROPERTY_NAME, encoding);
//        Hasher h = HashUtil.lookup(ColonCryptFormat.SINGLETON.family(), "PBKDF2", prop).get();
//        return h;
//    }
//
//    @Test
//    public void PBKDF2Crypt_Should_TakeCareOfConfiguration() throws InvalidAlgorithmParameterException {
//        int sbs = 16;
//        int hbs = 32;
//        Hasher h = configureHasher(16, 32, 99, "hexa");
//        String ch = h.encrypt(UNSAFE_PASSWORD.toCharArray());
//        String[] parts = ch.split(":");
//        Assertions.assertThat(parts.length).isEqualTo(4);
//        Assertions.assertThat(parts[0]).isEqualTo(h.getScheme());
//        Integer iter = (Integer) h.getConfiguration().get().get(PBKDF2Hasher.ITERATIONS_PROPERTY_NAME);
//        Assertions.assertThat(Integer.parseInt(parts[1])).isEqualTo(iter);
//        Assertions.assertThat(parts[2].length())
//        .as("Salt should have the required size").isEqualTo(sbs * 2);
//        Assertions.assertThat(parts[3].length())
//        .as("Hash should have the required size").isEqualTo(hbs * 2);
//    }
//
//    @Test
//    public void PBKDF2Hasher_Should_HaveDefaultConfiguration() throws InvalidAlgorithmParameterException {
//        Hasher h = Hasher.getDefault();
//        Properties hp = h.getConfiguration().get();
//        Assertions.assertThat(hp.size()).isEqualTo(4);
//        Assertions.assertThat(hp.get(PBKDF2Hasher.SALT_BYTE_SIZE_PROPERTY_NAME)).isNotNull();
//        Assertions.assertThat(hp.get(PBKDF2Hasher.HASH_BYTE_SIZE_PROPERTY_NAME)).isNotNull();
//        Assertions.assertThat(hp.get(PBKDF2Hasher.ITERATIONS_PROPERTY_NAME)).isNotNull();
//        Assertions.assertThat(hp.get(PBKDF2Hasher.ENCODING_PROPERTY_NAME)).isEqualTo("base64");
//        Assertions.assertThat(hp.get("doItLikeThis")).isNull();
//    }
//
//    @Test
//    public void PBKDF2Crypt_Should_HaveCorrectForm() throws InvalidAlgorithmParameterException {
//        Hasher h = Hasher.getDefault();
//        String ch = h.encrypt(UNSAFE_PASSWORD.toCharArray());
//        String[] parts = ch.split(":");
//        Assertions.assertThat(parts.length).isEqualTo(4);
//        Assertions.assertThat(parts[0]).isEqualTo(h.getScheme());
//        Integer iter = (Integer) h.getConfiguration().get().get(PBKDF2Hasher.ITERATIONS_PROPERTY_NAME);
//        Assertions.assertThat(Integer.parseInt(parts[1])).isEqualTo(iter);
//        Assertions.assertThat(parts[3].toCharArray()).isNotEqualTo(UNSAFE_PASSWORD.toCharArray());
//    }
//
//    @Test(dataProvider = "crypts")
//    public void PBKDF2Crypt_Should_MatchPassword(String crypt) throws InvalidAlgorithmParameterException {
//        Hasher h = ColonCryptFormat.SINGLETON.resolve(crypt)
//                .orElseThrow(InvalidAlgorithmParameterException::new);
//        Assertions.assertThat(h.check(Credentials.fromPassword(UNSAFE_PASSWORD.toCharArray()), crypt)).isTrue();
//    }
//
//    @Test
//    public void PBKDF2Crypt_Should_GenerateDifferentCrypts() throws InvalidAlgorithmParameterException {
//        Hasher h = Hasher.getDefault();
//        String c1 = h.encrypt(UNSAFE_PASSWORD.toCharArray());
//        String c2 = h.encrypt(UNSAFE_PASSWORD.toCharArray());
//        String c3 = h.encrypt(UNSAFE_PASSWORD.toCharArray());
//        String c4 = h.encrypt(UNSAFE_PASSWORD.toCharArray());
//        List<String> crypts = java.util.Arrays.asList(c1, c2, c3, c4);
//        Assertions.assertThat(Collections.duplicatesFrom(crypts)).isEmpty();
//        Assertions.assertThat(crypts).are(new Condition<String>() {
//            @Override
//            public boolean matches(String value) {
//                try {
//                    return h.check(Credentials.fromPassword(UNSAFE_PASSWORD.toCharArray()), value);
//                } catch (InvalidAlgorithmParameterException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        });
//    }
//
//    @Test
//    public void PBKDF2HasherCLI_Should_ComputeHash() throws InvalidAlgorithmParameterException, UnsupportedEncodingException {
//        PrintStream old = System.out;
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        forbidSystemExitCall() ;
//        try {
//            PrintStream ps = new PrintStream(baos);
//            System.setOut(ps);
//            PBKDF2Hasher.main(Arrays.array("--encrypt", "--unsafe", UNSAFE_PASSWORD));
//        } catch( ExitTrappedException e ) {
//        } finally {
//            enableSystemExitCall() ;
//            System.out.flush();
//            System.setOut(old);
//            Assertions.assertThat(baos.toString("UTF-8")).contains("Password crypt computed");
//            Assertions.assertThat(baos.toString("UTF-8")).contains("PBKDF2:1000:");
//            System.out.println(baos);
//        }
//    }
//
//    @Test
//    public void PBKDF2HasherCLI_Should_CheckHash() throws InvalidAlgorithmParameterException, UnsupportedEncodingException {
//        PrintStream old = System.out;
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        forbidSystemExitCall() ;
//        try {
//            PrintStream ps = new PrintStream(baos);
//            System.setOut(ps);
//            PBKDF2Hasher.main(Arrays.array("--check", (String) crypts()[0][0], "--unsafe", new String(UNSAFE_PASSWORD)));
//        } catch( ExitTrappedException e ) {
//        } finally {
//            enableSystemExitCall() ;
//            System.out.flush();
//            System.setOut(old);
//            Assertions.assertThat(baos.toString("UTF-8")).contains("Valid password");
//            System.out.println(baos);
//        }
//    }
//
//    @SuppressWarnings("serial")
//    private static class ExitTrappedException extends SecurityException { }
//
//    private static void forbidSystemExitCall() {
//        final SecurityManager securityManager = new SecurityManager() {
//            @Override
//            public void checkPermission(Permission permission) {
//                if (permission.getName().startsWith("exitVM")) {
//                    throw new ExitTrappedException() ;
//                }
//            }
//        } ;
//        System.setSecurityManager( securityManager ) ;
//    }
//
//    private static void enableSystemExitCall() {
//        System.setSecurityManager( null ) ;
//    }

}
