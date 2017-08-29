package ml.alternet.test.security.web.jetty.ldap;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jetty.util.component.AbstractLifeCycle;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldif.LDIFChangeRecord;
import com.unboundid.ldif.LDIFException;
import com.unboundid.ldif.LDIFReader;

import ml.alternet.util.StringUtil;

/**
 * An embedded LDAP server that can be attached to the Jetty life-cycle.
 *
 * Typical usage :
 * <pre>
 * &lt;?xml version="1.0"?&gt;
 * &lt;!DOCTYPE Configure PUBLIC "-//Jetty//Configure//EN" "http://www.eclipse.org/jetty/configure_9_2.dtd"&gt;
 * &lt;Configure id="Server" class="org.eclipse.jetty.server.Server"&gt;
 *   &lt;Call name="addBean"&gt;
 *     &lt;Arg&gt;
 *       &lt;New class="ml.alternet.test.security.web.jetty.ldap.LdapServer"&gt;
 *         &lt;Set name="port"&gt;&lt;Property name="ldap.port" default="10389"/&gt;&lt;/Set&gt;
 *         &lt;Set name="baseDn"&gt;&lt;Property name="ldap.baseDn" default="dc=alternet,dc=ml"/&gt;&lt;/Set&gt;
 *         &lt;Set name="ldifFiles"&gt;&lt;Property name="ldap.ldifFiles"/&gt;&lt;/Set&gt;
 *       &lt;/New&gt;
 *     &lt;/Arg&gt;
 *   &lt;/Call&gt;
 * &lt;/Configure&gt;
 * </pre>
 * 
 * <h1>NOTE</h1>
 * This LDAP server relies on <a href="https://www.ldap.com/unboundid-ldap-sdk-for-java">UnboundID LDAP SDK for Java</a>.
 * It appears that hash passwords are not supported by this version,
 * therefore the configuration (typically in <code>ldap.conf</code> file) should indicate
 * <code>forceBindingLogin="false"</code> (which means that the LDAP server will sent back
 * the password field and checking it with the clear password won't be performed by the LDAP server)
 * rather than <code>forceBindingLogin="true"</code>
 * (which means that the clear password will be sent to the LDAP server that will check if they
 * match the password field, therefore the password field stored in LDAP can't be a hash,
 * it must be a clear plaintext password only).
 * In a new release these may change, <a href="https://github.com/pingidentity/ldapsdk/issues/32">as mentioned here</a>
 *
 * @author Philippe Poulard
 */
public class LdapServer extends AbstractLifeCycle {

    int port;
    String baseDn;
    String bindDn;
    String bindPassword;
    String[] ldifFiles;

    InMemoryDirectoryServer ldapServer;

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * The port must be set.
     *
     * @param port the port to set
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @return the baseDN
     */
    public String getBaseDn() {
        return baseDn;
    }

    /**
     * The base DN must be set.
     *
     * @param baseDn the baseDN to set
     */
    public void setBaseDn(String baseDn) {
        this.baseDn = baseDn;
    }

    /**
     * @return the Bind DN
     */
    public String getBindDn() {
        return bindDn;
    }

    /**
     * The bind DN is optional.
     *
     * @param authDN the authDN to set
     */
    public void setBindDb(String bindDn) {
        this.bindDn = bindDn;
    }

    /**
     * @return the bindPassword
     */
    public String getBindPassword() {
        return bindPassword;
    }

    /**
     * The bind password must be set if the bind DN has been set.
     *
     * @param authPassword the authPassword to set
     */
    public void setBindPassword(String bindPassword) {
        this.bindPassword = bindPassword;
    }

    /**
     * A mandatory LDIF file to load in the LDAP server.
     *
     * @return the ldifFiles
     */
    public String[] getLdifFiles() {
        return ldifFiles;
    }

    /**
     * Mandatory LDIF files to load in the LDAP server.
     *
     * @param ldifFiles the ldifFiles to set
     */
    public void setLdifFiles(String... ldifFiles) {
        this.ldifFiles = ldifFiles;
    }

    /**
     * A mandatory LDIF file to load in the LDAP server.
     *
     * @param ldifFiles the ldifFiles to set
     */
    public void setLdifFiles(String ldifFile) {
        this.ldifFiles = new String[]{ ldifFile };
    }

    @Override
    protected void doStart() throws Exception {
        final InMemoryListenerConfig listenerConfig = InMemoryListenerConfig.createLDAPConfig("default", port);
        final InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig(new DN(baseDn));
        config.setListenerConfigs(listenerConfig);
        if (! StringUtil.isVoid(bindDn) && ! StringUtil.isVoid(bindPassword)) {
            config.addAdditionalBindCredentials(bindDn, bindPassword);
        }
        ldapServer = new InMemoryDirectoryServer(config);
        ldapServer .startListening();
        for (final String ldifFile : ldifFiles) {
            loadData(ldifFile);
        }
    }

    /**
     * Load LDIF records from a file to seed the LDAP directory.
     *
     * @param server   The embedded LDAP directory server.
     * @param ldifFile The LDIF resource or file from which LDIF records will be loaded.
     * @throws com.unboundid.ldif.LDIFException     If there was an error in the LDIF data.
     * @throws com.unboundid.ldap.sdk.LDAPException If there was a problem loading the LDIF records into the LDAP directory.
     * @throws java.io.IOException                  If there was a problem reading the LDIF records from the file.
     */
    public void loadData(String ldifFile)
            throws LDIFException, LDAPException, IOException {
        if (ldifFile != null && !ldifFile.isEmpty()) {
            InputStream inputStream = new FileInputStream(ldifFile);
            try {
                final LDIFReader ldifReader = new LDIFReader(inputStream);
                for (LDIFChangeRecord changeRecord = ldifReader.readChangeRecord(true);
                                      changeRecord != null;
                                      changeRecord = ldifReader.readChangeRecord(true)) {
                    changeRecord.processChange(ldapServer.getConnection());
                }
            } finally {
                inputStream.close();
            }
        }
    }

    @Override
    protected void doStop() throws Exception {
        this.ldapServer.shutDown(true);
        this.ldapServer = null;
    }

}
