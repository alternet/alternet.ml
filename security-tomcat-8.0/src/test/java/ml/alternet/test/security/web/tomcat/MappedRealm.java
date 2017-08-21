package ml.alternet.test.security.web.tomcat;

import java.security.InvalidAlgorithmParameterException;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.hashers.ModularCryptFormatHashers;

import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.realm.RealmBase;

class MappedRealm extends RealmBase {

    Hasher hr = ModularCryptFormatHashers.$2$.get().build(); // TODO Hasher conf

    private final Map<String,GenericPrincipal> principals = new HashMap<>();

    public void putUser(String userName, char[] password, String[] roles)
            throws InvalidAlgorithmParameterException {
//        String crypt = Hasher.getDefault().encrypt(password);
        String crypt = hr.encrypt(password);
        GenericPrincipal principal = new GenericPrincipal(userName, crypt, Arrays.asList(roles));
        principals.put(userName, principal);
    }

    @Override
    protected String getName() {
        return MappedRealm.class.getName();
    }

    @Override
    protected String getPassword(String username) {
        GenericPrincipal principal =  principals.get(username);
        if (principal != null) {
            return principal.getPassword();
        } else {
            return null;
        }
    }
    @Override
    protected Principal getPrincipal(String username) {
        return principals.get(username);
    }
}