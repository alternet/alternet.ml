package ml.alternet.security.web.jetty.auth;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.login.LoginException;

import org.eclipse.jetty.jaas.spi.LdapLoginModule;
import org.eclipse.jetty.jaas.spi.UserInfo;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

import ml.alternet.security.Password;
import ml.alternet.security.PasswordState;
import ml.alternet.security.auth.CredentialsChecker;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.web.jetty.auth.CredentialsCallback.AltCredential;

public class AltLdapLoginModule extends LdapLoginModule implements CredentialsChecker {

    private static final Logger LOG = Log.getLogger(AltLdapLoginModule.class);

    List<CryptFormat> formats;

    @Override
    public Callback[] configureCallbacks () {
        Callback[] callbacks = new Callback[2];
        callbacks[0] = new NameCallback("Enter user name");
        callbacks[1] = new CredentialsCallback(this.formats);
        return callbacks;
    }

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
        Map<String, ?> options) 
    {
        super.initialize(subject, callbackHandler, sharedState, options);
        // see LDAPAuthTest#setJaasConfiguration()
        // a comma separated list of classes CryptFormat
        String[] cryptFormatClasses = ((String) options.get(CryptFormat.class.getName())).split("\\s*,\\s*");
        setCryptFormats(cryptFormatClasses);
        
        initPrivateFields(options);
    }

    @Override
    public void setCryptFormats(List<CryptFormat> formats) {
        this.formats = formats;
    }

    @Override
    public List<CryptFormat> getCryptFormats() {
        return this.formats;
    }

    @Override
    public void reportError(String message, String crypt, Exception e) {
        // unused : CredentialsCallback will do it
    }

    //
    // ============= copy of jetty methods ==============
    //

    // first we pick the values from the options, since they are not reachable
    private void initPrivateFields(Map<String, ?> options) {
        _userBaseDn = (String) options.get("userBaseDn");
        _userObjectClass = getOption(options, "userObjectClass", _userObjectClass);
        _userIdAttribute = getOption(options, "userIdAttribute", _userIdAttribute);
        _roleBaseDn = (String) options.get("roleBaseDn");
        _roleObjectClass = getOption(options, "roleObjectClass", _roleObjectClass);
        _roleMemberAttribute = getOption(options, "roleMemberAttribute", _roleMemberAttribute);
        _roleNameAttribute = getOption(options, "roleNameAttribute", _roleNameAttribute);
        try {
            _rootContext = new InitialDirContext(getEnvironment());
        } catch (NamingException ex) {
            throw new IllegalStateException("Unable to establish root context", ex);
        }
    }

    // here are the copy of the fields
    private String _userBaseDn;
    private String _userObjectClass = "inetOrgPerson";
    private String _userIdAttribute = "cn";
    private String _roleBaseDn;
    private String _roleObjectClass = "groupOfUniqueNames";
    private String _roleMemberAttribute = "uniqueMember";
    private String _roleNameAttribute = "roleName";
    private DirContext _rootContext;

    // and below the methods that use that fields

    // here, we are binding the login with a Password object that require a specific handling
    public boolean bindingLogin(String username, Object password) throws LoginException, NamingException {
        SearchResult searchResult = findUser(username);
        String userDn = searchResult.getNameInNamespace();
        LOG.info("Attempting authentication: " + userDn);
        Hashtable<Object,Object> environment = getEnvironment();
        if ( userDn == null || "".equals(userDn) ) {
            throw new NamingException("username may not be empty");
        }
        environment.put(Context.SECURITY_PRINCIPAL, userDn);
        // RFC 4513 section 6.3.1, protect against ldap server implementations that allow successful binding on empty passwords
        if ( password == null || "".equals(password)) {
            throw new NamingException("password may not be empty");
        }

        // here is the special stuff with the Password class
        Password pwd = ((AltCredential) password).getCredentials().getPassword();
        if ( pwd.state() == PasswordState.Empty) {
            throw new NamingException("password may not be empty");
        }
        try (Password.Clear clear = pwd.getClearCopy()) {
            char[] clearPwd = clear.get();
            if ( clearPwd.length == 0) {
                throw new NamingException("password may not be empty");
            }

            // we set chars in the environment
            environment.put(Context.SECURITY_CREDENTIALS, clearPwd);
            DirContext dirContext = new InitialDirContext(environment);
            List<String> roles = getUserRolesByDn(dirContext, userDn);

            UserInfo userInfo = new UserInfo(username, null, roles);
            setCurrentUser(new JAASUserInfo(userInfo));
        } // pwd chars are cleared at this point

        setAuthenticated(true);
        return true;
    }

    // copy of the method in the parent class that has not enough visibility
    private SearchResult findUser(String username) throws NamingException, LoginException
    {
        SearchControls ctls = new SearchControls();
        ctls.setCountLimit(1);
        ctls.setDerefLinkFlag(true);
        ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        String filter = "(&(objectClass={0})({1}={2}))";

        LOG.info("Searching for users with filter: \'" + filter + "\'" + " from base dn: " + _userBaseDn);

        Object[] filterArguments = new Object[]{
            _userObjectClass,
            _userIdAttribute,
            username
        };
        NamingEnumeration<SearchResult> results = _rootContext.search(_userBaseDn, filter, filterArguments, ctls);

        LOG.info("Found user?: " + results.hasMoreElements());

        if (!results.hasMoreElements())
        {
            throw new LoginException("User not found.");
        }

        return (SearchResult) results.nextElement();
    }

    private List<String> getUserRolesByDn(DirContext dirContext, String userDn) throws LoginException, NamingException
    {
        List<String> roleList = new ArrayList<String>();

        if (dirContext == null || _roleBaseDn == null || _roleMemberAttribute == null || _roleObjectClass == null)
        {
            return roleList;
        }

        SearchControls ctls = new SearchControls();
        ctls.setDerefLinkFlag(true);
        ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        ctls.setReturningAttributes(new String[]{_roleNameAttribute});

        String filter = "(&(objectClass={0})({1}={2}))";
        Object[] filterArguments = {_roleObjectClass, _roleMemberAttribute, userDn};
        NamingEnumeration<SearchResult> results = dirContext.search(_roleBaseDn, filter, filterArguments, ctls);

        LOG.debug("Found user roles?: " + results.hasMoreElements());

        while (results.hasMoreElements())
        {
            SearchResult result = (SearchResult) results.nextElement();

            Attributes attributes = result.getAttributes();

            if (attributes == null)
            {
                continue;
            }

            Attribute roleAttribute = attributes.get(_roleNameAttribute);

            if (roleAttribute == null)
            {
                continue;
            }

            NamingEnumeration<?> roles = roleAttribute.getAll();
            while (roles.hasMore())
            {
                roleList.add(roles.next().toString());
            }
        }

        return roleList;
    }

    private String getOption(Map<String,?> options, String key, String defaultValue)
    {
        Object value = options.get(key);

        if (value == null)
        {
            return defaultValue;
        }

        return (String) value;
    }

}
